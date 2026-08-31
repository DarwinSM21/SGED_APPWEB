package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends ApiException {
    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
