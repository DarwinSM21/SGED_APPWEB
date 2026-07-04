package org.uteq.backend.deportivo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrenadorResponse {
    
    @JsonProperty("id_entrenador")
    private Long idEntrenador;
    
    @JsonProperty("id_persona")
    private Long idPersona;
    
    @JsonProperty("nombre_completo")
    private String nombreCompleto;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("especialidad")
    private String especialidad;
    
    @JsonProperty("fecha_contratacion")
    private LocalDate fechaContratacion;
    
    @JsonProperty("activo")
    private Boolean activo;
    
    @JsonProperty("creado_en")
    private LocalDateTime creadoEn;
}