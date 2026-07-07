package org.uteq.backend.common.exception;

import java.time.LocalDateTime;

/**
 * Cuerpo estándar de error que devuelve la API.
 * Todos los errores (401, 404, 409, 500, etc.) responden con esta forma,
 * lo que permite al frontend tratarlos de manera uniforme.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path
) {
    public static ErrorResponse of(int status, String error, String mensaje, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, mensaje, path);
    }
}
