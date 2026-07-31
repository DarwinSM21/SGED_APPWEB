package org.uteq.backend.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Manejador global de excepciones. Todas las respuestas de error
 * siguen el formato Problem Detail (RFC 7807).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        String tipo = ex.getClass().getSimpleName();
        ProblemDetail pd = ex.toProblemDetail(tipo, ex.getStatus().getReasonPhrase());
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, "Errores de validacion");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/Validacion"));
        pd.setTitle("Unprocessable Entity");
        pd.setProperty("errores", errores);
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/NoAutenticado"));
        pd.setTitle("Unauthorized");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/AccesoDenegado"));
        pd.setTitle("Forbidden");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    /**
     * Cuerpo de peticion ausente o mal formado. Sin este manejador cae en el
     * handler generico y responde 500 "Error interno del servidor": un error
     * del cliente reportado como fallo del servidor, que ademas ensucia el log
     * con trazas que no corresponden a un defecto del sistema.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleCuerpoIlegible(HttpMessageNotReadableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la peticion falta o no tiene el formato esperado");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/CuerpoIlegible"));
        pd.setTitle("Bad Request");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/Interno"));
        pd.setTitle("Internal Server Error");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage()); // 👈 ex.getMessage() pasa como el "detail"
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/ReglaDeNegocio"));
        pd.setTitle("Bad Request");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
