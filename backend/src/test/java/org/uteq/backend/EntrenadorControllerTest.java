package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.entrenador.controller.EntrenadorController;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorPageResponse;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorRequest;
import org.uteq.backend.deportivo.entrenador.dto.EntrenadorResponse;
import org.uteq.backend.deportivo.entrenador.service.EntrenadorService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EntrenadorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EntrenadorService entrenadorService;

    @InjectMocks
    private EntrenadorController entrenadorController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(entrenadorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private EntrenadorResponse respuesta() {
        return new EntrenadorResponse(1L, 1L, "Carlos", "Mora", "1234567890", "carlos@sged.test",
                "0999999999", 1L, "carlos.mora", "Fisica", (short) 5, null, true, OffsetDateTime.now());
    }

    @Test
    @DisplayName("GET /api/entrenadores - lista paginada")
    void listar_devuelve_200() throws Exception {
        when(entrenadorService.listar(any())).thenReturn(new EntrenadorPageResponse<>(List.of(respuesta()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/entrenadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Carlos"));
    }

    @Test
    @DisplayName("GET /api/entrenadores/{id} - 404 si no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(entrenadorService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/entrenadores/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/entrenadores - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(entrenadorService.crear(any(EntrenadorRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/entrenadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPersona\":1,\"idUsuario\":1,\"especialidad\":\"Fisica\",\"experienciaAnios\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Carlos"));
    }

    @Test
    @DisplayName("POST /api/entrenadores - datos invalidos da 422")
    void crear_con_datos_invalidos_da_422() throws Exception {
        mockMvc.perform(post("/api/entrenadores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPersona\":null,\"idUsuario\":null}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("DELETE /api/entrenadores/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(entrenadorService).eliminar(1L);

        mockMvc.perform(delete("/api/entrenadores/1"))
                .andExpect(status().isNoContent());
    }
}
