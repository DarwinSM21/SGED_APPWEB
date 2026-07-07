package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de la aplicación. Toda excepción de negocio
 * lleva asociado el código HTTP con el que debe responder la API.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
