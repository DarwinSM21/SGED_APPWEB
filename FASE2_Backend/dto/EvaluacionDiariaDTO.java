package org.uteq.backend.deportivo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionDiariaRequest {
    
    @JsonProperty("id_sesion")
    private Long idSesion;
    
    @JsonProperty("id_entrenador")
    private Long idEntrenador;
    
    @JsonProperty("observacion_general")
    private String observacionGeneral;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class EvaluacionDiariaResponse {
    
    @JsonProperty("id_evaluacion")
    private Long idEvaluacion;
    
    @JsonProperty("id_sesion")
    private Long idSesion;
    
    @JsonProperty("id_entrenador")
    private Long idEntrenador;
    
    @JsonProperty("fecha")
    private LocalDate fecha;
    
    @JsonProperty("observacion_general")
    private String observacionGeneral;
    
    @JsonProperty("estado")
    private String estado; // BORRADOR, FINALIZADA
}
