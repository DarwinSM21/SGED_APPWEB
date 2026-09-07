package org.uteq.backend.seguridad.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición de {@code POST /api/auth/reset} (RF-37): el token del enlace y la
 * contraseña nueva. La complejidad de la contraseña la valida
 * {@code PasswordPolicy} (RNF-14) dentro del servicio, porque la regla de
 * "distinta del nombre de usuario" necesita el usuario, que aquí se resuelve
 * del token, no del cuerpo.
 *
 * @param token         token recibido en el enlace de restablecimiento
 * @param nuevaPassword contraseña nueva elegida por el usuario
 */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank String nuevaPassword
) {}
