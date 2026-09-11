package org.uteq.backend.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Da forma {@code ProblemDetail} (RFC 9457) a los dos rechazos que Spring
 * Security resuelve antes de llegar a un controlador —sin sesión válida o
 * sin permiso—, que de otro modo saldrían con el cuerpo por defecto del
 * framework en vez del formato de error uniforme del resto de la API.
 */
@Configuration
public class ProblemDetailsAuthHandlers {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * @return el manejador que responde {@code 401} cuando la petición no trae una sesión válida
     */
    @Bean
    public AuthenticationEntryPoint problemAuthEntryPoint() {
        return (request, response, ex) ->
                escribir(request, response, HttpStatus.UNAUTHORIZED,
                        "NoAutenticado",
                        "Se requiere autenticación para acceder a este recurso");
    }

    /**
     * @return el manejador que responde {@code 403} cuando el rol autenticado no tiene permiso sobre el recurso
     */
    @Bean
    public AccessDeniedHandler problemAccessDeniedHandler() {
        return (request, response, ex) ->
                escribir(request, response, HttpStatus.FORBIDDEN,
                        "AccesoDenegado",
                        "No tiene permisos para acceder a este recurso");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response,
                          HttpStatus status, String tipo, String detalle)
            throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setType(URI.create("https://sged.uteq.edu.ec/errores/" + tipo));
        pd.setTitle(status.getReasonPhrase());
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(pd));
    }
}
