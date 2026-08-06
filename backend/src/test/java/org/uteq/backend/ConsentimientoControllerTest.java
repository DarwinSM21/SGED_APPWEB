package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.academico.representante.controller.ConsentimientoController;
import org.uteq.backend.academico.representante.dto.ConsentimientoDtos.ConsentimientoResponse;
import org.uteq.backend.academico.representante.entity.Consentimiento;
import org.uteq.backend.academico.representante.service.ConsentimientoService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ConsentimientoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConsentimientoService consentimientoService;

    @InjectMocks
    private ConsentimientoController consentimientoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(consentimientoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        var auth = new UsernamePasswordAuthenticationToken(
                "admin@sged.test", null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private ConsentimientoResponse respuesta(boolean vigente) {
        return new ConsentimientoResponse(1L, 1L, 10L, Consentimiento.ALCANCE_INFORMES,
                OffsetDateTime.now(), "admin@sged.test", vigente ? null : OffsetDateTime.now(), vigente);
    }

    @Test
    @DisplayName("POST /api/consentimientos - otorga y usa el username autenticado, no uno del cuerpo")
    void otorgar_devuelve_201_con_admin_autenticado() throws Exception {
        when(consentimientoService.otorgar(any(), eq("admin@sged.test"))).thenReturn(respuesta(true));

        mockMvc.perform(post("/api/consentimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRepresentante\":1,\"idEstudiante\":10,\"alcance\":\"INFORMES\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registradoPorUsername").value("admin@sged.test"))
                .andExpect(jsonPath("$.vigente").value(true));
    }

    @Test
    @DisplayName("POST /api/consentimientos/{id}/revocar - revoca y devuelve 200")
    void revocar_devuelve_200() throws Exception {
        when(consentimientoService.revocar(eq(1L), eq("admin@sged.test"))).thenReturn(respuesta(false));

        mockMvc.perform(post("/api/consentimientos/1/revocar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(false));
    }

    @Test
    @DisplayName("GET /api/consentimientos/estudiante/{id} - lista el historial")
    void listarPorEstudiante_devuelve_200() throws Exception {
        when(consentimientoService.listarPorEstudiante(10L)).thenReturn(List.of(respuesta(true)));

        mockMvc.perform(get("/api/consentimientos/estudiante/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idEstudiante").value(10));
    }
}
