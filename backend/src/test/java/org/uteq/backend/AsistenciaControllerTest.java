package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.uteq.backend.academico.asistencia.controller.AsistenciaController;
import org.uteq.backend.academico.asistencia.dto.AsistenciaRequest;
import org.uteq.backend.academico.asistencia.dto.AsistenciaResponse;
import org.uteq.backend.academico.asistencia.service.AsistenciaService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AsistenciaService asistenciaService;

    @InjectMocks
    private AsistenciaController asistenciaController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(asistenciaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private AsistenciaResponse respuesta() {
        return new AsistenciaResponse(1L, 1L, "Entrenamiento tecnico", LocalDate.now(),
                1L, "EST-001", "Juan", "Perez", 1L, "PRESENTE", LocalDate.now());
    }

    @Test
    @DisplayName("GET /api/asistencias - lista paginada")
    void listar_devuelve_200() throws Exception {
        when(asistenciaService.listar(any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/asistencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombreEstadoAsistencia").value("PRESENTE"));
    }

    @Test
    @DisplayName("GET /api/asistencias/estudiante/{id} - lista sin paginar")
    void listarPorEstudiante_devuelve_200() throws Exception {
        when(asistenciaService.listarPorEstudiante(1L)).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/asistencias/estudiante/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoEstudiante").value("EST-001"));
    }

    @Test
    @DisplayName("GET /api/asistencias/sesion/{id} - lista sin paginar")
    void listarPorSesion_devuelve_200() throws Exception {
        when(asistenciaService.listarPorSesion(1L)).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/asistencias/sesion/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tituloSesion").value("Entrenamiento tecnico"));
    }

    @Test
    @DisplayName("GET /api/asistencias/{id} - 404 si no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(asistenciaService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/asistencias/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/asistencias - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(asistenciaService.crear(any(AsistenciaRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/asistencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSesionEntrenamiento\":1,\"idEstudiante\":1,\"idEstadoAsistencia\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreEstadoAsistencia").value("PRESENTE"));
    }

    @Test
    @DisplayName("POST /api/asistencias - datos invalidos da 422")
    void crear_con_datos_invalidos_da_422() throws Exception {
        mockMvc.perform(post("/api/asistencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/asistencias - referencia inexistente da 404")
    void crear_con_sesion_inexistente_da_404() throws Exception {
        when(asistenciaService.crear(any(AsistenciaRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Sesión de entrenamiento no encontrada con id: 1"));

        mockMvc.perform(post("/api/asistencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSesionEntrenamiento\":1,\"idEstudiante\":1,\"idEstadoAsistencia\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/asistencias/{id} - edita y devuelve 200")
    void editar_devuelve_200() throws Exception {
        when(asistenciaService.editar(eq(1L), any(AsistenciaRequest.class))).thenReturn(respuesta());

        mockMvc.perform(put("/api/asistencias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSesionEntrenamiento\":1,\"idEstudiante\":1,\"idEstadoAsistencia\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/asistencias/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(asistenciaService).eliminar(1L);

        mockMvc.perform(delete("/api/asistencias/1"))
                .andExpect(status().isNoContent());
    }
}
