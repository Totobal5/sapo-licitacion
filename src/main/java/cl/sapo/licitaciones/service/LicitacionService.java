package cl.sapo.licitaciones.service;

import cl.sapo.licitaciones.entity.Licitacion;
import cl.sapo.licitaciones.repository.LicitacionRepository;
import cl.sapo.licitaciones.repository.LicitacionSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for querying and managing tenders.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LicitacionService {

    private final LicitacionRepository licitacionRepository;

    /**
     * Search tenders with optional text query and region filter.
     */
    @Transactional(readOnly = true)
    public List<Licitacion> searchTenders(String query, String region, String sortBy) {
        log.debug("Searching tenders with query='{}', region='{}', sortBy='{}'", query, region, sortBy);

        Specification<Licitacion> spec = LicitacionSpecs.searchWithFilters(query, region);
        Sort sort = getSortOrder(sortBy);
        if (sort == null) {
            sort = Sort.unsorted();
        }

        return licitacionRepository.findAll(spec, sort);
    }

    /**
     * Get all tenders (published only).
     */
    @Transactional(readOnly = true)
    public List<Licitacion> getAllTenders(String sortBy) {
        Specification<Licitacion> spec = LicitacionSpecs.hasStatus(5);
        Sort sort = getSortOrder(sortBy);
        if (sort == null) {
            sort = Sort.unsorted();
        }

        return licitacionRepository.findAll(spec, sort);
    }

    /**
     * Get sort order based on sortBy parameter.
     * - "creation_date": Order by creation date (newest first)
     * - "close_date": Order by close date (furthest in future first)
     */
    private Sort getSortOrder(String sortBy) {
        if ("creation_date".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        // Default: close_date - furthest closing date first
        return Sort.by(Sort.Direction.DESC, "fechaCierre");
    }

    /**
     * Get tenders by region.
     */
    @Transactional(readOnly = true)
    public List<Licitacion> getTendersByRegion(String region) {
        return licitacionRepository.findByRegionIgnoreCase(region);
    }

    /**
     * Get tender by external code.
     */
    @Transactional(readOnly = true)
    public Optional<Licitacion> getTenderByCode(@org.springframework.lang.NonNull String codigoExterno) {
        return licitacionRepository.findById(codigoExterno);
    }

    /**
     * Count total tenders.
     */
    @Transactional(readOnly = true)
    public long countTenders() {
        return licitacionRepository.count();
    }
}
