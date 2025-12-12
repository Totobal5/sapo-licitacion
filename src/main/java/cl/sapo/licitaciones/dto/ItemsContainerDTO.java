package cl.sapo.licitaciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Container DTO for items list.
 * IMPORTANT: The API wraps the items list in an object with a "Listado" field.
 */
public record ItemsContainerDTO(
        @JsonProperty("Cantidad")
        Integer cantidad,
        
        @JsonProperty("Listado")
        List<ItemDTO> listado
) {
}
