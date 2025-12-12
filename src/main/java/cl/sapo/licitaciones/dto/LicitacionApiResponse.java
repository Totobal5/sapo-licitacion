package cl.sapo.licitaciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Root DTO for Mercado Publico API response.
 * Contains metadata and the list of tenders.
 */
public record LicitacionApiResponse(
        @JsonProperty("Cantidad")
        Integer cantidad,
        
        @JsonProperty("FechaCreacion")
        String fechaCreacion,
        
        @JsonProperty("Version")
        String version,
        
        @JsonProperty("Listado")
        List<LicitacionDTO> listado
) {
}
