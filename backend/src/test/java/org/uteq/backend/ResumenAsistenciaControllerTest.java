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
import org.uteq.backend.deportivo.asistencia.controller.ResumenAsistenciaController;
import org.uteq.backend.deportivo.asistencia.dto.AsistenciaDtos.MapaAsistenciaResponse;
import org.uteq.backend.deportivo.asistencia.service.AsistenciaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ResumenAsistenciaControllerTest {
    private MockMvc mockMvc;

    @Mock private AsistenciaService asistenciaService;

    @InjectMocks private ResumenAsistenciaController resumenAsistenciaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(resumenAsistenciaController).build();
    }

    private MapaAsistenciaResponse mapa() {
        return new MapaAsistenciaResponse(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 4),
                List.of(), BigDecimal.valueOf(82.5), null, null);
    }

    @Test
    @DisplayName("GET /api/asistencias/mapa - usa la ventana por defecto de 35 días")
    void mapa_sin_parametro_usa_default_35() throws Exception {
        when(asistenciaService.mapaDeAsistencia(35)).thenReturn(mapa());

        mockMvc.perform(get("/api/asistencias/mapa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedio").value(82.5));

        verify(asistenciaService).mapaDeAsistencia(35);
    }

    @Test
    @DisplayName("GET /api/asistencias/mapa?dias=60 - propaga la ventana pedida al servicio")
    void mapa_con_parametro_dias() throws Exception {
        when(asistenciaService.mapaDeAsistencia(60)).thenReturn(mapa());

        mockMvc.perform(get("/api/asistencias/mapa").param("dias", "60"))
                .andExpect(status().isOk());

        verify(asistenciaService).mapaDeAsistencia(60);
    }
}
