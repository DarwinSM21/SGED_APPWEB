package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.uteq.backend.common.exception.ApiException;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.common.exception.TooManyRequestsException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ApiException produce un ProblemDetail con el estado de la excepcion")
    void apiException() {
        ProblemDetail pd = handler.handleApiException(
                new ResourceNotFoundException("recurso no encontrado"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("BadCredentialsException produce 401 con titulo Unauthorized")
    void badCredentials() {
        ProblemDetail pd = handler.handleBadCredentials(
                new BadCredentialsException("credenciales invalidas"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(pd.getTitle()).isEqualTo("Unauthorized");
        assertThat(pd.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("AccessDeniedException produce 403 con titulo Forbidden")
    void accessDenied() {
        ProblemDetail pd = handler.handleAccessDenied(new AccessDeniedException("no puede"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(pd.getTitle()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException produce 400")
    void cuerpoIlegible() {
        ProblemDetail pd = handler.handleUnreadableBody(
                new HttpMessageNotReadableException("cuerpo ilegible", (Throwable) null));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getTitle()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException refleja el nombre del parametro")
    void parametroFaltante() throws Exception {
        ProblemDetail pd = handler.handleMissingParameter(
                new MissingServletRequestParameterException("nombre", "string"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat((String) pd.getProperties().get("parametro")).isEqualTo("nombre");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException refleja el nombre del argumento")
    void tipoInvalido() {
        ProblemDetail pd = handler.handleInvalidType(
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat((String) pd.getProperties().get("parametro")).isEqualTo("id");
    }

    @Test
    @DisplayName("NoResourceFoundException produce 404")
    void rutaDesconocida() {
        ProblemDetail pd = handler.handleUnknownRoute(
                new NoResourceFoundException(HttpMethod.GET, "/api/no-existe"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException produce 405, no 500 (hallazgo del escaneo ZAP autenticado)")
    void metodoNoSoportado() {
        ProblemDetail pd = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("GET"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(pd.getTitle()).isEqualTo("Method Not Allowed");
        assertThat(pd.getDetail()).contains("GET");
    }

    @Test
    @DisplayName("Una excepcion generica cae en 500 con titulo Internal Server Error")
    void general() {
        ProblemDetail pd = handler.handleGeneral(new IllegalStateException("boom"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getTitle()).isEqualTo("Internal Server Error");
    }

    @Test
    @DisplayName("IllegalArgumentException produce 400 con su mensaje como detalle")
    void illegalArgument() {
        ProblemDetail pd = handler.handleIllegalArgument(
                new IllegalArgumentException("regla de negocio violada"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).isEqualTo("regla de negocio violada");
    }

    @Test
    @DisplayName("TooManyRequestsException (ApiException) conserva su estado 429")
    void tooManyRequests() {
        ProblemDetail pd = handler.handleApiException(
                new TooManyRequestsException("too many"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("Validation enumera los errores de campo como 422")
    void validation() throws Exception {
        Method metodo = GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDePrueba", String.class);
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "target");
        binding.addError(new FieldError("target", "nombre", "no puede estar vacío"));
        binding.addError(new FieldError("target", "edad", "debe ser positivo"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(new MethodParameter(metodo, 0), binding);

        ProblemDetail pd = handler.handleValidation(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat((java.util.List<?>) pd.getProperties().get("errores")).hasSize(2);
    }

    private void metodoDePrueba(String parametro) {
    }
}
