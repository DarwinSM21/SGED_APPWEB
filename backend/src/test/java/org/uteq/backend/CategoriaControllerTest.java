package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.uteq.backend.deportivo.categoria.controller.CategoriaController;
import org.uteq.backend.deportivo.categoria.dto.CategoriaRequest;
import org.uteq.backend.deportivo.categoria.dto.CategoriaResponse;
import org.uteq.backend.deportivo.categoria.service.CategoriaService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private CategoriaController categoriaController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(categoriaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private CategoriaResponse respuesta() {
        return new CategoriaResponse(1L, "Sub-12", (short) 10, (short) 12, "Formativa", true, Instant.now());
    }

    @Test
    @DisplayName("GET /api/categorias - lista paginada")
    void listarPaginado_devuelve_200() throws Exception {
        when(categoriaService.listarPaginado(any())).thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Sub-12"));
    }

    @Test
    @DisplayName("GET /api/categorias/activas - lista sin paginar")
    void listarActivas_devuelve_200() throws Exception {
        when(categoriaService.listarTodasActivas()).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/categorias/activas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Sub-12"));
    }

    @Test
    @DisplayName("GET /api/categorias/{id} - 404 si no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(categoriaService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/categorias/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/categorias - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(categoriaService.crear(any(CategoriaRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sub-12\",\"edadMin\":10,\"edadMax\":12,\"descripcion\":\"Formativa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Sub-12"));
    }

    @Test
    @DisplayName("POST /api/categorias - datos invalidos da 422")
    void crear_con_datos_invalidos_da_422() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"edadMin\":null,\"edadMax\":12}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/categorias - regla de negocio invalida da 400")
    void crear_con_edad_invalida_da_400() throws Exception {
        when(categoriaService.crear(any(CategoriaRequest.class)))
                .thenThrow(new IllegalArgumentException("La edad máxima debe ser mayor a la edad mínima"));

        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        // El nombre va valido a proposito: esta prueba mira la validacion de
                        // EDADES (minima mayor que maxima). Con "Sub-X" fallaba antes por
                        // formato de nombre y devolvia 422, tapando lo que se queria probar.
                        .content("{\"nombre\":\"SUB-12\",\"edadMin\":15,\"edadMax\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/categorias/{id} - edita y devuelve 200")
    void editar_devuelve_200() throws Exception {
        when(categoriaService.editar(eq(1L), any(CategoriaRequest.class))).thenReturn(respuesta());

        mockMvc.perform(put("/api/categorias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sub-12\",\"edadMin\":10,\"edadMax\":12,\"descripcion\":\"Formativa\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/categorias/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(categoriaService).eliminar(1L);

        mockMvc.perform(delete("/api/categorias/1"))
                .andExpect(status().isNoContent());
    }
}
