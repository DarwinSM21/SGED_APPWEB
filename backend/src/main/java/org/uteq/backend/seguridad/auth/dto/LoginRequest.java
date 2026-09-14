package org.uteq.backend.seguridad.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de inicio de sesión.
 *
 * @param username  nombre de usuario o correo
 * @param password  contraseña en texto plano (viaja solo por HTTPS)
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6) String password
) {}
