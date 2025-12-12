package cl.sapo.licitaciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for individual tender item from API response.
 * Represents a product/service within a tender.
 */
public record ItemDTO(
        @JsonProperty("CodigoProducto")
        String codigoProducto,
        
        @JsonProperty("NombreProducto")
        String nombreProducto,
        
        @JsonProperty("Descripcion")
        String descripcion,
        
        @JsonProperty("Cantidad")
        Integer cantidad,
        
        @JsonProperty("UnidadMedida")
        String unidadMedida
) {
}
