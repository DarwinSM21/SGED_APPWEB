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
import org.uteq.backend.inventario.articulo.controller.ArticuloController;
import org.uteq.backend.inventario.articulo.dto.ArticuloDtos.ArticuloRequest;
import org.uteq.backend.inventario.articulo.dto.ArticuloDtos.ArticuloResponse;
import org.uteq.backend.inventario.articulo.dto.ArticuloDtos.StockBajoResponse;
import org.uteq.backend.inventario.articulo.entity.Articulo.TipoArticulo;
import org.uteq.backend.inventario.articulo.service.ArticuloService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ArticuloControllerTest {
    private MockMvc mockMvc;

    @Mock private ArticuloService articuloService;

    @InjectMocks private ArticuloController articuloController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(articuloController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private ArticuloResponse respuesta() {
        return new ArticuloResponse(1L, "Balón N5", TipoArticulo.BALON, null, null,
                10, 3, "unidad", true, Instant.now());
    }

    @Test
    @DisplayName("GET /api/inventario/articulos - lista paginada")
    void listarPaginado_devuelve_200() throws Exception {
        when(articuloService.listarPaginado(any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/inventario/articulos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Balón N5"));
    }

    @Test
    @DisplayName("GET /api/inventario/articulos/activos - lista sin paginar")
    void listarActivos_devuelve_200() throws Exception {
        when(articuloService.listarActivos()).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/inventario/articulos/activos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("BALON"));
    }

    @Test
    @DisplayName("GET /api/inventario/articulos/stock-bajo - devuelve el resumen")
    void stockBajo_devuelve_200() throws Exception {
        when(articuloService.stockBajo()).thenReturn(new StockBajoResponse(1, List.of(respuesta())));

        mockMvc.perform(get("/api/inventario/articulos/stock-bajo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("GET /api/inventario/articulos/{id} - 404 si no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(articuloService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/inventario/articulos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/inventario/articulos - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(articuloService.crear(any(ArticuloRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/inventario/articulos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Balón N5\",\"tipo\":\"BALON\",\"stockMinimo\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Balón N5"));
    }

    @Test
    @DisplayName("POST /api/inventario/articulos - sin tipo da 422")
    void crear_sin_tipo_da_422() throws Exception {
        mockMvc.perform(post("/api/inventario/articulos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Balón N5\",\"stockMinimo\":3}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PUT /api/inventario/articulos/{id} - edita y devuelve 200")
    void editar_devuelve_200() throws Exception {
        when(articuloService.editar(eq(1L), any(ArticuloRequest.class))).thenReturn(respuesta());

        mockMvc.perform(put("/api/inventario/articulos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Balón N5\",\"tipo\":\"BALON\",\"stockMinimo\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/inventario/articulos/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(articuloService).eliminar(1L);

        mockMvc.perform(delete("/api/inventario/articulos/1"))
                .andExpect(status().isNoContent());
    }
}
