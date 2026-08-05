package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.deportivo.asistencia.controller.AsistenciaQrController;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.service.AsistenciaService;
import org.uteq.backend.deportivo.asistencia.service.QrAsistenciaService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaQrControllerTest {

    private MockMvc mockMvc;

    @Mock private QrAsistenciaService qrService;
    @Mock private AsistenciaService asistenciaService;

    @InjectMocks
    private AsistenciaQrController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        var auth = new UsernamePasswordAuthenticationToken(
                "andres.estudiante@sged.test", null, List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /marcar - token invalido, ya usado o expirado da 410, uniforme para los tres casos")
    void marcar_token_invalido_da_410() throws Exception {
        when(qrService.canjear("token-malo")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/asistencias/qr/marcar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-malo\"}"))
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("POST /marcar - token valido marca la asistencia con el username autenticado, no uno del cuerpo")
    void marcar_token_valido_devuelve_201_con_estado() throws Exception {
        when(qrService.canjear("token-bueno")).thenReturn(Optional.of(1L));
        when(asistenciaService.marcarPorQr(eq("andres.estudiante@sged.test"), eq(1L)))
                .thenReturn(Asistencia.builder().idAsistencia(1L).estado(Asistencia.ESTADO_PRESENTE).build());

        mockMvc.perform(post("/api/asistencias/qr/marcar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-bueno\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PRESENTE"));
    }

    @Test
    @DisplayName("POST /marcar - un doble marcado propaga el 400 del servicio, no un 500")
    void marcar_doble_marcado_da_400() throws Exception {
        when(qrService.canjear("token-bueno")).thenReturn(Optional.of(1L));
        when(asistenciaService.marcarPorQr(any(), any()))
                .thenThrow(new IllegalArgumentException("Ya marcaste tu asistencia en esta sesión"));

        mockMvc.perform(post("/api/asistencias/qr/marcar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-bueno\"}"))
                .andExpect(status().isBadRequest());
    }
}
