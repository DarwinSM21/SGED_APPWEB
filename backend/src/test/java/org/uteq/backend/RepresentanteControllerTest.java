package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.academico.representante.controller.RepresentanteController;
import org.uteq.backend.academico.representante.dto.RepresentantePageResponse;
import org.uteq.backend.academico.representante.dto.RepresentanteRequest;
import org.uteq.backend.academico.representante.dto.RepresentanteResponse;
import org.uteq.backend.academico.representante.service.RepresentanteService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RepresentanteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RepresentanteService representanteService;

    @InjectMocks
    private RepresentanteController representanteController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(representanteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private RepresentanteResponse respuesta() {
        return new RepresentanteResponse(1L, 1L, "Ana", "Vera", "1234567890", "ana@sged.test",
                1L, "ana.vera@sged.test", "Madre", "0999999999", true, Instant.now(), List.of());
    }

    @Test
    @DisplayName("GET /api/representantes - lista paginada")
    void listar_devuelve_200() throws Exception {
        when(representanteService.listar(any())).thenReturn(new RepresentantePageResponse<>(List.of(respuesta()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/representantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Ana"));
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
                        .content("{\"idPersona\":1,\"idUsuario\":1,\"parentesco\":\"Madre\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    @DisplayName("POST /api/representantes - datos invalidos da 422")
    void crear_con_datos_invalidos_da_422() throws Exception {
        mockMvc.perform(post("/api/representantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPersona\":null,\"idUsuario\":null}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("DELETE /api/representantes/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(representanteService).eliminar(1L);

        mockMvc.perform(delete("/api/representantes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/representantes/{id}/estudiantes/{idEstudiante} - vincula y devuelve 200")
    void vincularEstudiante_devuelve_200() throws Exception {
        when(representanteService.vincularEstudiante(eq(1L), eq(10L), any())).thenReturn(respuesta());

        mockMvc.perform(post("/api/representantes/1/estudiantes/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relacion\":\"Madre\",\"contactoPrincipal\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/representantes/{id}/estudiantes/{idEstudiante} - sin cuerpo tambien vincula")
    void vincularEstudiante_sin_cuerpo_devuelve_200() throws Exception {
        when(representanteService.vincularEstudiante(eq(1L), eq(10L), any())).thenReturn(respuesta());

        mockMvc.perform(post("/api/representantes/1/estudiantes/10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/representantes/{id}/estudiantes/{idEstudiante} - desvincula y devuelve 204")
    void desvincularEstudiante_devuelve_204() throws Exception {
        doNothing().when(representanteService).desvincularEstudiante(anyLong(), anyLong());

        mockMvc.perform(delete("/api/representantes/1/estudiantes/10"))
                .andExpect(status().isNoContent());
    }
}
