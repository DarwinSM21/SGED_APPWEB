package org.uteq.backend.seguridad.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
/**
 * Datos de sesión que se devuelven al cliente tras iniciar sesión; el JWT
 * viaja solo en la cookie {@code HttpOnly}, nunca en este cuerpo.
 */
public class SessionResponse {
    private String username;
    private String name;
    private String role;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long personId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long userId;
}
