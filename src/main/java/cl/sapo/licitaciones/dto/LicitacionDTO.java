package cl.sapo.licitaciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for individual tender (licitacion) from API response.
 * Maps the nested JSON structure from Mercado Publico API.
 */
public record LicitacionDTO(
        @JsonProperty("CodigoExterno")
        String codigoExterno,
        
        @JsonProperty("Nombre")
        String nombre,
        
        @JsonProperty("Descripcion")
        String descripcion,
        
        @JsonProperty("CodigoEstado")
        Integer codigoEstado,
        
        @JsonProperty("FechaCierre")
        String fechaCierre,
        
        @JsonProperty("FechaPublicacion")
        String fechaPublicacion,
        
        @JsonProperty("Fechas")
        FechasDTO fechas,
        
        @JsonProperty("Comprador")
        CompradorDTO comprador,
        
        @JsonProperty("Items")
        ItemsContainerDTO items
) {
}
