package org.uteq.backend.seguridad.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición de {@code POST /api/auth/forgot} (RF-37): el nombre de usuario o
 * el correo registrado de quien quiere restablecer su contraseña.
 *
 * @param identificador username o correo; el servidor no revela cuál de los
 *                      dos coincidió ni si existe una cuenta
 */
public record ForgotPasswordRequest(
        @NotBlank @Size(max = 200) String identificador
) {}
