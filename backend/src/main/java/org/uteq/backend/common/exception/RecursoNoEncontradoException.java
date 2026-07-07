package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

/** El recurso solicitado no existe -> 404. */
public class RecursoNoEncontradoException extends ApiException {
    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
