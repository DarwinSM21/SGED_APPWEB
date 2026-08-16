package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.inventario.movimiento.controller.MovimientoStockController;
import org.uteq.backend.inventario.movimiento.dto.MovimientoDtos.MovimientoRequest;
import org.uteq.backend.inventario.movimiento.dto.MovimientoDtos.MovimientoResponse;
import org.uteq.backend.inventario.movimiento.entity.MovimientoStock.TipoMovimiento;
import org.uteq.backend.inventario.movimiento.service.MovimientoStockService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * R-07 (informe de evaluacion de calidad, D-09): MovimientoStockController
 * estaba en la lista de controladores con 0% de cobertura.
 */
@ExtendWith(MockitoExtension.class)
class MovimientoStockControllerTest {

    private MockMvc mockMvc;

    @Mock private MovimientoStockService movimientoStockService;

    @InjectMocks private MovimientoStockController movimientoStockController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(movimientoStockController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private MovimientoResponse respuesta() {
        return new MovimientoResponse(1L, 1L, "Balón N5", TipoMovimiento.ENTRADA, 10,
                "Compra inicial", "recepcion@sged.test", Instant.now());
    }

    @Test
    @DisplayName("GET /api/inventario/movimientos - lista paginada")
    void listar_devuelve_200() throws Exception {
        when(movimientoStockService.listarPaginado(any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/inventario/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].articulo").value("Balón N5"));
    }

    @Test
    @DisplayName("GET /api/inventario/movimientos/articulo/{id} - filtra por articulo")
    void listarPorArticulo_devuelve_200() throws Exception {
        when(movimientoStockService.listarPorArticulo(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(respuesta()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/inventario/movimientos/articulo/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/inventario/movimientos - usa el username autenticado, no uno del body")
    void registrar_usa_el_username_autenticado() throws Exception {
        autenticarComo("recepcion@sged.test");
        when(movimientoStockService.registrar(any(MovimientoRequest.class), eq("recepcion@sged.test")))
                .thenReturn(respuesta());

        mockMvc.perform(post("/api/inventario/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idArticulo\":1,\"tipoMovimiento\":\"ENTRADA\",\"cantidad\":10,\"motivo\":\"Compra inicial\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMovimiento").value("ENTRADA"));

        verify(movimientoStockService).registrar(any(MovimientoRequest.class), eq("recepcion@sged.test"));
    }

    @Test
    @DisplayName("POST /api/inventario/movimientos - cantidad menor a 1 da 422")
    void registrar_con_cantidad_invalida_da_422() throws Exception {
        autenticarComo("recepcion@sged.test");

        mockMvc.perform(post("/api/inventario/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idArticulo\":1,\"tipoMovimiento\":\"ENTRADA\",\"cantidad\":0}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
