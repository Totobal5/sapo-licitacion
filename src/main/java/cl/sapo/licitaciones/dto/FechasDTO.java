package cl.sapo.licitaciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Fechas (dates) from detailed API response.
 */
public record FechasDTO(
        @JsonProperty("FechaCreacion")
        String fechaCreacion,
        
        @JsonProperty("FechaCierre")
        String fechaCierre,
        
        @JsonProperty("FechaInicio")
        String fechaInicio,
        
        @JsonProperty("FechaFinal")
        String fechaFinal,
        
        @JsonProperty("FechaPubRespuestas")
        String fechaPubRespuestas,
        
        @JsonProperty("FechaActoAperturaTecnica")
        String fechaActoAperturaTecnica,
        
        @JsonProperty("FechaActoAperturaEconomica")
        String fechaActoAperturaEconomica,
        
        @JsonProperty("FechaPublicacion")
        String fechaPublicacion,
        
        @JsonProperty("FechaAdjudicacion")
        String fechaAdjudicacion,
        
        @JsonProperty("FechaEstimadaAdjudicacion")
        String fechaEstimadaAdjudicacion,
        
        @JsonProperty("FechaSoporteFisico")
        String fechaSoporteFisico,
        
        @JsonProperty("FechaTiempoEvaluacion")
        String fechaTiempoEvaluacion,
        
        @JsonProperty("FechaEstimadaFirma")
        String fechaEstimadaFirma,
        
        @JsonProperty("FechasUsuario")
        String fechasUsuario,
        
        @JsonProperty("FechaVisitaTerreno")
        String fechaVisitaTerreno,
        
        @JsonProperty("FechaEntregaAntecedentes")
        String fechaEntregaAntecedentes
) {
}
