package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

/** La operación choca con el estado actual (duplicados, etc.) -> 409. */
public class ConflictoException extends ApiException {
    public ConflictoException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
