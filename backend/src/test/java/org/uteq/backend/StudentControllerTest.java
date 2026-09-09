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
import org.uteq.backend.academico.student.controller.StudentController;
import org.uteq.backend.academico.student.dto.StudentPageResponse;
import org.uteq.backend.academico.student.dto.StudentRequest;
import org.uteq.backend.academico.student.dto.StudentResponse;
import org.uteq.backend.academico.student.service.StudentService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.ResourceNotFoundException;

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
class StudentControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private StudentService estudianteService;

    @InjectMocks
    private StudentController estudianteController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(estudianteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private StudentResponse crearEstudianteResponse() {
        return new StudentResponse(
                1L,
                1L,
                1L,
                1L,
                "Juan",
                "Perez",
                "SUB-12",
                "ACTIVO",
                "EST-001",
                LocalDate.now(),
                new BigDecimal("60.50"),
                new BigDecimal("1.70"),
                null,
                null,
                null,
                true,
                Instant.now()
        );
    }

    private StudentRequest crearEstudianteRequestValido() {
        return new StudentRequest(
                1L,
                2L,
                1L,
                "EST-001",
                LocalDate.now(),
                new BigDecimal("60.50"),
                new BigDecimal("1.70"),
                null
        );
    }

    @Test
    @DisplayName("GET /api/estudiantes - Listar devuelve página con éxito")
    void listar_devuelve_pagina() throws Exception {
        StudentPageResponse<StudentResponse> pagina =
                new StudentPageResponse<>(List.of(crearEstudianteResponse()), 0, 10, 1, 1);

        when(estudianteService.list(any())).thenReturn(pagina);

        mockMvc.perform(get("/api/estudiantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombrePersona").value("Juan"))
                .andExpect(jsonPath("$.content[0].nombreCategoria").value("SUB-12"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/estudiantes/{id} - Devuelve el estudiante cuando existe")
    void buscarPorId_existente() throws Exception {
        when(estudianteService.findById(1L)).thenReturn(crearEstudianteResponse());

        mockMvc.perform(get("/api/estudiantes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEstudiante").value(1))
                .andExpect(jsonPath("$.nombreCategoria").value("SUB-12"))
                .andExpect(jsonPath("$.codigoEstudiante").value("EST-001"));
    }

    @Test
    @DisplayName("RF-11b/H-06 - RECEPCIONISTA no ve peso/altura; ENTRENADOR sí")
    void datos_fisicos_solo_para_administrador_y_entrenador() throws Exception {
        when(estudianteService.findById(1L)).thenReturn(crearEstudianteResponse());

        var entrenador = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "coach", "x", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ENTRENADOR")));
        mockMvc.perform(get("/api/estudiantes/1").principal(entrenador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peso").value(60.50))
                .andExpect(jsonPath("$.altura").value(1.70));

        var recepcion = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "front", "x", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_RECEPCIONISTA")));
        mockMvc.perform(get("/api/estudiantes/1").principal(recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoEstudiante").value("EST-001"))
                .andExpect(jsonPath("$.peso").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.altura").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("GET /api/estudiantes/{id} - Devuelve 404 cuando no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(estudianteService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Estudiante no encontrado con id: 99"));

        mockMvc.perform(get("/api/estudiantes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/estudiantes - Crea correctamente un estudiante y devuelve 201")
    void crear_devuelve_201() throws Exception {
        StudentRequest request = crearEstudianteRequestValido();
        when(estudianteService.create(any(StudentRequest.class))).thenReturn(crearEstudianteResponse());

        mockMvc.perform(post("/api/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombrePersona").value("Juan"))
                .andExpect(jsonPath("$.codigoEstudiante").value("EST-001"));
    }

    @Test
    @DisplayName("POST /api/estudiantes - Falla validación con Request incompleto (Devuelve 400)")
    void crear_con_datos_invalidos_da_422() throws Exception {
        StudentRequest requestInvalido = new StudentRequest(
                null, null, null, "", LocalDate.now().plusDays(1), new BigDecimal("0.00"), new BigDecimal("0.00"), null
        );

        mockMvc.perform(post("/api/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PUT /api/estudiantes/{id} - Edita correctamente y devuelve 200")
    void editar_actualiza_estudiante() throws Exception {
        StudentRequest request = crearEstudianteRequestValido();
        when(estudianteService.update(eq(1L), any(StudentRequest.class))).thenReturn(crearEstudianteResponse());

        mockMvc.perform(put("/api/estudiantes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEstudiante").value(1));
    }

    @Test
    @DisplayName("DELETE /api/estudiantes/{id} - Elimina (soft delete) y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(estudianteService).delete(1L);

        mockMvc.perform(delete("/api/estudiantes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/estudiantes/conteo/categoria/{idCategoria} - Devuelve conteo de activos")
    void contarActivos_delega_en_service() throws Exception {
        when(estudianteService.countActiveByCategory(2L)).thenReturn(5L);

        mockMvc.perform(get("/api/estudiantes/conteo/categoria/2"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("POST /api/estudiantes/operaciones/desactivar-categoria - Delega en service")
    void desactivarCategoria_delega_en_service() throws Exception {
        doNothing().when(estudianteService).deactivateByCategory(2L);

        mockMvc.perform(post("/api/estudiantes/operaciones/desactivar-categoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("2"))
                .andExpect(status().isOk());
    }
}
