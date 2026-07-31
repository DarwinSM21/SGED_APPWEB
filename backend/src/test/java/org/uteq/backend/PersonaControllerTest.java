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
import org.uteq.backend.seguridad.persona.controller.PersonaController;
import org.uteq.backend.seguridad.persona.dto.PersonaRequest;
import org.uteq.backend.seguridad.persona.dto.PersonaResponse;
import org.uteq.backend.seguridad.persona.service.PersonaService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PersonaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PersonaService personaService;

    @InjectMocks
    private PersonaController personaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(personaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private PersonaResponse respuesta() {
        return new PersonaResponse(1L, "Maria", "Lopez", "1234567890", "maria@sged.test",
                "0999999999", null, LocalDate.of(2012, 5, 10), true, Instant.now());
    }

    @Test
    @DisplayName("GET /api/personas - lista paginada")
    void listar_devuelve_200() throws Exception {
        when(personaService.listar(any())).thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/personas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Maria"));
    }

    @Test
    @DisplayName("GET /api/personas/{id} - 404 si no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(personaService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/personas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/personas/cedula/{cedula} - devuelve la persona")
    void buscarPorCedula_devuelve_200() throws Exception {
        when(personaService.buscarPorCedula("1234567890")).thenReturn(respuesta());

        mockMvc.perform(get("/api/personas/cedula/1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cedula").value("1234567890"));
    }

    @Test
    @DisplayName("POST /api/personas - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(personaService.crear(any(PersonaRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Maria\",\"apellido\":\"Lopez\",\"cedula\":\"1234567890\",\"correo\":\"maria@sged.test\",\"telefono\":\"0999999999\",\"fechaNacimiento\":\"2012-05-10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Maria"));
    }

    @Test
    @DisplayName("POST /api/personas - cedula con formato invalido da 422")
    void crear_con_cedula_invalida_da_422() throws Exception {
        mockMvc.perform(post("/api/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Maria\",\"apellido\":\"Lopez\",\"cedula\":\"abc\",\"correo\":\"maria@sged.test\",\"fechaNacimiento\":\"2012-05-10\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/personas - cedula duplicada da 400")
    void crear_con_cedula_duplicada_da_400() throws Exception {
        when(personaService.crear(any(PersonaRequest.class)))
                .thenThrow(new IllegalArgumentException("Ya existe una persona registrada con la cédula: 1234567890"));

        mockMvc.perform(post("/api/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Maria\",\"apellido\":\"Lopez\",\"cedula\":\"1234567890\",\"correo\":\"maria@sged.test\",\"fechaNacimiento\":\"2012-05-10\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/personas/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(personaService).eliminar(1L);

        mockMvc.perform(delete("/api/personas/1"))
                .andExpect(status().isNoContent());
    }
}
