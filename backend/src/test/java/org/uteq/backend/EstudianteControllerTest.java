package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.academico.estudiante.service.*;
import org.uteq.backend.academico.estudiante.dto.*;
import org.uteq.backend.academico.estudiante.controller.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas del EstudianteController. Usa Mockito + MockMvc standalone (sin
 * contexto Spring completo, sin cadena de seguridad real).
 */
@ExtendWith(MockitoExtension.class)
class EstudianteControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private EstudianteService estudianteService;
    @InjectMocks private EstudianteController estudianteController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(estudianteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private EstudianteResponse estudianteResponse() {
        return new EstudianteResponse(
                1L,                        
                "Juan",                    
                "Perez",                 
                "SUB-12",                  
                "ACTIVO",              
                "EST-001",            
                LocalDate.now(),           
                new BigDecimal("60.5"),   
                new BigDecimal("1.70"),     
                true,                     
                Instant.now()          
        );
    }

    @Test
    void listar_devuelve_pagina() throws Exception {
        EstudiantePageResponse<EstudianteResponse> pagina =
                new EstudiantePageResponse<>(List.of(estudianteResponse()), 0, 10, 1, 1);
        when(estudianteService.listar(any())).thenReturn(pagina);

        mockMvc.perform(get("/api/estudiantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Juan"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void buscarPorId_existente() throws Exception {
        when(estudianteService.buscarPorId(1L)).thenReturn(estudianteResponse());

        mockMvc.perform(get("/api/estudiantes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("SUB-12"));
    }

    @Test
    void buscarPorId_inexistente_da_404() throws Exception {
        when(estudianteService.buscarPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Estudiante no encontrado con id: 99"));

        mockMvc.perform(get("/api/estudiantes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_devuelve_201() throws Exception {
        when(estudianteService.crear(any())).thenReturn(estudianteResponse());

        mockMvc.perform(post("/api/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("nombre", "Juan");
                            put("apellido", "Perez");
                            put("categoria", "SUB-12");
                        }})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Juan"));
    }

    @Test
    void crear_con_categoria_invalida_da_422() throws Exception {
        mockMvc.perform(post("/api/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("nombre", "Juan");
                            put("apellido", "Perez");
                            put("categoria", "' OR '1'='1");
                        }})))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void editar_actualiza_estudiante() throws Exception {
        when(estudianteService.editar(eq(1L), any())).thenReturn(estudianteResponse());

        mockMvc.perform(put("/api/estudiantes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HashMap<>() {{
                            put("nombre", "Juan");
                            put("apellido", "Perez");
                            put("categoria", "SUB-12");
                        }})))
                .andExpect(status().isOk());
    }

    @Test
    void eliminar_devuelve_204() throws Exception {
        mockMvc.perform(delete("/api/estudiantes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void contarActivos_delega_en_service() throws Exception {
        when(estudianteService.contarActivosPorCategoria(1L)).thenReturn(3L);

        mockMvc.perform(get("/api/estudiantes/conteo/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

}
