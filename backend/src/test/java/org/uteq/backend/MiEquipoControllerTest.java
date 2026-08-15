package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.academico.estudiante.controller.MiEquipoController;
import org.uteq.backend.academico.estudiante.dto.MiEquipoDtos.*;
import org.uteq.backend.academico.estudiante.service.MiEquipoService;
import org.uteq.backend.academico.representante.dto.InformeDtos.InformeEstudianteResponse;
import org.uteq.backend.academico.representante.service.InformeService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MiEquipoControllerTest {

    @Mock private InformeService informeService;
    @Mock private MiEquipoService miEquipoService;

    @InjectMocks private MiEquipoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        autenticarComo("juan@sged.test", "ESTUDIANTE");
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username, String rol) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("miInforme devuelve 200 con el mismo DTO que ya usa el representante")
    void miInforme_devuelve_200() throws Exception {
        when(informeService.miInforme("juan@sged.test")).thenReturn(
                new InformeEstudianteResponse(1L, "Juan Perez", "SUB-12", List.of(), List.of(), new BigDecimal("90.00")));

        mockMvc.perform(get("/api/estudiante/mi-informe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Juan Perez"))
                .andExpect(jsonPath("$.porcentajeAsistencia").value(90.00));
    }

    @Test
    @DisplayName("miInforme responde 404 si la cuenta no tiene estudiante asociado")
    void miInforme_sin_estudiante_da_404() throws Exception {
        when(informeService.miInforme("juan@sged.test"))
                .thenThrow(new RecursoNoEncontradoException("No hay un estudiante asociado a esta cuenta"));

        mockMvc.perform(get("/api/estudiante/mi-informe"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("miEquipo devuelve 200 con categoria, posicion, entrenador y companeros")
    void miEquipo_devuelve_200() throws Exception {
        var respuesta = new MiEquipoResponse(
                new CategoriaDetalleResponse("SUB-12", 10, 12, "Sub 12 anios"),
                new PosicionResponse("Delantero", "DC"),
                new EntrenadorAsignadoResponse("Pedro Gomez", "Tecnico"),
                List.of(new CompaneroResponse(2L, "Carlos Perez", "Defensa")));
        when(miEquipoService.miEquipo("juan@sged.test")).thenReturn(respuesta);

        mockMvc.perform(get("/api/estudiante/mi-equipo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.nombre").value("SUB-12"))
                .andExpect(jsonPath("$.posicion.nombre").value("Delantero"))
                .andExpect(jsonPath("$.entrenador.nombre").value("Pedro Gomez"))
                .andExpect(jsonPath("$.companeros", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("miEquipo responde 404 si la cuenta no tiene estudiante asociado")
    void miEquipo_sin_estudiante_da_404() throws Exception {
        when(miEquipoService.miEquipo("juan@sged.test"))
                .thenThrow(new RecursoNoEncontradoException("No hay un estudiante asociado a esta cuenta"));

        mockMvc.perform(get("/api/estudiante/mi-equipo"))
                .andExpect(status().isNotFound());
    }
}
