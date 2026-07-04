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
public class EntrenadorRequest {
    @JsonProperty("id_persona")
    private Long idPersona;
    @JsonProperty("especialidad")
    private String especialidad;
    @JsonProperty("fecha_contratacion")
    private LocalDate fechaContratacion;
}