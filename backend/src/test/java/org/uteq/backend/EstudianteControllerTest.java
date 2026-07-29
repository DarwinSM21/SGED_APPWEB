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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.academico.estudiante.controller.EstudianteController;
import org.uteq.backend.academico.estudiante.dto.EstudiantePageResponse;
import org.uteq.backend.academico.estudiante.dto.EstudianteRequest;
import org.uteq.backend.academico.estudiante.dto.EstudianteResponse;
import org.uteq.backend.academico.estudiante.service.EstudianteService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EstudianteControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private EstudianteService estudianteService;

    @InjectMocks
    private EstudianteController estudianteController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Soporte para LocalDate e Instant

        mockMvc = MockMvcBuilders
                .standaloneSetup(estudianteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- Métodos de Ayuda para Instanciar DTOs ---

    private EstudianteResponse crearEstudianteResponse() {
        return new EstudianteResponse(
                1L,
                "Juan",
                "Perez",
                "SUB-12",
                "ACTIVO",
                "EST-001",
                LocalDate.now(),
                new BigDecimal("60.50"),
                new BigDecimal("1.70"),
                true,
                Instant.now()
        );
    }

    private EstudianteRequest crearEstudianteRequestValido() {
        return new EstudianteRequest(
                1L,                       // idPersona
                2L,                       // idCategoria
                1L,                       // idEstadoGeneral
                "EST-001",                // codigoEstudiante
                LocalDate.now(),          // fechaIngreso
                new BigDecimal("60.50"),  // peso
                new BigDecimal("1.70")    // altura
        );
    }

    // --- Pruebas Unitarias ---

    @Test
    @DisplayName("GET /api/estudiantes - Listar devuelve página con éxito")
    void listar_devuelve_pagina() throws Exception {
        EstudiantePageResponse<EstudianteResponse> pagina =
                new EstudiantePageResponse<>(List.of(crearEstudianteResponse()), 0, 10, 1, 1);
        
        when(estudianteService.listar(any())).thenReturn(pagina);

        mockMvc.perform(get("/api/estudiantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombrePersona").value("Juan"))
                .andExpect(jsonPath("$.content[0].nombreCategoria").value("SUB-12"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/estudiantes/{id} - Devuelve el estudiante cuando existe")
    void buscarPorId_existente() throws Exception {
        when(estudianteService.buscarPorId(1L)).thenReturn(crearEstudianteResponse());

        mockMvc.perform(get("/api/estudiantes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEstudiante").value(1))
                .andExpect(jsonPath("$.nombreCategoria").value("SUB-12"))
                .andExpect(jsonPath("$.codigoEstudiante").value("EST-001"));
    }

    @Test
    @DisplayName("GET /api/estudiantes/{id} - Devuelve 404 cuando no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(estudianteService.buscarPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Estudiante no encontrado con id: 99"));

        mockMvc.perform(get("/api/estudiantes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/estudiantes - Crea correctamente un estudiante y devuelve 201")
    void crear_devuelve_201() throws Exception {
        EstudianteRequest request = crearEstudianteRequestValido();
        when(estudianteService.crear(any(EstudianteRequest.class))).thenReturn(crearEstudianteResponse());

        mockMvc.perform(post("/api/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombrePersona").value("Juan"))
                .andExpect(jsonPath("$.codigoEstudiante").value("EST-001"));
    }

    @Test
    @DisplayName("POST /api/estudiantes - Falla validación con Request incompleto (Devuelve 400)")
    void crear_con_datos_invalidos_da_400() throws Exception {
        // Objeto request inválido (campos @NotNull nulos)
        EstudianteRequest requestInvalido = new EstudianteRequest(
                null, null, null, "", LocalDate.now().plusDays(1), new BigDecimal("0.00"), new BigDecimal("0.00")
        );

        mockMvc.perform(post("/api/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/estudiantes/{id} - Edita correctamente y devuelve 200")
    void editar_actualiza_estudiante() throws Exception {
        EstudianteRequest request = crearEstudianteRequestValido();
        when(estudianteService.editar(eq(1L), any(EstudianteRequest.class))).thenReturn(crearEstudianteResponse());

        mockMvc.perform(put("/api/estudiantes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEstudiante").value(1));
    }

    @Test
    @DisplayName("DELETE /api/estudiantes/{id} - Elimina (soft delete) y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(estudianteService).eliminar(1L);

        mockMvc.perform(delete("/api/estudiantes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/estudiantes/conteo/categoria/{idCategoria} - Devuelve conteo de activos")
    void contarActivos_delega_en_service() throws Exception {
        when(estudianteService.contarActivosPorCategoria(2L)).thenReturn(5L);

        mockMvc.perform(get("/api/estudiantes/conteo/categoria/2"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
}