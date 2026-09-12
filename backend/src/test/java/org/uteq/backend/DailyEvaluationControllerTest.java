package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.evaluation.controller.DailyEvaluationController;
import org.uteq.backend.deportivo.evaluation.dto.EvaluationDtos.SessionEvaluationResponse;
import org.uteq.backend.deportivo.evaluation.dto.EvaluationDtos.SavePlayerRequest;
import org.uteq.backend.deportivo.evaluation.service.DailyEvaluationService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DailyEvaluationControllerTest {
    private MockMvc mockMvc;

    @Mock private DailyEvaluationService evaluacionService;

    @InjectMocks private DailyEvaluationController evaluacionDiariaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(evaluacionDiariaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/evaluaciones/sesion/{id} - abre la pantalla de evaluacion")
    void abrir_devuelve_200() throws Exception {
        when(evaluacionService.open(1L)).thenReturn(
                new SessionEvaluationResponse(null, 1L, LocalDate.now(), "SUB-12", "EN_CURSO",
                        List.of(), List.of(), null));

        mockMvc.perform(get("/api/evaluaciones/sesion/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("SUB-12"));
    }

    @Test
    @DisplayName("GET /api/evaluaciones/sesion/{id} - 404 si la sesion no existe")
    void abrir_sesion_inexistente_da_404() throws Exception {
        when(evaluacionService.open(99L)).thenThrow(new ResourceNotFoundException("no existe"));

        mockMvc.perform(get("/api/evaluaciones/sesion/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/evaluaciones/sesion/{id}/jugadores - guarda y devuelve 204")
    void guardarJugador_devuelve_204() throws Exception {
        doNothing().when(evaluacionService).savePlayer(eq(1L), any(SavePlayerRequest.class));

        mockMvc.perform(put("/api/evaluaciones/sesion/1/jugadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":5,\"puntajes\":[{\"idCriterio\":1,\"puntaje\":8.5}]}"))
                .andExpect(status().isNoContent());

        verify(evaluacionService).savePlayer(eq(1L), any(SavePlayerRequest.class));
    }

    @Test
    @DisplayName("PUT /api/evaluaciones/sesion/{id}/jugadores - sin idEstudiante da 422")
    void guardarJugador_sin_idEstudiante_da_422() throws Exception {
        mockMvc.perform(put("/api/evaluaciones/sesion/1/jugadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"puntajes\":[{\"idCriterio\":1,\"puntaje\":8.5}]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/evaluaciones/sesion/{id}/finalizar - cierra y devuelve 204")
    void finalizar_devuelve_204() throws Exception {
        doNothing().when(evaluacionService).finish(1L, "Buen entrenamiento");

        mockMvc.perform(post("/api/evaluaciones/sesion/1/finalizar")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Buen entrenamiento"))
                .andExpect(status().isNoContent());
    }
}
