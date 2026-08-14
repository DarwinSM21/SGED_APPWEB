package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.pago.repository.PagoRepository;
import org.uteq.backend.academico.pago.service.PagoService;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private PagoService service;

    @Test
    @DisplayName("ingresosDelMes suma por fecha real de pago del mes calendario vigente en Ecuador")
    void ingresosDelMes_suma_el_mes_calendario_vigente() {
        YearMonth hoy = YearMonth.now(Zonas.ECUADOR);
        LocalDate inicio = hoy.atDay(1);
        LocalDate fin = hoy.atEndOfMonth();

        when(pagoRepository.sumarMontoEntreFechas(inicio, fin)).thenReturn(new BigDecimal("150.00"));
        when(pagoRepository.countByFechaPagoBetween(inicio, fin)).thenReturn(3L);

        var response = service.ingresosDelMes();

        assertThat(response.anio()).isEqualTo(hoy.getYear());
        assertThat(response.mes()).isEqualTo(hoy.getMonthValue());
        assertThat(response.total()).isEqualByComparingTo("150.00");
        assertThat(response.cantidadPagos()).isEqualTo(3L);
    }

    @Test
    @DisplayName("ingresosDelMes devuelve cero, no null, cuando no hay pagos este mes")
    void ingresosDelMes_sin_pagos_devuelve_cero() {
        when(pagoRepository.sumarMontoEntreFechas(any(), any())).thenReturn(BigDecimal.ZERO);
        when(pagoRepository.countByFechaPagoBetween(any(), any())).thenReturn(0L);

        var response = service.ingresosDelMes();

        assertThat(response.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.cantidadPagos()).isZero();
    }
}
