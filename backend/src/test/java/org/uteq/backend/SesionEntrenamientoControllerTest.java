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
import org.uteq.backend.deportivo.sesion.controller.SesionEntrenamientoController;
import org.uteq.backend.deportivo.sesion.dto.SesionHoyResponse;
import org.uteq.backend.deportivo.sesion.service.SesionEntrenamientoService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de SesionEntrenamientoController a nivel HTTP: que resuelve la
 * identidad correcta desde SecurityContextHolder (username y si el rol ve
 * todas las sesiones) y delega en SesionEntrenamientoService, ya mockeado.
 * Las reglas de negocio (filtrado por entrenador, validaciones de
 * creacion) se prueban en SesionEntrenamientoServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class SesionEntrenamientoControllerTest {

    @Mock private SesionEntrenamientoService sesionService;

    @InjectMocks private SesionEntrenamientoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        // SecurityContextHolder usa un ThreadLocal: sin esto, una prueba
        // podria heredar la autenticacion que dejo la anterior.
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username, String rol) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private SesionHoyResponse respuesta(String entrenador) {
        return new SesionHoyResponse(1L, "SUB-12", entrenador, LocalDate.now(),
                LocalTime.of(16, 0), LocalTime.of(17, 0), "Cancha 1", "PROGRAMADA", false);
    }

    @Test
    @DisplayName("hoy pasa veTodasLasSesiones=true para ADMINISTRADOR")
    void hoy_administrador_pide_todas() throws Exception {
        autenticarComo("admin@sged.test", "ADMINISTRADOR");
        when(sesionService.sesionesDeHoy("admin@sged.test", true))
                .thenReturn(List.of(respuesta("Carlos"), respuesta("Marta")));

        mockMvc.perform(get("/api/sesiones/hoy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("hoy pasa veTodasLasSesiones=true para RECEPCIONISTA")
    void hoy_recepcionista_pide_todas() throws Exception {
        autenticarComo("recepcion@sged.test", "RECEPCIONISTA");
        when(sesionService.sesionesDeHoy("recepcion@sged.test", true))
                .thenReturn(List.of(respuesta("Carlos")));

        mockMvc.perform(get("/api/sesiones/hoy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("hoy pasa veTodasLasSesiones=false y el username propio para ENTRENADOR")
    void hoy_entrenador_pide_solo_las_propias() throws Exception {
        autenticarComo("carlos@sged.test", "ENTRENADOR");
        when(sesionService.sesionesDeHoy("carlos@sged.test", false))
                .thenReturn(List.of(respuesta("Carlos")));

        mockMvc.perform(get("/api/sesiones/hoy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entrenador").value("Carlos"));

        verify(sesionService).sesionesDeHoy("carlos@sged.test", false);
    }

    @Test
    @DisplayName("mias delega el username autenticado y la paginacion")
    void mias_delega_username_y_paginacion() throws Exception {
        autenticarComo("carlos@sged.test", "ENTRENADOR");
        when(sesionService.misSesiones(eq("carlos@sged.test"), anyInt(), anyInt()))
                .thenReturn(List.of(respuesta("Carlos")));

        mockMvc.perform(get("/api/sesiones/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(sesionService).misSesiones("carlos@sged.test", 0, 20);
    }

    @Test
    @DisplayName("crear delega el username autenticado, nunca uno enviado en el body")
    void crear_delega_username_autenticado() throws Exception {
        autenticarComo("carlos@sged.test", "ENTRENADOR");
        when(sesionService.crear(eq("carlos@sged.test"), any()))
                .thenReturn(respuesta("Carlos"));

        mockMvc.perform(post("/api/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\":5,\"fecha\":\"2026-08-10\",\"horaInicio\":\"16:00:00\",\"horaFin\":\"17:30:00\",\"campo\":\"Cancha 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entrenador").value("Carlos"));

        verify(sesionService).crear(eq("carlos@sged.test"), any());
    }

    @Test
    @DisplayName("crear con body invalido responde 422 sin llegar al servicio")
    void crear_body_invalido_no_llega_al_servicio() throws Exception {
        autenticarComo("carlos@sged.test", "ENTRENADOR");

        mockMvc.perform(post("/api/sesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
