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
public class EvaluacionDiariaRequest {
    @JsonProperty("id_sesion")
    private Long idSesion;
    @JsonProperty("id_entrenador")
    private Long idEntrenador;
    @JsonProperty("observacion_general")
    private String observacionGeneral;
}