package cl.sapo.licitaciones.service;

import cl.sapo.licitaciones.dto.ItemDTO;
import cl.sapo.licitaciones.dto.LicitacionApiResponse;
import cl.sapo.licitaciones.dto.LicitacionDTO;
import cl.sapo.licitaciones.entity.ItemLicitacion;
import cl.sapo.licitaciones.entity.Licitacion;
import cl.sapo.licitaciones.repository.LicitacionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service responsible for synchronizing tenders from Mercado Publico API.
 * Runs every hour to fetch and persist new tenders.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter API_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int STATUS_PUBLISHED = 5; // Only tenders with status "Publicada"

    private final RestClient mercadoPublicoRestClient;
    private final LicitacionRepository licitacionRepository;

    @Value("${mercadopublico.api.ticket}")
    private String apiTicket;

    @PostConstruct
    public void validateConfig() {
        if (apiTicket == null || "YOUR_API_KEY_HERE".equals(apiTicket)) {
            log.error("API ticket not configured! Set mercadopublico.api.ticket in application.properties");
            throw new IllegalStateException("Mercado Público API ticket is required");
        }
        log.info("API ticket validated successfully");
    }

    /**
     * Scheduled sync task that runs every hour at minute 0.
     * Fetches tenders from Mercado Publico API and persists them.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void syncTenders() {
        log.info("Starting tender synchronization...");

        try {
            // Use yesterday's date to avoid "fecha mayor a la actual" error
            String formattedDate = LocalDate.now().minusDays(1).format(API_DATE_FORMATTER);
            LicitacionApiResponse response = fetchTendersFromApi(formattedDate);

            if (response == null || response.listado() == null) {
                log.warn("No tenders received from API");
                return;
            }

            log.info("Fetched {} tenders from API", response.cantidad());

            // Fetch detailed data for each tender (with delay to avoid rate limiting)
            List<LicitacionDTO> detailedTenders = new java.util.ArrayList<>();
            List<LicitacionDTO> basicList = response.listado().stream()
                    .filter(this::isValidTender)
                    .collect(Collectors.toList());

            log.info("Fetching detailed data for {} valid tenders (this will take ~{} seconds)...", 
                    basicList.size(), basicList.size() * 3);

            for (int i = 0; i < basicList.size(); i++) {
                LicitacionDTO dto = basicList.get(i);
                log.debug("Fetching detail {}/{}: {}", i + 1, basicList.size(), dto.codigoExterno());
                
                LicitacionDTO detail = fetchTenderDetail(dto.codigoExterno());
                if (detail != null) {
                    detailedTenders.add(detail);
                }
                
                // Wait 3 seconds between requests to avoid rate limiting
                if (i < basicList.size() - 1) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Sleep interrupted during API calls");
                        break;
                    }
                }
            }

            log.info("Fetched detailed data for {} valid tenders", detailedTenders.size());

            // Process tenders: upsert valid ones and delete inactive ones
            int savedCount = processAndSaveTenders(response.listado(), detailedTenders);
            log.info("Synchronization completed. Processed {} tenders", savedCount);

        } catch (Exception e) {
            log.error("Error during tender synchronization", e);
        }
    }

    /**
     * Scheduled cleanup task that runs every day at midnight.
     * Removes tenders whose close date has passed.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTenders() {
        log.info("Starting cleanup of expired tenders...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = licitacionRepository.deleteExpiredTenders(now);
            
            if (deletedCount > 0) {
                log.info("Deleted {} expired tenders", deletedCount);
            } else {
                log.info("No expired tenders found");
            }
        } catch (Exception e) {
            log.error("Error during expired tenders cleanup", e);
        }
    }

    /**
     * Fetches tenders from Mercado Publico API for a specific date.
     * Logs sanitized to prevent API ticket exposure.
     */
    private LicitacionApiResponse fetchTendersFromApi(String date) {
        try {
            log.info("Fetching tenders for date: {}", date); // No ticket in logs
            String url = "/licitaciones.json?fecha={date}&ticket={ticket}";

            return mercadoPublicoRestClient
                    .get()
                    .uri(url, date, apiTicket)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response) -> {
                            log.error("HTTP Error {}: {} (API call failed)", 
                                response.getStatusCode(), response.getStatusText());
                        })
                    .body(LicitacionApiResponse.class);
        } catch (RestClientResponseException e) {
            log.error("API Error: Status {} (details hidden for security)", e.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error calling API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetches detailed tender data by codigo externo.
     * Logs sanitized to prevent API ticket exposure.
     */
    private LicitacionDTO fetchTenderDetail(String codigoExterno) {
        try {
            log.debug("Fetching tender detail: {}", codigoExterno); // No ticket/URL in logs
            String url = "/licitaciones.json?codigo={codigo}&ticket={ticket}";
            
            LicitacionApiResponse response = mercadoPublicoRestClient
                    .get()
                    .uri(url, codigoExterno, apiTicket)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, httpResponse) -> {
                            log.error("HTTP Error {} for tender {} (details hidden)", 
                                httpResponse.getStatusCode(), codigoExterno);
                        })
                    .body(LicitacionApiResponse.class);

            if (response != null && response.listado() != null && !response.listado().isEmpty()) {
                return response.listado().get(0);
            }
            
            log.warn("No detail found for tender {}", codigoExterno);
            return null;
            
        } catch (RestClientResponseException e) {
            log.error("API Error for tender {}: Status {} (details hidden)", 
                codigoExterno, e.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Error fetching tender {}: {}", codigoExterno, e.getMessage());
            return null;
        }
    }

    /**
     * Validates if a tender should be processed.
     * Criteria:
     * - Status must be 5 (Published)
     * - Close date must be in the future
     */
    private boolean isValidTender(LicitacionDTO dto) {
        if (dto.codigoEstado() == null || dto.codigoEstado() != STATUS_PUBLISHED) {
            return false;
        }

        String closeDateStr = getCloseDateString(dto);
        if (closeDateStr == null) {
            return false;
        }

        try {
            LocalDateTime closeDate = parseDateTime(closeDateStr);
            return closeDate.isAfter(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Invalid close date for tender {}: {}", dto.codigoExterno(), closeDateStr);
            return false;
        }
    }
    
    /**
     * Gets close date from DTO, checking both Fechas and direct FechaCierre.
     */
    private String getCloseDateString(LicitacionDTO dto) {
        if (dto.fechas() != null && dto.fechas().fechaCierre() != null) {
            return dto.fechas().fechaCierre();
        }
        return dto.fechaCierre();
    }
    
    /**
     * Gets publication date from DTO, checking both Fechas and direct FechaPublicacion.
     */
    private String getPublicationDateString(LicitacionDTO dto) {
        if (dto.fechas() != null && dto.fechas().fechaPublicacion() != null) {
            return dto.fechas().fechaPublicacion();
        }
        return dto.fechaPublicacion();
    }

    /**
     * Maps DTO to Entity, flattening the nested structure.
     * Extracts region from Comprador.RegionUnidad.
     */
    private Licitacion mapToEntity(LicitacionDTO dto) {
        String closeDateStr = getCloseDateString(dto);
        String publicationDateStr = getPublicationDateString(dto);
        
        Licitacion licitacion = Licitacion.builder()
                .codigoExterno(dto.codigoExterno())
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .codigoEstado(dto.codigoEstado())
                .fechaCierre(closeDateStr != null ? parseDateTime(closeDateStr) : null)
                .fechaPublicacion(publicationDateStr != null ? parseDateTime(publicationDateStr) : null)
                .build();

        // Extract region from Comprador
        if (dto.comprador() != null) {
            licitacion.setRegion(dto.comprador().regionUnidad());
            licitacion.setBuyerName(dto.comprador().nombreUnidad());
            licitacion.setBuyerRut(dto.comprador().rutUnidad());
        }

        // Map items
        if (dto.items() != null && dto.items().listado() != null) {
            List<ItemLicitacion> items = dto.items().listado().stream()
                    .map(itemDto -> mapItemToEntity(itemDto, licitacion))
                    .collect(Collectors.toList());

            items.forEach(licitacion::addItem);
        }

        return licitacion;
    }

    /**
     * Maps ItemDTO to ItemLicitacion entity.
     */
    private ItemLicitacion mapItemToEntity(ItemDTO dto, Licitacion licitacion) {
        return ItemLicitacion.builder()
                .productCode(dto.codigoProducto())
                .productName(dto.nombreProducto())
                .description(dto.descripcion())
                .quantity(dto.cantidad())
                .unitOfMeasure(dto.unidadMedida())
                .licitacion(licitacion)
                .build();
    }

    /**
     * Processes all tenders from API: upserts valid ones and deletes inactive ones.
     * 
     * @param basicList All tenders from API (basic data)
     * @param detailedTenders Valid detailed tenders to save
     * @return Number of tenders processed
     */
    private int processAndSaveTenders(List<LicitacionDTO> basicList, List<LicitacionDTO> detailedTenders) {
        int processedCount = 0;
        
        // First, check all tenders from API and delete those with status != 5
        for (LicitacionDTO basicDto : basicList) {
            String codigoExterno = basicDto.codigoExterno();
            
            // If tender exists in DB but is no longer published (status != 5), delete it
            if (basicDto.codigoEstado() != null && basicDto.codigoEstado() != STATUS_PUBLISHED) {
                if (licitacionRepository.existsByCodigoExterno(codigoExterno)) {
                    licitacionRepository.deleteByCodigoExterno(codigoExterno);
                    log.info("Deleted inactive tender: {} (status: {})", codigoExterno, basicDto.codigoEstado());
                    processedCount++;
                }
            }
        }
        
        // Then, upsert valid detailed tenders
        List<Licitacion> mappedTenders = detailedTenders.stream()
                .map(this::mapToEntity)
                .collect(Collectors.toList());
        
        for (Licitacion tender : mappedTenders) {
            try {
                licitacionRepository.save(tender); // save() does upsert automatically
                processedCount++;
                log.debug("Saved/updated tender: {}", tender.getCodigoExterno());
            } catch (Exception e) {
                log.error("Error saving tender {}", tender.getCodigoExterno(), e);
            }
        }
        
        return processedCount;
    }

    /**
     * Parses datetime string from API format.
     * Handles variable millisecond precision (2-3 digits) from API.
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }

        try {
            // Remove milliseconds if present (API returns variable precision)
            String normalized = dateTimeStr.contains(".") 
                ? dateTimeStr.substring(0, dateTimeStr.indexOf("."))
                : dateTimeStr;
            return LocalDateTime.parse(normalized, API_DATETIME_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr);
            return null;
        }
    }

    /**
     * Manual trigger for testing purposes.
     */
    public void triggerSync() {
        log.info("Manual sync triggered");
        syncTenders();
    }

    /**
     * Async manual trigger that doesn't block HTTP requests.
     */
    @Async
    public CompletableFuture<String> triggerSyncAsync() {
        log.info("Manual async sync triggered");
        syncTenders();
        return CompletableFuture.completedFuture("Sync completed");
    }
}
