package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * Excepcion base que encapsula un ProblemDetail (RFC 7807).
 * Todas las excepciones de dominio extienden esta clase.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ProblemDetail toProblemDetail(String tipo, String titulo) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, getMessage());
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/" + tipo));
        pd.setTitle(titulo);
        return pd;
    }
}
