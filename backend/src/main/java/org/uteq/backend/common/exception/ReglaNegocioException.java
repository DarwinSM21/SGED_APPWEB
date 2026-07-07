package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Se violó una regla de negocio (p. ej. calificar a un estudiante AUSENTE,
 * modificar una evaluación FINALIZADA) -> 422.
 */
public class ReglaNegocioException extends ApiException {
    public ReglaNegocioException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }
}
