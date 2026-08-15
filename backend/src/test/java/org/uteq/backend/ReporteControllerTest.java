package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.reportes.controller.ReporteController;
import org.uteq.backend.reportes.service.ReporteService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock private ReporteService reporteService;
    @InjectMocks private ReporteController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/reportes/estudiantes-fichas devuelve un PDF descargable")
    void estudiantesFichasDevuelvePdf() throws Exception {
        byte[] pdf = "%PDF-1.4 contenido".getBytes();
        when(reporteService.estudiantesFichas(isNull(), isNull())).thenReturn(pdf);

        mockMvc.perform(get("/api/reportes/estudiantes-fichas"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("fichas-estudiantes.pdf")))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @DisplayName("GET /api/reportes/pagos reenvia los filtros de query al servicio")
    void pagosReenviaFiltros() throws Exception {
        when(reporteService.pagos(eq(7L), any(), any())).thenReturn("%PDF".getBytes());

        mockMvc.perform(get("/api/reportes/pagos")
                        .param("estudianteId", "7")
                        .param("fechaDesde", "2026-08-01")
                        .param("fechaHasta", "2026-08-14"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sin resultados para los filtros, el servicio 404 se propaga tal cual")
    void sinResultadosDa404() throws Exception {
        when(reporteService.lesiones(any(), any(), any(), any()))
                .thenThrow(new RecursoNoEncontradoException("No hay datos para los filtros seleccionados"));

        mockMvc.perform(get("/api/reportes/lesiones"))
                .andExpect(status().isNotFound());
    }
}
