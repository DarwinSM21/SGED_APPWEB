package org.uteq.backend.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Traduce cada excepción no atrapada por el código de negocio a una
 * respuesta {@code ProblemDetail} (RFC 9457), sin exponer trazas de pila
 * ni detalles internos (RNF-10). Cada manejador fija el {@code type}, el
 * {@code title} y una marca de tiempo; el manejador general además deja
 * registro en el log del servidor, nunca en la respuesta.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @param ex excepción de negocio lanzada por el código de la aplicación
     * @return el detalle del problema con el estado HTTP que declara la excepción
     */
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        String tipo = ex.getClass().getSimpleName();
        ProblemDetail pd = ex.toProblemDetail(tipo, ex.getStatus().getReasonPhrase());
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    /**
     * @param ex excepción con los errores de validación de Bean Validation ({@code @Valid})
     * @return {@code 422} con la lista de errores campo por campo
     */
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

    /**
     * @param ex excepción lanzada por Spring Security ante usuario/contraseña inválidos
     * @return {@code 401} genérico (no distingue usuario inexistente de contraseña incorrecta)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/NoAutenticado"));
        pd.setTitle("Unauthorized");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    /**
     * @param ex excepción lanzada cuando el rol autenticado no tiene permiso sobre el recurso
     * @return {@code 403} (RNF-06: la comprobación siempre ocurre del lado del servidor)
     */
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
     * @param ex excepción lanzada cuando el cuerpo de la petición falta o no es JSON válido
     * @return {@code 400}
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

    /**
     * @param ex excepción lanzada cuando falta un parámetro de consulta obligatorio
     * @return {@code 400} con el nombre del parámetro faltante en {@code parametro}
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleParametroFaltante(MissingServletRequestParameterException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Falta el parametro obligatorio '" + ex.getParameterName() + "'");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/ParametroFaltante"));
        pd.setTitle("Bad Request");
        pd.setProperty("parametro", ex.getParameterName());
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    /**
     * @param ex excepción lanzada cuando un parámetro no puede convertirse al tipo esperado
     *           (ej. texto no numérico en un identificador)
     * @return {@code 400} con el nombre del parámetro inválido en {@code parametro}
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El valor de '" + ex.getName() + "' no tiene el formato esperado");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/ParametroInvalido"));
        pd.setTitle("Bad Request");
        pd.setProperty("parametro", ex.getName());
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    /**
     * @param ex excepción lanzada cuando la ruta pedida no existe en la aplicación
     * @return {@code 404} genérico
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleRutaDesconocida(NoResourceFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "El servidor no reconoce esta operacion");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/RutaDesconocida"));
        pd.setTitle("Not Found");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    // Hallazgo del escaneo ZAP autenticado (4.4): una ruta existente pedida
    // con un metodo HTTP que no soporta (p. ej. GET a /api/auth/login, que
    // solo acepta POST) caia en el catch-all de abajo y devolvia 500. No
    // filtraba nada (el cuerpo ya era el ProblemDetail generico, sin traza),
    // pero un metodo no soportado es un 405, no un error del servidor.
    /**
     * @param ex excepción lanzada cuando la ruta existe pero no admite el método HTTP usado
     *           (ej. {@code GET} a un endpoint que solo acepta {@code POST})
     * @return {@code 405}, con el método rechazado en el mensaje
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                "El metodo " + ex.getMethod() + " no esta soportado en esta ruta");
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/MetodoNoSoportado"));
        pd.setTitle("Method Not Allowed");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    /**
     * Manejador de último recurso: cualquier excepción no cubierta por los
     * manejadores anteriores llega acá. Registra el detalle en el log del
     * servidor y devuelve un mensaje genérico, para no filtrar información
     * interna en la respuesta.
     *
     * @param ex excepción no controlada
     * @return {@code 500} genérico
     */
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

    /**
     * @param ex excepción lanzada por una regla de negocio incumplida en la capa de servicio
     * @return {@code 400} con el mensaje de la excepción como detalle
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/ReglaDeNegocio"));
        pd.setTitle("Bad Request");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
