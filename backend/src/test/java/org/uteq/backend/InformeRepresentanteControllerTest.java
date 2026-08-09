package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.academico.representante.controller.InformeRepresentanteController;
import org.uteq.backend.academico.representante.dto.InformeDtos.EstudianteResumenResponse;
import org.uteq.backend.academico.representante.dto.InformeDtos.InformeEstudianteResponse;
import org.uteq.backend.academico.representante.dto.NotificacionDtos.NotificacionResponse;
import org.uteq.backend.academico.representante.entity.Notificacion.Tipo;
import org.uteq.backend.academico.representante.service.InformeService;
import org.uteq.backend.academico.representante.service.NotificacionService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * La propiedad que importa: el id del estudiante nunca sale de un parametro
 * de confianza, sale de resolver quien esta autenticado (aqui, mockeado via
 * SecurityContextHolder, igual que SesionEntrenamientoControllerTest) y de
 * ahi al servicio. Un id ajeno debe dar 404, nunca 200 con datos de otro.
 */
@ExtendWith(MockitoExtension.class)
class InformeRepresentanteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InformeService informeService;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private InformeRepresentanteController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        var auth = new UsernamePasswordAuthenticationToken(
                "marta.representante@sged.test", null, List.of(new SimpleGrantedAuthority("ROLE_REPRESENTANTE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/representante/estudiantes - lista los representados del autenticado")
    void misRepresentados_devuelve_200() throws Exception {
        when(informeService.misRepresentados("marta.representante@sged.test"))
                .thenReturn(List.of(new EstudianteResumenResponse(6L, "Andres Rivas", "SUB-14")));

        mockMvc.perform(get("/api/representante/estudiantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Andres Rivas"));
    }

    @Test
    @DisplayName("GET /api/representante/estudiantes/{id}/informe - informe de un representado propio")
    void informe_de_representado_propio_devuelve_200() throws Exception {
        when(informeService.informeDe("marta.representante@sged.test", 6L)).thenReturn(
                new InformeEstudianteResponse(6L, "Andres Rivas", "SUB-14", List.of(), List.of(), null));

        mockMvc.perform(get("/api/representante/estudiantes/6/informe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Andres Rivas"));
    }

    @Test
    @DisplayName("GET /api/representante/estudiantes/{id}/informe - 404 si el estudiante no es suyo")
    void informe_de_estudiante_ajeno_devuelve_404() throws Exception {
        when(informeService.informeDe("marta.representante@sged.test", 1L))
                .thenThrow(new RecursoNoEncontradoException("Estudiante no encontrado con id: 1"));

        mockMvc.perform(get("/api/representante/estudiantes/1/informe"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/representante/notificaciones - las del autenticado, mas recientes primero")
    void misNotificaciones_devuelve_200() throws Exception {
        when(notificacionService.misNotificaciones("marta.representante@sged.test")).thenReturn(List.of(
                new NotificacionResponse(1L, 6L, "Andres Rivas", Tipo.ASISTENCIA,
                        "Andres Rivas marcó asistencia hoy (a tiempo).", false, Instant.now())));

        mockMvc.perform(get("/api/representante/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo").value("ASISTENCIA"))
                .andExpect(jsonPath("$[0].leida").value(false));
    }

    @Test
    @DisplayName("POST /api/representante/notificaciones/{id}/leida - marca como leida y responde 204")
    void marcarLeida_devuelve_204() throws Exception {
        mockMvc.perform(post("/api/representante/notificaciones/1/leida"))
                .andExpect(status().isNoContent());

        verify(notificacionService).marcarLeida("marta.representante@sged.test", 1L);
    }

    @Test
    @DisplayName("POST /api/representante/notificaciones/{id}/leida - 404 si no es suya")
    void marcarLeida_ajena_devuelve_404() throws Exception {
        doThrow(new RecursoNoEncontradoException("Notificación no encontrada con id: 9"))
                .when(notificacionService).marcarLeida("marta.representante@sged.test", 9L);

        mockMvc.perform(post("/api/representante/notificaciones/9/leida"))
                .andExpect(status().isNotFound());
    }
}
