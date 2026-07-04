package org.uteq.backend.deportivo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsistenciaRequest {
    @JsonProperty("estado")
    private String estado;
    @JsonProperty("metodo")
    private String metodo;
    @JsonProperty("hora_entrada")
    private LocalTime horaEntrada;
    @JsonProperty("observacion")
    private String observacion;
}