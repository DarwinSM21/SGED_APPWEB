package org.uteq.backend.deportivo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionDiariaResponse {
    
    @JsonProperty("id_evaluacion")
    private Long idEvaluacion;
    
    @JsonProperty("id_sesion")
    private Long idSesion;
    
    @JsonProperty("id_entrenador")
    private Long idEntrenador;
    
    @JsonProperty("observacion_general")
    private String observacionGeneral;
    
    @JsonProperty("estado")
    private String estado;
    
    @JsonProperty("creado_en")
    private LocalDateTime creadoEn;
}