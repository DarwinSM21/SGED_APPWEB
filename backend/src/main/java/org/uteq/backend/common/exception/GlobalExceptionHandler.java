package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
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
                HttpStatus.BAD_REQUEST, "Errores de validacion");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/Validacion"));
        pd.setTitle("Bad Request");
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

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/Interno"));
        pd.setTitle("Internal Server Error");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
