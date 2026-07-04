package org.uteq.backend.deportivo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleEvaluacionResponse {
    
    @JsonProperty("id_detalle")
    private Long idDetalle;
    
    @JsonProperty("id_evaluacion")
    private Long idEvaluacion;
    
    @JsonProperty("id_estudiante")
    private Long idEstudiante;
    
    @JsonProperty("id_criterio")
    private Long idCriterio;
    
    @JsonProperty("id_posicion_jugada")
    private Long idPosicionJugada;
    
    @JsonProperty("puntaje")
    private Integer puntaje;
}