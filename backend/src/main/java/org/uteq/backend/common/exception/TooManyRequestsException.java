package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {
    public TooManyRequestsException(String mensaje) {
        super(HttpStatus.TOO_MANY_REQUESTS, mensaje);
    }
}
