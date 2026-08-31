package org.uteq.backend.seguridad.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SesionResponse {
    private String username;
    private String nombre;
    private String rol;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long idPersona;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long idUsuario;
}
