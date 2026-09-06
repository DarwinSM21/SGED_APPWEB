package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.payment.entity.Payment;
import org.uteq.backend.academico.payment.entity.Payment.TipoPago;
import org.uteq.backend.academico.payment.repository.PaymentRepository;
import org.uteq.backend.academico.payment.service.PaymentService;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock private PaymentRepository pagoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UserAccountRepository usuarioRepository;

    @InjectMocks private PaymentService service;

    private static final Long ID_EST = 1L;
    private static final String USERNAME = "recepcion@sged.test";

    private Estudiante estudiante() {
        return Estudiante.builder().idEstudiante(ID_EST)
                .persona(Person.builder().nombre("Juan").apellido("Perez").build())
                .build();
    }

    private UserAccount registrador() {
        return UserAccount.builder().idUsuario(9L).username(USERNAME)
                .persona(Person.builder().nombre("Ana").apellido("Admin").build())
                .build();
    }

    private void existenEstudianteYUsuario() {
        when(estudianteRepository.findById(ID_EST)).thenReturn(Optional.of(estudiante()));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(registrador()));
    }

    @Test
    @DisplayName("ingresosDelMes suma por fecha real de pago del mes calendario vigente en Ecuador")
    void ingresosDelMes_suma_el_mes_calendario_vigente() {
        YearMonth hoy = YearMonth.now(Zones.ECUADOR);
        LocalDate inicio = hoy.atDay(1);
        LocalDate fin = hoy.atEndOfMonth();

        when(pagoRepository.sumAmountBetweenDates(inicio, fin)).thenReturn(new BigDecimal("150.00"));
        when(pagoRepository.countByFechaPagoBetweenAndAnuladoEnIsNull(inicio, fin)).thenReturn(3L);

        var response = service.currentMonthIncome();

        assertThat(response.anio()).isEqualTo(hoy.getYear());
        assertThat(response.mes()).isEqualTo(hoy.getMonthValue());
        assertThat(response.total()).isEqualByComparingTo("150.00");
        assertThat(response.cantidadPagos()).isEqualTo(3L);
    }

    @Test
    @DisplayName("ingresosDelMes devuelve cero, no null, cuando no hay pagos este mes")
    void ingresosDelMes_sin_pagos_devuelve_cero() {
        when(pagoRepository.sumAmountBetweenDates(any(), any())).thenReturn(BigDecimal.ZERO);
        when(pagoRepository.countByFechaPagoBetweenAndAnuladoEnIsNull(any(), any())).thenReturn(0L);

        var response = service.currentMonthIncome();

        assertThat(response.total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.cantidadPagos()).isZero();
    }

    @Test
    @DisplayName("registrarMembresia guarda un pago por cada mes, sin duplicados y en orden")
    void registrarMembresia_guarda_un_pago_por_mes_distinto_y_ordenado() {
        existenEstudianteYUsuario();
        when(pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                any(), any(), any(), any())).thenReturn(false);
        when(pagoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        var meses = List.of(3, 1, 2, 1);
        var pagos = service.registerMembership(ID_EST, 2026, meses, new BigDecimal("30.00"), null, USERNAME);

        assertThat(pagos).extracting(p -> p.getMes().intValue()).containsExactly(1, 2, 3);
        assertThat(pagos).allSatisfy(p -> {
            assertThat(p.getTipo()).isEqualTo(TipoPago.MEMBRESIA);
            assertThat(p.getAnio()).isEqualTo((short) 2026);
            assertThat(p.getFechaPago()).isEqualTo(LocalDate.now(Zones.ECUADOR));
        });
    }

    @Test
    @DisplayName("registrarMembresia usa la fecha dada en vez de la de hoy si se especifica")
    void registrarMembresia_usa_la_fecha_dada() {
        existenEstudianteYUsuario();
        when(pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                any(), any(), any(), any())).thenReturn(false);
        when(pagoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        var fechaFija = LocalDate.of(2026, 1, 15);
        var pagos = service.registerMembership(ID_EST, 2026, List.of(1), new BigDecimal("30.00"), fechaFija, USERNAME);

        assertThat(pagos).singleElement().satisfies(p -> assertThat(p.getFechaPago()).isEqualTo(fechaFija));
    }

    @Test
    @DisplayName("registrarMembresia rechaza si algun mes solicitado ya esta cubierto")
    void registrarMembresia_rechaza_mes_ya_cubierto() {
        existenEstudianteYUsuario();
        when(pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                ID_EST, TipoPago.MEMBRESIA, (short) 2026, (short) 1)).thenReturn(false);
        when(pagoRepository.existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
                ID_EST, TipoPago.MEMBRESIA, (short) 2026, (short) 2)).thenReturn(true);

        var e = assertThrows(IllegalArgumentException.class, () ->
                service.registerMembership(ID_EST, 2026, List.of(1, 2), new BigDecimal("30.00"), null, USERNAME));

        assertThat(e.getMessage()).contains("2/2026").contains("ya está cubierto");
        verify(pagoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registrarMembresia responde 404 si el estudiante no existe")
    void registrarMembresia_estudiante_inexistente() {
        when(estudianteRepository.findById(ID_EST)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.registerMembership(ID_EST, 2026, List.of(1), new BigDecimal("30.00"), null, USERNAME));

        verify(pagoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registrarMembresia falla si el usuario autenticado no tiene fila propia")
    void registrarMembresia_usuario_registrador_inexistente() {
        when(estudianteRepository.findById(ID_EST)).thenReturn(Optional.of(estudiante()));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                service.registerMembership(ID_EST, 2026, List.of(1), new BigDecimal("30.00"), null, USERNAME));

        verify(pagoRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("registrarDiario guarda un pago DIARIO con la fecha dada")
    void registrarDiario_usa_la_fecha_dada() {
        existenEstudianteYUsuario();
        when(pagoRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        var fechaFija = LocalDate.of(2026, 3, 10);
        var pago = service.registerDaily(ID_EST, new BigDecimal("5.00"), fechaFija, USERNAME);

        assertThat(pago.getTipo()).isEqualTo(TipoPago.DIARIO);
        assertThat(pago.getFechaPago()).isEqualTo(fechaFija);
        assertThat(pago.getAnio()).isNull();
        assertThat(pago.getMes()).isNull();
    }

    @Test
    @DisplayName("registrarDiario usa la fecha de hoy en Ecuador si no se especifica")
    void registrarDiario_usa_fecha_de_hoy_si_no_se_da() {
        existenEstudianteYUsuario();
        when(pagoRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        var pago = service.registerDaily(ID_EST, new BigDecimal("5.00"), null, USERNAME);

        assertThat(pago.getFechaPago()).isEqualTo(LocalDate.now(Zones.ECUADOR));
    }

    @Test
    @DisplayName("historialDe responde 404 si el estudiante no existe")
    void historialDe_estudiante_inexistente() {
        when(estudianteRepository.existsById(ID_EST)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.historyFor(ID_EST));

        verify(pagoRepository, never()).findByEstudiante_IdEstudianteOrderByFechaPagoDesc(any());
    }

    @Test
    @DisplayName("historialDe devuelve los pagos del estudiante ordenados por fecha descendente")
    void historialDe_devuelve_pagos_del_estudiante() {
        when(estudianteRepository.existsById(ID_EST)).thenReturn(true);
        var esperado = List.of(Payment.builder().idPago(1L).tipo(TipoPago.DIARIO).build());
        when(pagoRepository.findByEstudiante_IdEstudianteOrderByFechaPagoDesc(ID_EST)).thenReturn(esperado);

        var pagos = service.historyFor(ID_EST);

        assertThat(pagos).isEqualTo(esperado);
    }

    @Test
    @DisplayName("anular deja constancia de quien, cuando y por que")
    void anular_registra_la_trazabilidad() {
        Payment pago = Payment.builder()
                .idPago(9L).tipo(Payment.TipoPago.DIARIO)
                .monto(new BigDecimal("250.00")).fechaPago(LocalDate.now())
                .build();
        when(pagoRepository.findById(9L)).thenReturn(Optional.of(pago));
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(registrador()));
        when(pagoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Payment resultado = service.cancel(9L, "Monto mal digitado", USERNAME);

        assertThat(resultado.getAnuladoEn()).isNotNull();
        assertThat(resultado.getAnuladoPor()).isNotNull();
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Monto mal digitado");

        assertThat(resultado.getMonto()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("anular dos veces se rechaza en vez de pasar en silencio")
    void anular_dos_veces_falla() {
        Payment yaAnulado = Payment.builder()
                .idPago(9L).tipo(Payment.TipoPago.DIARIO)
                .monto(new BigDecimal("25.00")).fechaPago(LocalDate.now())
                .anuladoEn(java.time.OffsetDateTime.now())
                .motivoAnulacion("ya estaba")
                .build();
        when(pagoRepository.findById(9L)).thenReturn(Optional.of(yaAnulado));

        assertThrows(IllegalArgumentException.class,
                () -> service.cancel(9L, "otra vez", USERNAME));

        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("anular un pago que no existe da recurso no encontrado")
    void anular_inexistente() {
        when(pagoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.cancel(404L, "motivo", USERNAME));
    }
}
