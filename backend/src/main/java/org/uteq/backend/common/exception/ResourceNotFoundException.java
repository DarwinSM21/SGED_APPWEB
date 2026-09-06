package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
