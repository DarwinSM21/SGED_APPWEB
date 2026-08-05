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
import org.uteq.backend.academico.estudianteRepresentante.controller.EstudianteRepresentanteController;
import org.uteq.backend.academico.estudianteRepresentante.dto.EstudianteRepresentanteRequest;
import org.uteq.backend.academico.estudianteRepresentante.dto.EstudianteRepresentanteResponse;
import org.uteq.backend.academico.estudianteRepresentante.service.EstudianteRepresentanteService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EstudianteRepresentanteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EstudianteRepresentanteService estudianteRepresentanteService;

    @InjectMocks
    private EstudianteRepresentanteController estudianteRepresentanteController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(estudianteRepresentanteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private EstudianteRepresentanteResponse respuesta() {
        return new EstudianteRepresentanteResponse(1L, 1L, "EST-001", "Juan", "Perez",
                1L, "Rosa", "Vera", "0999999999", "Madre", true);
    }

    @Test
    @DisplayName("GET /api/estudiante-representante - lista paginada")
    void listar_devuelve_200() throws Exception {
        when(estudianteRepresentanteService.listar(any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/estudiante-representante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].relacion").value("Madre"));
    }

    @Test
    @DisplayName("GET /api/estudiante-representante/estudiante/{id} - lista sin paginar")
    void listarPorEstudiante_devuelve_200() throws Exception {
        when(estudianteRepresentanteService.listarPorEstudiante(1L)).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/estudiante-representante/estudiante/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreRepresentante").value("Rosa"));
    }

    @Test
    @DisplayName("GET /api/estudiante-representante/{id} - 404 si no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(estudianteRepresentanteService.buscarPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/estudiante-representante/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/estudiante-representante - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(estudianteRepresentanteService.crear(any(EstudianteRepresentanteRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/estudiante-representante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"idRepresentante\":1,\"relacion\":\"Madre\",\"contactoPrincipal\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relacion").value("Madre"));
    }

    @Test
    @DisplayName("POST /api/estudiante-representante - datos invalidos da 422")
    void crear_con_datos_invalidos_da_422() throws Exception {
        mockMvc.perform(post("/api/estudiante-representante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRepresentante\":1,\"relacion\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/estudiante-representante - relacion duplicada da 400")
    void crear_relacion_duplicada_da_400() throws Exception {
        when(estudianteRepresentanteService.crear(any(EstudianteRepresentanteRequest.class)))
                .thenThrow(new IllegalArgumentException("Ese representante ya está vinculado a este estudiante"));

        mockMvc.perform(post("/api/estudiante-representante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"idRepresentante\":1,\"relacion\":\"Madre\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/estudiante-representante/{id} - edita y devuelve 200")
    void editar_devuelve_200() throws Exception {
        when(estudianteRepresentanteService.editar(eq(1L), any(EstudianteRepresentanteRequest.class)))
                .thenReturn(respuesta());

        mockMvc.perform(put("/api/estudiante-representante/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"idRepresentante\":1,\"relacion\":\"Madre\",\"contactoPrincipal\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/estudiante-representante/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(estudianteRepresentanteService).eliminar(1L);

        mockMvc.perform(delete("/api/estudiante-representante/1"))
                .andExpect(status().isNoContent());
    }
}
