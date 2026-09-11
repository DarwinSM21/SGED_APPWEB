package org.uteq.backend.seguridad.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición de {@code POST /api/auth/confirmar-correo} (RNF-26): el token del
 * enlace de confirmación de correo.
 *
 * @param token token de un solo uso recibido en el enlace de confirmación
 */
public record ConfirmEmailRequest(
        @NotBlank @Size(max = 128) String token
) {}
