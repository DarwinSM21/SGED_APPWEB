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
    private String estado; // PRESENTE, TARDE, AUSENTE, JUSTIFICADO
    
    @JsonProperty("metodo")
    private String metodo; // RFID, MANUAL
    
    @JsonProperty("hora_entrada")
    private LocalTime horaEntrada;
    
    @JsonProperty("observacion")
    private String observacion;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class AsistenciaResponse {
    
    @JsonProperty("id_asistencia")
    private Long idAsistencia;
    
    @JsonProperty("id_sesion")
    private Long idSesion;
    
    @JsonProperty("id_estudiante")
    private Long idEstudiante;
    
    @JsonProperty("hora_entrada")
    private LocalTime horaEntrada;
    
    @JsonProperty("metodo")
    private String metodo;
    
    @JsonProperty("estado")
    private String estado;
    
    @JsonProperty("observacion")
    private String observacion;
}
