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
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.horario.controller.HorarioController;
import org.uteq.backend.deportivo.horario.dto.HorarioResponse;
import org.uteq.backend.deportivo.horario.service.HorarioService;

import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HorarioControllerTest {

    @Mock private HorarioService horarioService;

    @InjectMocks private HorarioController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_ENTRENADOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("crear responde 201 y resuelve el entrenador del username autenticado")
    void crear_devuelve_201() throws Exception {
        autenticarComo("carlos@sged.test");
        when(horarioService.crear(eq("carlos@sged.test"), any()))
                .thenReturn(new HorarioResponse(1L, 5L, "SUB-12", 1, LocalTime.of(16, 0), LocalTime.of(18, 0), "Cancha 1", null, true));

        mockMvc.perform(post("/api/horarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\":5,\"diaSemana\":1,\"horaInicio\":\"16:00:00\",\"horaFin\":\"18:00:00\",\"campo\":\"Cancha 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoria").value("SUB-12"));
    }

    @Test
    @DisplayName("mios devuelve la lista del entrenador autenticado")
    void mios_devuelve_200() throws Exception {
        autenticarComo("carlos@sged.test");
        when(horarioService.misHorarios("carlos@sged.test")).thenReturn(List.of(
                new HorarioResponse(1L, 5L, "SUB-12", 1, LocalTime.of(16, 0), LocalTime.of(18, 0), null, null, true)));

        mockMvc.perform(get("/api/horarios/mios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("desactivar responde 204")
    void desactivar_devuelve_204() throws Exception {
        autenticarComo("carlos@sged.test");

        mockMvc.perform(delete("/api/horarios/1"))
                .andExpect(status().isNoContent());

        verify(horarioService).desactivar("carlos@sged.test", 1L);
    }

    @Test
    @DisplayName("desactivar un horario ajeno responde 404, no 403: mismo criterio IDOR del resto de la app")
    void desactivar_ajeno_devuelve_404() throws Exception {
        autenticarComo("carlos@sged.test");
        doThrow(new RecursoNoEncontradoException("Horario no encontrado con id: 99"))
                .when(horarioService).desactivar("carlos@sged.test", 99L);

        mockMvc.perform(delete("/api/horarios/99"))
                .andExpect(status().isNotFound());
    }
}
