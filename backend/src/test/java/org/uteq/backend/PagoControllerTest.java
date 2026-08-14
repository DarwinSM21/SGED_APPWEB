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
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.pago.controller.PagoController;
import org.uteq.backend.academico.pago.dto.PagoDtos.IngresosMesResponse;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;
import org.uteq.backend.academico.pago.service.PagoService;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El principal autenticado (no un campo del body) es quien queda como
 * registradoPor de cada pago: por eso los tests de alta fijan el usuario
 * autenticado y verifican que ese mismo username llega al servicio.
 */
@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    @Mock private PagoService pagoService;

    @InjectMocks private PagoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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

    private Pago pago(Long id, TipoPago tipo, Integer anio, Integer mes) {
        var estudiante = Estudiante.builder().idEstudiante(1L)
                .persona(Persona.builder().nombre("Juan").apellido("Perez").build())
                .build();
        var registrador = Usuario.builder().idUsuario(9L)
                .persona(Persona.builder().nombre("Ana").apellido("Admin").build())
                .build();
        return Pago.builder()
                .idPago(id)
                .estudiante(estudiante)
                .tipo(tipo)
                .anio(anio == null ? null : anio.shortValue())
                .mes(mes == null ? null : mes.shortValue())
                .monto(new BigDecimal("30.00"))
                .fechaPago(LocalDate.of(2026, 8, 14))
                .registradoPor(registrador)
                .build();
    }

    // --- POST /api/pagos/membresia ---

    @Test
    @DisplayName("registrarMembresia devuelve 201 usando el usuario autenticado, no uno del body")
    void registrarMembresia_devuelve_201() throws Exception {
        autenticarComo("recepcion@sged.test", "RECEPCIONISTA");
        when(pagoService.registrarMembresia(eq(1L), eq(2026), eq(List.of(8)),
                eq(new BigDecimal("30.00")), isNull(), eq("recepcion@sged.test")))
                .thenReturn(List.of(pago(1L, TipoPago.MEMBRESIA, 2026, 8)));

        mockMvc.perform(post("/api/pagos/membresia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"anio\":2026,\"meses\":[8],\"monto\":30.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].estudiante").value("Juan Perez"))
                .andExpect(jsonPath("$[0].registradoPor").value("Ana Admin"))
                .andExpect(jsonPath("$[0].tipo").value("MEMBRESIA"))
                .andExpect(jsonPath("$[0].mes").value(8));
    }

    @Test
    @DisplayName("registrarMembresia propaga como 400 el mes ya cubierto")
    void registrarMembresia_mes_cubierto_da_400() throws Exception {
        autenticarComo("recepcion@sged.test", "RECEPCIONISTA");
        when(pagoService.registrarMembresia(any(), anyInt(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("El mes 8/2026 ya está cubierto para este estudiante"));

        mockMvc.perform(post("/api/pagos/membresia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"anio\":2026,\"meses\":[8],\"monto\":30.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("registrarMembresia con datos invalidos da 422 sin llegar al servicio")
    void registrarMembresia_datos_invalidos_da_422() throws Exception {
        mockMvc.perform(post("/api/pagos/membresia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":null,\"anio\":2026,\"meses\":[],\"monto\":30.00}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- POST /api/pagos/diario ---

    @Test
    @DisplayName("registrarDiario devuelve 201 con anio y mes nulos (no cubre un periodo)")
    void registrarDiario_devuelve_201() throws Exception {
        autenticarComo("recepcion@sged.test", "RECEPCIONISTA");
        when(pagoService.registrarDiario(eq(1L), eq(new BigDecimal("5.00")), isNull(), eq("recepcion@sged.test")))
                .thenReturn(pago(2L, TipoPago.DIARIO, null, null));

        mockMvc.perform(post("/api/pagos/diario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstudiante\":1,\"monto\":5.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DIARIO"))
                .andExpect(jsonPath("$.anio").isEmpty())
                .andExpect(jsonPath("$.mes").isEmpty());
    }

    // --- GET /api/pagos/estudiante/{id} ---

    @Test
    @DisplayName("historial devuelve la lista de pagos del estudiante")
    void historial_devuelve_lista() throws Exception {
        when(pagoService.historialDe(1L)).thenReturn(List.of(pago(3L, TipoPago.DIARIO, null, null)));

        mockMvc.perform(get("/api/pagos/estudiante/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("historial responde 404 si el estudiante no existe")
    void historial_estudiante_inexistente_da_404() throws Exception {
        when(pagoService.historialDe(99L))
                .thenThrow(new RecursoNoEncontradoException("Estudiante no encontrado con id: 99"));

        mockMvc.perform(get("/api/pagos/estudiante/99"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/pagos/ingresos-mes ---

    @Test
    @DisplayName("ingresosDelMes devuelve el total y la cantidad de pagos del mes vigente")
    void ingresosDelMes_devuelve_200() throws Exception {
        when(pagoService.ingresosDelMes()).thenReturn(new IngresosMesResponse(2026, 8, new BigDecimal("150.00"), 3L));

        mockMvc.perform(get("/api/pagos/ingresos-mes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(150.00))
                .andExpect(jsonPath("$.cantidadPagos").value(3));
    }
}
