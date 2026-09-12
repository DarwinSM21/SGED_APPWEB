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
import org.uteq.backend.deportivo.match.controller.MatchController;
import org.uteq.backend.deportivo.match.dto.RosterDtos.LineupResponse;
import org.uteq.backend.deportivo.match.dto.RosterDtos.LineupFeedbackResponse;
import org.uteq.backend.deportivo.match.dto.MatchDtos.MatchPageResponse;
import org.uteq.backend.deportivo.match.dto.MatchDtos.MatchResponse;
import org.uteq.backend.deportivo.match.service.LineupService;
import org.uteq.backend.deportivo.match.service.MatchService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {
    private MockMvc mockMvc;

    @Mock private MatchService matchService;
    @Mock private LineupService lineupService;

    @InjectMocks private MatchController partidoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(partidoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MatchResponse partido() {
        return new MatchResponse(1L, 3L, "SUB-12", LocalDate.of(2026, 4, 12), null,
                null, null, "Amistoso", "PENDIENTE", false, 0, false, null);
    }

    private LineupResponse alineacion(boolean guardada) {
        return new LineupResponse(1L, 3L, "SUB-12", LocalDate.of(2026, 4, 12), guardada,
                null, null, null, List.of(), List.of(), List.of(), List.of(), 11, false);
    }

    @Test
    @DisplayName("GET /api/partidos - lista paginada, filtro de categoría opcional")
    void listar_devuelve_200() throws Exception {
        when(matchService.list(isNull(), eq(0), eq(20)))
                .thenReturn(new MatchPageResponse(List.of(partido()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/partidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].categoria").value("SUB-12"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("GET /api/partidos?idCategoria=3 - propaga el filtro")
    void listar_con_filtro_categoria() throws Exception {
        when(matchService.list(eq(3L), eq(0), eq(20)))
                .thenReturn(new MatchPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/partidos").param("idCategoria", "3"))
                .andExpect(status().isOk());

        verify(matchService).list(3L, 0, 20);
    }

    @Test
    @DisplayName("GET /api/partidos/{id} - detalle del partido")
    void ver_devuelve_200() throws Exception {
        when(matchService.findById(1L)).thenReturn(partido());

        mockMvc.perform(get("/api/partidos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPartido").value(1))
                .andExpect(jsonPath("$.resultado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("GET /api/partidos/{id} - 404 si no existe")
    void ver_inexistente_da_404() throws Exception {
        when(matchService.findById(99L)).thenThrow(new ResourceNotFoundException("No existe el partido 99"));

        mockMvc.perform(get("/api/partidos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/partidos - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(matchService.create(any())).thenReturn(partido());

        mockMvc.perform(post("/api/partidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\":3,\"fecha\":\"2026-04-12\",\"observacion\":\"Amistoso\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPartido").value(1));
    }

    @Test
    @DisplayName("POST /api/partidos - sin categoría ni fecha da 422")
    void crear_sin_datos_obligatorios_da_422() throws Exception {
        mockMvc.perform(post("/api/partidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PUT /api/partidos/{id}/resultado - registra el marcador y cierra")
    void registrarResultado_devuelve_200() throws Exception {
        when(matchService.registerResult(eq(1L), any())).thenReturn(partido());

        mockMvc.perform(put("/api/partidos/1/resultado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"golesFavor\":2,\"golesContra\":1}"))
                .andExpect(status().isOk());

        verify(matchService).registerResult(eq(1L), any());
    }

    @Test
    @DisplayName("POST /api/partidos/{id}/reapertura - reabre un partido cerrado")
    void reabrir_devuelve_200() throws Exception {
        when(matchService.reopen(1L)).thenReturn(partido());

        mockMvc.perform(post("/api/partidos/1/reapertura"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/partidos/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(matchService).delete(1L);

        mockMvc.perform(delete("/api/partidos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/partidos/{id}/alineacion - once guardado o sugerido")
    void verAlineacion_devuelve_200() throws Exception {
        when(lineupService.view(1L)).thenReturn(alineacion(true));

        mockMvc.perform(get("/api/partidos/1/alineacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardada").value(true))
                .andExpect(jsonPath("$.cupoTitulares").value(11));
    }

    @Test
    @DisplayName("PUT /api/partidos/{id}/alineacion - guarda el once del entrenador")
    void guardarAlineacion_devuelve_200() throws Exception {
        when(lineupService.save(eq(1L), any())).thenReturn(alineacion(true));

        mockMvc.perform(put("/api/partidos/1/alineacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadores\":[{\"idEstudiante\":5,\"titular\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardada").value(true));
    }

    @Test
    @DisplayName("DELETE /api/partidos/{id}/alineacion - vuelve a la sugerencia del sistema")
    void restablecerAlineacion_devuelve_200() throws Exception {
        when(lineupService.reset(1L)).thenReturn(alineacion(false));

        mockMvc.perform(delete("/api/partidos/1/alineacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardada").value(false));
    }

    @Test
    @DisplayName("POST /api/partidos/{id}/alineacion/feedback - comentario de IA a demanda")
    void feedback_devuelve_200() throws Exception {
        when(lineupService.feedback(1L))
                .thenReturn(new LineupFeedbackResponse("Buen equilibrio en el mediocampo", true, null));

        mockMvc.perform(post("/api/partidos/1/alineacion/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.comentario").value("Buen equilibrio en el mediocampo"));
    }
}
