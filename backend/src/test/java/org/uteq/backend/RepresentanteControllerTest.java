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
import org.uteq.backend.academico.representante.controller.RepresentanteController;
import org.uteq.backend.academico.representante.dto.RepresentanteRequest;
import org.uteq.backend.academico.representante.dto.RepresentanteResponse;
import org.uteq.backend.academico.representante.service.RepresentanteService;
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
class RepresentanteControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RepresentanteService representanteService;

    @InjectMocks
    private RepresentanteController representanteController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(representanteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private RepresentanteResponse respuesta() {
        return new RepresentanteResponse(1L, 1L, "Rosa", "Vera", "1111111111", "0999999999", "Comerciante", true);
    }

    @Test
    @DisplayName("GET /api/representantes - lista paginada")
    void listar_devuelve_200() throws Exception {
        when(representanteService.listar(any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/representantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombrePersona").value("Rosa"));
    }

    @Test
    @DisplayName("GET /api/representantes/{id} - 404 si no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(representanteService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/representantes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/representantes - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(representanteService.crear(any(RepresentanteRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/representantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPersona\":1,\"ocupacion\":\"Comerciante\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombrePersona").value("Rosa"));
    }

    @Test
    @DisplayName("POST /api/representantes - datos invalidos da 422")
    void crear_con_datos_invalidos_da_422() throws Exception {
        mockMvc.perform(post("/api/representantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ocupacion\":\"Comerciante\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/representantes - regla de negocio invalida da 400")
    void crear_persona_duplicada_da_400() throws Exception {
        when(representanteService.crear(any(RepresentanteRequest.class)))
                .thenThrow(new IllegalArgumentException("La persona ya está registrada como representante"));

        mockMvc.perform(post("/api/representantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPersona\":1,\"ocupacion\":\"Comerciante\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/representantes/{id} - edita y devuelve 200")
    void editar_devuelve_200() throws Exception {
        when(representanteService.editar(eq(1L), any(RepresentanteRequest.class))).thenReturn(respuesta());

        mockMvc.perform(put("/api/representantes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPersona\":1,\"ocupacion\":\"Comerciante\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/representantes/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(representanteService).eliminar(1L);

        mockMvc.perform(delete("/api/representantes/1"))
                .andExpect(status().isNoContent());
    }
}
