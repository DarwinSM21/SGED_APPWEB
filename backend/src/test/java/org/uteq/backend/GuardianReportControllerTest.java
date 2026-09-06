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
import org.uteq.backend.academico.guardian.controller.GuardianReportController;
import org.uteq.backend.academico.guardian.dto.ReportDtos.StudentSummaryResponse;
import org.uteq.backend.academico.guardian.dto.ReportDtos.StudentReportResponse;
import org.uteq.backend.academico.guardian.dto.NotificationDtos.NotificationResponse;
import org.uteq.backend.academico.guardian.entity.Notification.Type;
import org.uteq.backend.academico.guardian.service.StudentReportService;
import org.uteq.backend.academico.guardian.service.NotificationService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GuardianReportControllerTest {
    private MockMvc mockMvc;

    @Mock
    private StudentReportService informeService;

    @Mock
    private NotificationService notificacionService;

    @InjectMocks
    private GuardianReportController controller;

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
        when(informeService.myStudents("marta.representante@sged.test"))
                .thenReturn(List.of(new StudentSummaryResponse(6L, "Andres Rivas", "SUB-14")));

        mockMvc.perform(get("/api/representante/estudiantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Andres Rivas"));
    }

    @Test
    @DisplayName("GET /api/representante/estudiantes/{id}/informe - informe de un representado propio")
    void informe_de_representado_propio_devuelve_200() throws Exception {
        when(informeService.reportFor("marta.representante@sged.test", 6L)).thenReturn(
                new StudentReportResponse(6L, "Andres Rivas", "SUB-14", List.of(), List.of(), null));

        mockMvc.perform(get("/api/representante/estudiantes/6/informe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Andres Rivas"));
    }

    @Test
    @DisplayName("GET /api/representante/estudiantes/{id}/informe - 404 si el estudiante no es suyo")
    void informe_de_estudiante_ajeno_devuelve_404() throws Exception {
        when(informeService.reportFor("marta.representante@sged.test", 1L))
                .thenThrow(new ResourceNotFoundException("Estudiante no encontrado con id: 1"));

        mockMvc.perform(get("/api/representante/estudiantes/1/informe"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/representante/notificaciones - las del autenticado, mas recientes primero")
    void misNotificaciones_devuelve_200() throws Exception {
        when(notificacionService.myNotifications("marta.representante@sged.test")).thenReturn(List.of(
                new NotificationResponse(1L, 6L, "Andres Rivas", Type.ASISTENCIA,
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

        verify(notificacionService).markRead("marta.representante@sged.test", 1L);
    }

    @Test
    @DisplayName("POST /api/representante/notificaciones/{id}/leida - 404 si no es suya")
    void marcarLeida_ajena_devuelve_404() throws Exception {
        doThrow(new ResourceNotFoundException("Notificación no encontrada con id: 9"))
                .when(notificacionService).markRead("marta.representante@sged.test", 9L);

        mockMvc.perform(post("/api/representante/notificaciones/9/leida"))
                .andExpect(status().isNotFound());
    }
}
