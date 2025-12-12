package cl.sapo.licitaciones.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for search request parameters with validation.
 * Prevents malicious input and ensures data integrity.
 */
public record SearchRequestDTO(
    
    @Size(max = 200, message = "La consulta de búsqueda no puede exceder 200 caracteres")
    String q,
    
    @Size(max = 100, message = "El nombre de región no puede exceder 100 caracteres")
    String region,
    
    @Pattern(regexp = "^(close_date|creation_date)?$", message = "El orden debe ser 'close_date' o 'creation_date'")
    String sortBy
    
) {
    /**
     * Returns sanitized query string (null-safe).
     */
    public String getQueryOrDefault() {
        return q != null && !q.isBlank() ? q.trim() : null;
    }
    
    /**
     * Returns sanitized region string (null-safe).
     */
    public String getRegionOrDefault() {
        return region != null && !region.isBlank() ? region.trim() : null;
    }
    
    /**
     * Returns sort order with default value.
     */
    public String getSortByOrDefault() {
        return sortBy != null && !sortBy.isBlank() ? sortBy : "close_date";
    }
}
