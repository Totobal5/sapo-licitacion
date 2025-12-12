package cl.sapo.licitaciones.controller;

import cl.sapo.licitaciones.dto.SearchRequestDTO;
import cl.sapo.licitaciones.entity.ItemLicitacion;
import cl.sapo.licitaciones.entity.Licitacion;
import cl.sapo.licitaciones.service.LicitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RSS Feed Controller for Miniflux integration.
 * Generates RSS 2.0 compliant XML feed with tender information.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RssController {

    private static final DateTimeFormatter RFC_1123_FORMATTER = 
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("America/Santiago"));

    private final LicitacionService licitacionService;

    /**
     * RSS Feed endpoint with optional filters and validation.
     * 
     * @param searchRequest Validated search parameters
     * @return RSS 2.0 XML feed
     */
    @GetMapping(value = "/rss", produces = MediaType.APPLICATION_XML_VALUE)
    public String getRssFeed(
            @Valid @ModelAttribute SearchRequestDTO searchRequest,
            BindingResult bindingResult) {

        // If validation fails, return empty feed with error message
        if (bindingResult.hasErrors()) {
            log.warn("Invalid RSS parameters: {}", bindingResult.getAllErrors());
            return generateRssFeed(List.of(), null, null); // Empty feed
        }

        String q = searchRequest.getQueryOrDefault();
        String region = searchRequest.getRegionOrDefault();

        log.info("RSS feed requested with query='{}', region='{}'", q, region);

        // RSS always uses close_date sorting (furthest closing date first)
        List<Licitacion> tenders = licitacionService.searchTenders(q, region, "close_date");

        return generateRssFeed(tenders, q, region);
    }

    /**
     * Generates RSS 2.0 XML feed from tender list.
     */
    private String generateRssFeed(List<Licitacion> tenders, String query, String region) {
        StringBuilder xml = new StringBuilder();

        // XML Declaration
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <channel>\n");

        // Channel metadata
        String title = buildChannelTitle(query, region);
        xml.append("    <title>").append(escapeXml(title)).append("</title>\n");
        xml.append("    <link>http://www.mercadopublico.cl</link>\n");
        xml.append("    <description>Licitaciones públicas de Chile - MercadoPublicoMonitor</description>\n");
        xml.append("    <language>es-CL</language>\n");
        xml.append("    <atom:link href=\"http://localhost:8080/rss\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        // Items
        for (Licitacion tender : tenders) {
            xml.append(generateItem(tender));
        }

        xml.append("  </channel>\n");
        xml.append("</rss>");

        return xml.toString();
    }

    /**
     * Generates RSS item for a single tender.
     */
    private String generateItem(Licitacion tender) {
        StringBuilder item = new StringBuilder();

        item.append("    <item>\n");

        // Title
        item.append("      <title>")
            .append(escapeXml(tender.getNombre()))
            .append("</title>\n");

        // Link to Mercado Publico
        String link = "https://www.mercadopublico.cl/Procurement/Modules/RFB/DetailsAcquisition.aspx?idlicitacion=" + tender.getCodigoExterno();
        item.append("      <link>").append(link).append("</link>\n");

        // GUID (unique identifier)
        item.append("      <guid isPermaLink=\"false\">")
            .append(tender.getCodigoExterno())
            .append("</guid>\n");

        // Publication date (RFC-1123 format)
        if (tender.getFechaPublicacion() != null) {
            String pubDate = RFC_1123_FORMATTER.format(
                tender.getFechaPublicacion().atZone(ZoneId.of("America/Santiago"))
            );
            item.append("      <pubDate>").append(pubDate).append("</pubDate>\n");
        }

        // Description (HTML summary)
        String description = buildDescription(tender);
        item.append("      <description>")
            .append(escapeXml(description))
            .append("</description>\n");

        item.append("    </item>\n");

        return item.toString();
    }

    /**
     * Builds HTML description for RSS item.
     */
    private String buildDescription(Licitacion tender) {
        StringBuilder html = new StringBuilder();

        html.append("<![CDATA[");
        html.append("<div style='font-family: Arial, sans-serif;'>");

        // Region
        if (tender.getRegion() != null) {
            html.append("<p><strong>Región:</strong> ")
                .append(sanitizeHtml(tender.getRegion()))
                .append("</p>");
        }

        // Buyer
        if (tender.getBuyerName() != null) {
            html.append("<p><strong>Comprador:</strong> ")
                .append(sanitizeHtml(tender.getBuyerName()))
                .append("</p>");
        }

        // Close date
        if (tender.getFechaCierre() != null) {
            String closeDate = tender.getFechaCierre()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            html.append("<p><strong>Fecha de Cierre:</strong> ")
                .append(closeDate)
                .append("</p>");
        }

        // Description
        if (tender.getDescripcion() != null && !tender.getDescripcion().isBlank()) {
            html.append("<p><strong>Descripción:</strong><br/>")
                .append(sanitizeHtml(tender.getDescripcion()))
                .append("</p>");
        }

        // Items list
        if (tender.getItems() != null && !tender.getItems().isEmpty()) {
            html.append("<p><strong>Productos/Servicios:</strong></p>");
            html.append("<ul>");

            for (ItemLicitacion item : tender.getItems()) {
                html.append("<li>")
                    .append(sanitizeHtml(item.getProductName()));

                if (item.getQuantity() != null) {
                    html.append(" (Cantidad: ").append(item.getQuantity()).append(")");
                }

                if (item.getDescription() != null && !item.getDescription().isBlank()) {
                    html.append("<br/><em>").append(sanitizeHtml(item.getDescription())).append("</em>");
                }

                html.append("</li>");
            }

            html.append("</ul>");
        }

        html.append("<p><a href='https://www.mercadopublico.cl/Procurement/Modules/RFB/DetailsAcquisition.aspx?idlicitacion=")
            .append(tender.getCodigoExterno())
            .append("'>Ver en MercadoPublico.cl</a></p>");

        html.append("</div>");
        html.append("]]>");

        return html.toString();
    }

    /**
     * Builds channel title based on filters.
     */
    private String buildChannelTitle(String query, String region) {
        StringBuilder title = new StringBuilder("Licitaciones Públicas Chile");

        if (region != null && !region.isBlank()) {
            title.append(" - ").append(region);
        }

        if (query != null && !query.isBlank()) {
            title.append(" - Búsqueda: ").append(query);
        }

        return title.toString();
    }

    /**
     * Escapes XML special characters.
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }

    /**
     * Sanitizes HTML content to prevent XSS.
     * Removes potentially dangerous tags and attributes.
     */
    private String sanitizeHtml(String html) {
        if (html == null) {
            return "";
        }
        
        // Remove script tags and their content
        String sanitized = html.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        
        // Remove iframe, object, embed tags
        sanitized = sanitized.replaceAll("(?i)<(iframe|object|embed)[^>]*>.*?</(iframe|object|embed)>", "");
        
        // Remove event handlers (onclick, onerror, etc.)
        sanitized = sanitized.replaceAll("(?i)\\s*on\\w+\\s*=\\s*['\"].*?['\"]", "");
        
        // Remove javascript: protocol
        sanitized = sanitized.replaceAll("(?i)javascript:\\s*", "");
        
        // Escape remaining HTML to prevent CDATA breakout
        sanitized = sanitized.replace("]]>", "]]&gt;");
        
        return sanitized;
    }
}
