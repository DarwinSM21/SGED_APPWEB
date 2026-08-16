package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.especialidad.controller.EspecialidadController;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadRequest;
import org.uteq.backend.deportivo.especialidad.dto.EspecialidadResponse;
import org.uteq.backend.deportivo.especialidad.service.EspecialidadService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * R-07 (informe de evaluacion de calidad, D-09): EspecialidadController
 * estaba en la lista de controladores con 0% de cobertura.
 */
@ExtendWith(MockitoExtension.class)
class EspecialidadControllerTest {

    private MockMvc mockMvc;

    @Mock private EspecialidadService especialidadService;

    @InjectMocks private EspecialidadController especialidadController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(especialidadController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private EspecialidadResponse respuesta() {
        return new EspecialidadResponse(1L, "Portero", true, Instant.now());
    }

    @Test
    @DisplayName("GET /api/especialidades - lista paginada")
    void listarPaginado_devuelve_200() throws Exception {
        when(especialidadService.listarPaginado(any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/especialidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Portero"));
    }

    @Test
    @DisplayName("GET /api/especialidades/activas - lista sin paginar")
    void listarActivas_devuelve_200() throws Exception {
        when(especialidadService.listarTodasActivas()).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/especialidades/activas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idEspecialidad").value(1));
    }

    @Test
    @DisplayName("GET /api/especialidades/{id} - 404 si no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(especialidadService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/especialidades/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/especialidades - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(especialidadService.crear(any(EspecialidadRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/especialidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Portero\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Portero"));
    }

    @Test
    @DisplayName("POST /api/especialidades - nombre en blanco da 422")
    void crear_con_nombre_en_blanco_da_422() throws Exception {
        mockMvc.perform(post("/api/especialidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PUT /api/especialidades/{id} - edita y devuelve 200")
    void editar_devuelve_200() throws Exception {
        when(especialidadService.editar(org.mockito.ArgumentMatchers.eq(1L), any(EspecialidadRequest.class)))
                .thenReturn(respuesta());

        mockMvc.perform(put("/api/especialidades/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Portero\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/especialidades/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(especialidadService).eliminar(1L);

        mockMvc.perform(delete("/api/especialidades/1"))
                .andExpect(status().isNoContent());
    }
}
