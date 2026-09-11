package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * Excepción base de negocio con su propio estado HTTP, para que
 * {@link GlobalExceptionHandler} la traduzca a un {@code ProblemDetail} sin
 * caer en el manejador genérico de 500.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    /**
     * @param status estado HTTP a devolver
     * @param mensaje detalle legible del error
     */
    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    /**
     * @return el estado HTTP asociado a esta excepción
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * @param tipo segmento final de la URL de {@code type} (RFC 9457)
     * @param titulo título legible del problema
     * @return el detalle del problema con el estado, tipo y título indicados
     */
    public ProblemDetail toProblemDetail(String tipo, String titulo) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, getMessage());
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/" + tipo));
        pd.setTitle(titulo);
        return pd;
    }
}
