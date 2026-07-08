package org.uteq.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Manejador global de excepciones conforme al RFC 7807 (Problem Details
 * for HTTP APIs), exigido por el Bloque A.1 de la Tercera Entrega.
 * Toda respuesta de error incluye: type, title, status, detail e instance,
 * con Content-Type application/problem+json.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE = "https://sged.uteq.edu.ec/errores/";

    /** Excepciones de negocio propias: usan el status que ellas declaran. */
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
        return problema(ex.getStatus(), ex.getClass().getSimpleName(),
                ex.getMessage(), request);
    }

    /** Body inválido o JSON malformado -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleBodyInvalido(HttpMessageNotReadableException ex,
                                            HttpServletRequest request) {
        return problema(HttpStatus.BAD_REQUEST, "CuerpoInvalido",
                "El cuerpo de la petición es inválido", request);
    }

    /**
     * Errores de validación de DTOs (@Valid) -> 422 Unprocessable Entity.
     * La auditoría OWASP A03 (Bloque C.2) espera exactamente 422 con
     * ProblemDetails ante payloads maliciosos como ' OR '1'='1.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacion(MethodArgumentNotValidException ex,
                                          HttpServletRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Datos de entrada inválidos");
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "ValidacionFallida",
                detalle, request);
    }

    /** Autenticado pero sin el rol necesario (@PreAuthorize) -> 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccesoDenegado(AccessDeniedException ex,
                                              HttpServletRequest request) {
        return problema(HttpStatus.FORBIDDEN, "AccesoDenegado",
                "No tiene permisos para realizar esta operación", request);
    }

    /** Red de seguridad -> 500 sin filtrar detalles internos. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(),
                request.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "ErrorInterno",
                "Ocurrió un error interno. Contacte al administrador.", request);
    }

    private ProblemDetail problema(HttpStatus status, String tipo,
                                   String detalle, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setType(URI.create(BASE_TYPE + tipo));
        pd.setTitle(status.getReasonPhrase());
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
