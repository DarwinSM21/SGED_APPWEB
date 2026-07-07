package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

/** Usuario/contraseña incorrectos, usuario inactivo o token inválido -> 401. */
public class CredencialesInvalidasException extends ApiException {
    public CredencialesInvalidasException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
