package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.inventario.asignacion.controller.AsignacionController;
import org.uteq.backend.inventario.asignacion.dto.AsignacionDtos.AsignacionRequest;
import org.uteq.backend.inventario.asignacion.dto.AsignacionDtos.AsignacionResponse;
import org.uteq.backend.inventario.asignacion.entity.Asignacion.EstadoAsignacion;
import org.uteq.backend.inventario.asignacion.entity.Asignacion.TipoDestinatario;
import org.uteq.backend.inventario.asignacion.service.AsignacionService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * R-07 (informe de evaluacion de calidad, D-09): AsignacionController
 * estaba en la lista de controladores con 0% de cobertura.
 */
@ExtendWith(MockitoExtension.class)
class AsignacionControllerTest {

    private MockMvc mockMvc;

    @Mock private AsignacionService asignacionService;

    @InjectMocks private AsignacionController asignacionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(asignacionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private AsignacionResponse respuesta() {
        return new AsignacionResponse(1L, 1L, "Balón N5", 1, TipoDestinatario.ESTUDIANTE,
                5L, "Andres Vera", null, null, LocalDate.now(), null, null,
                EstadoAsignacion.ASIGNADO, "recepcion@sged.test", null, Instant.now());
    }

    @Test
    @DisplayName("GET /api/inventario/asignaciones - lista paginada")
    void listar_devuelve_200() throws Exception {
        when(asignacionService.listarPaginado(any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/inventario/asignaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].articulo").value("Balón N5"));
    }

    @Test
    @DisplayName("GET /api/inventario/asignaciones/estudiante/{id} - filtra por estudiante")
    void listarPorEstudiante_devuelve_200() throws Exception {
        when(asignacionService.listarPorEstudiante(eq(5L), any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/inventario/asignaciones/estudiante/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/inventario/asignaciones/entrenador/{id} - filtra por entrenador")
    void listarPorEntrenador_devuelve_200() throws Exception {
        when(asignacionService.listarPorEntrenador(eq(3L), any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/inventario/asignaciones/entrenador/3"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/inventario/asignaciones - usa el username autenticado, no uno del body")
    void crear_usa_el_username_autenticado() throws Exception {
        autenticarComo("recepcion@sged.test");
        when(asignacionService.crear(any(AsignacionRequest.class), eq("recepcion@sged.test")))
                .thenReturn(respuesta());

        mockMvc.perform(post("/api/inventario/asignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idArticulo\":1,\"cantidad\":1,\"tipoDestinatario\":\"ESTUDIANTE\",\"idEstudiante\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articulo").value("Balón N5"));

        verify(asignacionService).crear(any(AsignacionRequest.class), eq("recepcion@sged.test"));
    }

    @Test
    @DisplayName("POST /api/inventario/asignaciones - cantidad menor a 1 da 422")
    void crear_con_cantidad_invalida_da_422() throws Exception {
        autenticarComo("recepcion@sged.test");

        mockMvc.perform(post("/api/inventario/asignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idArticulo\":1,\"cantidad\":0,\"tipoDestinatario\":\"ESTUDIANTE\",\"idEstudiante\":5}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /api/inventario/asignaciones/{id}/devolver - marca la devolucion")
    void devolver_devuelve_200() throws Exception {
        when(asignacionService.devolver(eq(1L), any())).thenReturn(respuesta());

        mockMvc.perform(patch("/api/inventario/asignaciones/1/devolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"DEVUELTO\"}"))
                .andExpect(status().isOk());
    }
}
