package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

/** Recurso solicitado no encontrado en la base de datos. */
public class RecursoNoEncontradoException extends ApiException {
    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
