package org.uteq.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de excepciones.
 *
 * Antes de esta clase, cualquier RuntimeException lanzada en un servicio
 * terminaba reenviada al endpoint interno /error, que al no estar permitido
 * en SecurityConfig respondía 403 y ocultaba el error real.
 * Con @RestControllerAdvice la excepción se convierte aquí mismo en una
 * respuesta JSON con el código HTTP correcto, sin pasar por /error.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Excepciones de negocio propias: usan el status que ellas declaran. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    /** Body inválido o JSON malformado -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBodyInvalido(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido", request);
    }

    /** Errores de validación de DTOs (@Valid) -> 400 con el primer campo fallido. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Datos de entrada inválidos");
        return build(HttpStatus.BAD_REQUEST, detalle, request);
    }

    /** Usuario autenticado pero sin el rol necesario (@PreAuthorize) -> 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(
            AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operación", request);
    }

    /**
     * Red de seguridad para RuntimeException heredadas que aún no fueron
     * migradas a ApiException -> 400 (nunca más un 403 engañoso).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException ex, HttpServletRequest request) {
        log.warn("RuntimeException no tipificada en {}: {}",
                request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Cualquier otro error inesperado -> 500 sin filtrar detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intente nuevamente", request);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String mensaje, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
