package org.uteq.backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.uteq.backend.common.exception.ProblemDetailsAuthHandlers;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProblemDetailsAuthHandlersTest {

    private final ProblemDetailsAuthHandlers handlers = new ProblemDetailsAuthHandlers();

    @Test
    @DisplayName("AuthenticationEntryPoint escribe 401 con ProblemDetail de no autenticado")
    void authEntryPointWrites401() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/api/reportes/estudiantes-fichas");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        AuthenticationEntryPoint entryPoint = handlers.problemAuthEntryPoint();
        entryPoint.commence(request, response, new BadCredentialsException("anónimo"));

        verify(response).setStatus(401);
        verify(response).setContentType("application/problem+json");
    }

    @Test
    @DisplayName("AccessDeniedHandler escribe 403 con ProblemDetail de acceso denegado")
    void accessDeniedHandlerWrites403() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/api/reportes/pagos");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        AccessDeniedHandler deniedHandler = handlers.problemAccessDeniedHandler();
        deniedHandler.handle(request, response, new AccessDeniedException("sin rol"));

        verify(response).setStatus(403);
        verify(response).setContentType("application/problem+json");
    }

    @Test
    @DisplayName("El ProblemDetail de no autenticado incluye el tipo del error")
    void authEntryPointWritesType() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter buffer = new StringWriter();
        when(request.getRequestURI()).thenReturn("/api/seguro");
        when(response.getWriter()).thenReturn(new PrintWriter(buffer));

        handlers.problemAuthEntryPoint().commence(request, response, new BadCredentialsException("no"));

        String cuerpo = buffer.toString();
        assertThat(cuerpo).contains("NoAutenticado");
        assertThat(cuerpo).contains("Se requiere autenticación");
    }

    @Test
    @DisplayName("El ProblemDetail de acceso denegado incluye el tipo del error")
    void accessDeniedHandlerWritesType() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter buffer = new StringWriter();
        when(request.getRequestURI()).thenReturn("/api/seguro");
        when(response.getWriter()).thenReturn(new PrintWriter(buffer));

        handlers.problemAccessDeniedHandler().handle(request, response, new AccessDeniedException("no"));

        String cuerpo = buffer.toString();
        assertThat(cuerpo).contains("AccesoDenegado");
        assertThat(cuerpo).contains("No tiene permisos");
    }
}