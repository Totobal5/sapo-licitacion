package cl.sapo.licitaciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for nested buyer information in API response.
 * Extracts the region unit from the tender buyer.
 */
public record CompradorDTO(
        @JsonProperty("RutUnidad")
        String rutUnidad,
        
        @JsonProperty("NombreUnidad")
        String nombreUnidad,
        
        @JsonProperty("RegionUnidad")
        String regionUnidad
) {
}
