package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.usuario.controller.UsuarioController;
import org.uteq.backend.seguridad.usuario.dto.UsuarioPageResponse;
import org.uteq.backend.seguridad.usuario.dto.UsuarioRequest;
import org.uteq.backend.seguridad.usuario.dto.UsuarioResponse;
import org.uteq.backend.seguridad.usuario.service.UsuarioService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    @BeforeEach
    void setUp() {
        // standaloneSetup no aplica @PreAuthorize (no hay contexto de Spring Security):
        // esta prueba cubre el mapeo HTTP del controller, no la autorizacion,
        // que ya se verifica contra el servidor real en docs/mediciones/sec/.
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private UsuarioResponse respuesta() {
        return new UsuarioResponse(1L, 1L, "Ana", "Torres", "ana@sged.test", 1L, "ACTIVO",
                "ana.torres", List.of(), null, true, OffsetDateTime.now());
    }

    @Test
    @DisplayName("GET /api/usuarios - lista paginada")
    void listar_devuelve_200() throws Exception {
        when(usuarioService.listar(any())).thenReturn(new UsuarioPageResponse<>(List.of(respuesta()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("ana.torres"));
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} - 404 si no existe")
    void buscarPorId_inexistente_da_404() throws Exception {
        when(usuarioService.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/usuarios - crea y devuelve 201")
    void crear_devuelve_201() throws Exception {
        when(usuarioService.crear(any(UsuarioRequest.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPersona\":1,\"idEstadoGeneral\":1,\"username\":\"ana.torres\",\"password\":\"clave123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ana.torres"));
    }

    @Test
    @DisplayName("POST /api/usuarios - username duplicado da 400")
    void crear_username_duplicado_da_400() throws Exception {
        when(usuarioService.crear(any(UsuarioRequest.class)))
                .thenThrow(new IllegalArgumentException("El nombre de usuario ya se encuentra registrado"));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPersona\":1,\"idEstadoGeneral\":1,\"username\":\"ana.torres\",\"password\":\"clave123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/usuarios - contrasena corta da 422")
    void crear_con_contrasena_corta_da_422() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPersona\":1,\"idEstadoGeneral\":1,\"username\":\"abcd\",\"password\":\"12\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id} - elimina y devuelve 204")
    void eliminar_devuelve_204() throws Exception {
        doNothing().when(usuarioService).eliminar(1L);

        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isNoContent());
    }
}
