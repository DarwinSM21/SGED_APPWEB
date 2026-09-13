package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.guardian.service.NotificationService;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.attendance.entity.Attendance;
import org.uteq.backend.deportivo.attendance.repository.AttendanceRepository;
import org.uteq.backend.deportivo.attendance.service.AttendanceService;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.session.entity.TrainingSession;
import org.uteq.backend.deportivo.session.repository.TrainingSessionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private StudentRepository estudianteRepository;
    @Mock private TrainingSessionRepository sesionRepository;
    @Mock private NotificationService notificacionService;

    @InjectMocks
    private AttendanceService attendanceService;

    private Student estudiante() {
        return Student.builder().id(6L).build();
    }

    private TrainingSession sesionConHoraInicio(LocalTime horaInicio) {
        return TrainingSession.builder().idSesion(1L).horaInicio(horaInicio).build();
    }

    private LocalTime enUnaHora() {
        LocalTime ahora = LocalTime.now(Zones.ECUADOR);
        return ahora.isAfter(LocalTime.of(23, 0)) ? LocalTime.of(23, 59) : ahora.plusHours(1);
    }

    @Test
    @DisplayName("marcarPorQr lanza ResourceNotFoundException si la cuenta no tiene estudiante asociado")
    void marcarPorQr_sin_estudiante_asociado_lanza_excepcion() {
        when(estudianteRepository.findByUserAccount_Username("huerfano@sged.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.markByQr("huerfano@sged.test", 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("marcarPorQr rechaza un segundo marcado en la misma sesion")
    void marcarPorQr_rechaza_doble_marcado() {
        Student e = estudiante();
        when(estudianteRepository.findByUserAccount_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(attendanceRepository.findBySession_IdAndStudent_Id(1L, 6L))
                .thenReturn(Optional.of(Attendance.builder().idAsistencia(50L).build()));

        assertThatThrownBy(() -> attendanceService.markByQr("andres@sged.test", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("marcarPorQr marca PRESENTE dentro de la tolerancia")
    void marcarPorQr_marca_presente_dentro_de_tolerancia() {
        Student e = estudiante();
        TrainingSession sesion = sesionConHoraInicio(enUnaHora());

        when(estudianteRepository.findByUserAccount_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(attendanceRepository.findBySession_IdAndStudent_Id(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(attendanceRepository.matchesCategory(6L, 1L)).thenReturn(true);
        when(attendanceRepository.save(org.mockito.ArgumentMatchers.any(Attendance.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Attendance resultado = attendanceService.markByQr("andres@sged.test", 1L);

        assertThat(resultado.getEstado()).isEqualTo(Attendance.STATUS_PRESENT);
        assertThat(resultado.getMetodo()).isEqualTo(Attendance.METHOD_QR);
        assertThat(resultado.getEstudiante()).isSameAs(e);
    }

    @Test
    @DisplayName("marcarPorQr marca TARDE fuera de la tolerancia")
    void marcarPorQr_marca_tarde_fuera_de_tolerancia() {
        Student e = estudiante();

        LocalTime ahora = LocalTime.now(Zones.ECUADOR);
        LocalTime horaInicio = ahora.isBefore(LocalTime.of(1, 0)) ? LocalTime.MIDNIGHT : ahora.minusHours(1);
        TrainingSession sesion = sesionConHoraInicio(horaInicio);

        when(estudianteRepository.findByUserAccount_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(attendanceRepository.findBySession_IdAndStudent_Id(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(attendanceRepository.matchesCategory(6L, 1L)).thenReturn(true);
        when(attendanceRepository.save(org.mockito.ArgumentMatchers.any(Attendance.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Attendance resultado = attendanceService.markByQr("andres@sged.test", 1L);

        assertThat(resultado.getEstado()).isEqualTo(Attendance.STATUS_LATE);
    }

    @Test
    @DisplayName("marcarPorQr marca PRESENTE si la sesion no tiene hora de inicio programada")
    void marcarPorQr_sin_hora_inicio_marca_presente() {
        Student e = estudiante();
        TrainingSession sesion = sesionConHoraInicio(null);

        when(estudianteRepository.findByUserAccount_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(attendanceRepository.findBySession_IdAndStudent_Id(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(attendanceRepository.matchesCategory(6L, 1L)).thenReturn(true);
        when(attendanceRepository.save(org.mockito.ArgumentMatchers.any(Attendance.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Attendance resultado = attendanceService.markByQr("andres@sged.test", 1L);

        assertThat(resultado.getEstado()).isEqualTo(Attendance.STATUS_PRESENT);
    }

    @Test
    @DisplayName("la tolerancia es configurable via asistencia.tolerancia-tarde-minutos")
    void tolerancia_es_configurable() {
        ReflectionTestUtils.setField(attendanceService, "toleranciaTardeMinutos", 1);
        Student e = estudiante();

        LocalTime ahoraTolerancia = LocalTime.now(Zones.ECUADOR);
        LocalTime horaInicioTolerancia = ahoraTolerancia.isBefore(LocalTime.of(0, 5))
                ? LocalTime.MIDNIGHT : ahoraTolerancia.minusMinutes(5);
        TrainingSession sesion = sesionConHoraInicio(horaInicioTolerancia);

        when(estudianteRepository.findByUserAccount_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(attendanceRepository.findBySession_IdAndStudent_Id(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(attendanceRepository.matchesCategory(6L, 1L)).thenReturn(true);
        when(attendanceRepository.save(org.mockito.ArgumentMatchers.any(Attendance.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Attendance resultado = attendanceService.markByQr("andres@sged.test", 1L);

        assertThat(resultado.getEstado()).isEqualTo(Attendance.STATUS_LATE);
    }

    @Test
    @DisplayName("marcarPorQr rechaza una sesion que no es de la categoria del estudiante")
    void marcarPorQr_rechaza_categoria_no_coincidente() {
        Student e = estudiante();
        TrainingSession sesion = sesionConHoraInicio(enUnaHora());

        when(estudianteRepository.findByUserAccount_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(attendanceRepository.findBySession_IdAndStudent_Id(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(attendanceRepository.matchesCategory(6L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> attendanceService.markByQr("andres@sged.test", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categoría");
    }

    @Test
    @DisplayName("marcarPorQr rechaza si el procedimiento no puede determinar la categoria (null)")
    void marcarPorQr_rechaza_categoria_indeterminada() {
        Student e = estudiante();
        TrainingSession sesion = sesionConHoraInicio(enUnaHora());

        when(estudianteRepository.findByUserAccount_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(attendanceRepository.findBySession_IdAndStudent_Id(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(attendanceRepository.matchesCategory(6L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> attendanceService.markByQr("andres@sged.test", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("misAsistencias lanza ResourceNotFoundException si la cuenta no tiene estudiante asociado")
    void misAsistencias_sin_estudiante_asociado_lanza_excepcion() {
        when(estudianteRepository.findByUserAccount_Username("huerfano@sged.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.myAttendances("huerfano@sged.test"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("misAsistencias devuelve el historial propio ordenado y el porcentaje de los ultimos 30 dias")
    void misAsistencias_devuelve_historial_y_porcentaje() {
        Student e = estudiante();
        TrainingSession sesion = TrainingSession.builder()
                .idSesion(1L).fecha(LocalDate.of(2026, 8, 10))
                .categoria(Category.builder().categoryId(1L).nombre("SUB-12").build())
                .build();
        Attendance asistencia = Attendance.builder()
                .idAsistencia(50L).sesion(sesion).estudiante(e)
                .horaEntrada(LocalTime.of(16, 5)).estado(Attendance.STATUS_PRESENT)
                .build();

        when(estudianteRepository.findByUserAccount_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(attendanceRepository.findByStudent_IdOrderBySession_DateDesc(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(asistencia)));
        when(attendanceRepository.calculateAttendancePercentage(anyLong(), any(), any()))
                .thenReturn(new BigDecimal("80.00"));

        var respuesta = attendanceService.myAttendances("andres@sged.test");

        assertThat(respuesta.attendances()).hasSize(1);
        assertThat(respuesta.attendances().get(0).category()).isEqualTo("SUB-12");
        assertThat(respuesta.attendances().get(0).status()).isEqualTo(Attendance.STATUS_PRESENT);
        assertThat(respuesta.percentageLast30Days()).isEqualByComparingTo("80.00");
    }

    private Object[] fila(LocalDate fecha, long presentes, long esperados) {
        return new Object[]{fecha, presentes, esperados};
    }

    @Test
    void mapaSumaLasCategoriasQueEntrenaronElMismoDia() {
        LocalDate dia = LocalDate.now(Zones.ECUADOR).minusDays(3);
        when(sesionRepository.attendanceSummaryByDay(any(), any()))
                .thenReturn(List.of(fila(dia, 8, 10), fila(dia, 6, 10)));

        var mapa = attendanceService.attendanceMap(30);

        assertThat(mapa.days()).hasSize(1);
        assertThat(mapa.days().get(0).present()).isEqualTo(14);
        assertThat(mapa.days().get(0).expected()).isEqualTo(20);
        assertThat(mapa.days().get(0).percentage()).isEqualByComparingTo("70.00");
    }

    @Test
    void mapaNoLlegaHastaHoyPorqueLaSesionDeHoyPuedeNoHaberOcurrido() {
        when(sesionRepository.attendanceSummaryByDay(any(), any())).thenReturn(List.of());

        attendanceService.attendanceMap(30);

        ArgumentCaptor<LocalDate> desde = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> hasta = ArgumentCaptor.forClass(LocalDate.class);
        verify(sesionRepository).attendanceSummaryByDay(desde.capture(), hasta.capture());

        LocalDate ayer = LocalDate.now(Zones.ECUADOR).minusDays(1);
        assertThat(hasta.getValue()).isEqualTo(ayer);
        assertThat(desde.getValue()).isEqualTo(ayer.minusDays(29));
    }

    @Test
    void mapaPromediaSoloSobreLosDiasQueTuvieronEntrenamiento() {
        LocalDate base = LocalDate.now(Zones.ECUADOR).minusDays(5);
        when(sesionRepository.attendanceSummaryByDay(any(), any())).thenReturn(List.of(
                fila(base, 10, 10),
                fila(base.plusDays(1), 6, 10)
        ));

        var mapa = attendanceService.attendanceMap(30);

        assertThat(mapa.average()).isEqualByComparingTo("80.00");
        assertThat(mapa.bestDay().percentage()).isEqualByComparingTo("100.00");
        assertThat(mapa.worstDay().percentage()).isEqualByComparingTo("60.00");
    }

    @Test
    void mapaSinDatosNoRompeNiInventaExtremos() {
        when(sesionRepository.attendanceSummaryByDay(any(), any())).thenReturn(List.of());

        var mapa = attendanceService.attendanceMap(30);

        assertThat(mapa.days()).isEmpty();
        assertThat(mapa.average()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(mapa.bestDay()).isNull();
        assertThat(mapa.worstDay()).isNull();
    }
}
