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
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.service.NotificacionService;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.asistencia.service.AsistenciaService;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

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
class AsistenciaServiceTest {
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private SesionEntrenamientoRepository sesionRepository;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private AsistenciaService asistenciaService;

    private Estudiante estudiante() {
        return Estudiante.builder().idEstudiante(6L).build();
    }

    private SesionEntrenamiento sesionConHoraInicio(LocalTime horaInicio) {
        return SesionEntrenamiento.builder().idSesion(1L).horaInicio(horaInicio).build();
    }

    private LocalTime enUnaHora() {
        LocalTime ahora = LocalTime.now(Zonas.ECUADOR);
        return ahora.isAfter(LocalTime.of(23, 0)) ? LocalTime.of(23, 59) : ahora.plusHours(1);
    }

    @Test
    @DisplayName("marcarPorQr lanza RecursoNoEncontradoException si la cuenta no tiene estudiante asociado")
    void marcarPorQr_sin_estudiante_asociado_lanza_excepcion() {
        when(estudianteRepository.findByUsuario_Username("huerfano@sged.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asistenciaService.marcarPorQr("huerfano@sged.test", 1L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("marcarPorQr rechaza un segundo marcado en la misma sesion")
    void marcarPorQr_rechaza_doble_marcado() {
        Estudiante e = estudiante();
        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L))
                .thenReturn(Optional.of(Asistencia.builder().idAsistencia(50L).build()));

        assertThatThrownBy(() -> asistenciaService.marcarPorQr("andres@sged.test", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("marcarPorQr marca PRESENTE dentro de la tolerancia")
    void marcarPorQr_marca_presente_dentro_de_tolerancia() {
        Estudiante e = estudiante();
        SesionEntrenamiento sesion = sesionConHoraInicio(enUnaHora());

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaRepository.validarCategoriaCoincide(6L, 1L)).thenReturn(true);
        when(asistenciaRepository.save(org.mockito.ArgumentMatchers.any(Asistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Asistencia resultado = asistenciaService.marcarPorQr("andres@sged.test", 1L);

        assertThat(resultado.getEstado()).isEqualTo(Asistencia.ESTADO_PRESENTE);
        assertThat(resultado.getMetodo()).isEqualTo(Asistencia.METODO_QR);
        assertThat(resultado.getEstudiante()).isSameAs(e);
    }

    @Test
    @DisplayName("marcarPorQr marca TARDE fuera de la tolerancia")
    void marcarPorQr_marca_tarde_fuera_de_tolerancia() {
        Estudiante e = estudiante();

        LocalTime ahora = LocalTime.now(Zonas.ECUADOR);
        LocalTime horaInicio = ahora.isBefore(LocalTime.of(1, 0)) ? LocalTime.MIDNIGHT : ahora.minusHours(1);
        SesionEntrenamiento sesion = sesionConHoraInicio(horaInicio);

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaRepository.validarCategoriaCoincide(6L, 1L)).thenReturn(true);
        when(asistenciaRepository.save(org.mockito.ArgumentMatchers.any(Asistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Asistencia resultado = asistenciaService.marcarPorQr("andres@sged.test", 1L);

        assertThat(resultado.getEstado()).isEqualTo(Asistencia.ESTADO_TARDE);
    }

    @Test
    @DisplayName("marcarPorQr marca PRESENTE si la sesion no tiene hora de inicio programada")
    void marcarPorQr_sin_hora_inicio_marca_presente() {
        Estudiante e = estudiante();
        SesionEntrenamiento sesion = sesionConHoraInicio(null);

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaRepository.validarCategoriaCoincide(6L, 1L)).thenReturn(true);
        when(asistenciaRepository.save(org.mockito.ArgumentMatchers.any(Asistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Asistencia resultado = asistenciaService.marcarPorQr("andres@sged.test", 1L);

        assertThat(resultado.getEstado()).isEqualTo(Asistencia.ESTADO_PRESENTE);
    }

    @Test
    @DisplayName("la tolerancia es configurable via asistencia.tolerancia-tarde-minutos")
    void tolerancia_es_configurable() {
        ReflectionTestUtils.setField(asistenciaService, "toleranciaTardeMinutos", 1);
        Estudiante e = estudiante();

        LocalTime ahoraTolerancia = LocalTime.now(Zonas.ECUADOR);
        LocalTime horaInicioTolerancia = ahoraTolerancia.isBefore(LocalTime.of(0, 5))
                ? LocalTime.MIDNIGHT : ahoraTolerancia.minusMinutes(5);
        SesionEntrenamiento sesion = sesionConHoraInicio(horaInicioTolerancia);

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaRepository.validarCategoriaCoincide(6L, 1L)).thenReturn(true);
        when(asistenciaRepository.save(org.mockito.ArgumentMatchers.any(Asistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Asistencia resultado = asistenciaService.marcarPorQr("andres@sged.test", 1L);

        assertThat(resultado.getEstado()).isEqualTo(Asistencia.ESTADO_TARDE);
    }

    @Test
    @DisplayName("marcarPorQr rechaza una sesion que no es de la categoria del estudiante")
    void marcarPorQr_rechaza_categoria_no_coincidente() {
        Estudiante e = estudiante();
        SesionEntrenamiento sesion = sesionConHoraInicio(enUnaHora());

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaRepository.validarCategoriaCoincide(6L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> asistenciaService.marcarPorQr("andres@sged.test", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categoría");
    }

    @Test
    @DisplayName("marcarPorQr rechaza si el procedimiento no puede determinar la categoria (null)")
    void marcarPorQr_rechaza_categoria_indeterminada() {
        Estudiante e = estudiante();
        SesionEntrenamiento sesion = sesionConHoraInicio(enUnaHora());

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaRepository.validarCategoriaCoincide(6L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> asistenciaService.marcarPorQr("andres@sged.test", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("misAsistencias lanza RecursoNoEncontradoException si la cuenta no tiene estudiante asociado")
    void misAsistencias_sin_estudiante_asociado_lanza_excepcion() {
        when(estudianteRepository.findByUsuario_Username("huerfano@sged.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asistenciaService.misAsistencias("huerfano@sged.test"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("misAsistencias devuelve el historial propio ordenado y el porcentaje de los ultimos 30 dias")
    void misAsistencias_devuelve_historial_y_porcentaje() {
        Estudiante e = estudiante();
        SesionEntrenamiento sesion = SesionEntrenamiento.builder()
                .idSesion(1L).fecha(LocalDate.of(2026, 8, 10))
                .categoria(Categoria.builder().idCategoria(1L).nombre("SUB-12").build())
                .build();
        Asistencia asistencia = Asistencia.builder()
                .idAsistencia(50L).sesion(sesion).estudiante(e)
                .horaEntrada(LocalTime.of(16, 5)).estado(Asistencia.ESTADO_PRESENTE)
                .build();

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findByEstudiante_IdEstudianteOrderBySesion_FechaDesc(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(asistencia)));
        when(asistenciaRepository.calcularPorcentajeAsistencia(anyLong(), any(), any()))
                .thenReturn(new BigDecimal("80.00"));

        var respuesta = asistenciaService.misAsistencias("andres@sged.test");

        assertThat(respuesta.asistencias()).hasSize(1);
        assertThat(respuesta.asistencias().get(0).categoria()).isEqualTo("SUB-12");
        assertThat(respuesta.asistencias().get(0).estado()).isEqualTo(Asistencia.ESTADO_PRESENTE);
        assertThat(respuesta.porcentajeUltimos30Dias()).isEqualByComparingTo("80.00");
    }

    private Object[] fila(LocalDate fecha, long presentes, long esperados) {
        return new Object[]{fecha, presentes, esperados};
    }

    @Test
    void mapaSumaLasCategoriasQueEntrenaronElMismoDia() {
        LocalDate dia = LocalDate.now(Zonas.ECUADOR).minusDays(3);
        when(sesionRepository.resumenAsistenciaPorDia(any(), any()))
                .thenReturn(List.of(fila(dia, 8, 10), fila(dia, 6, 10)));

        var mapa = asistenciaService.mapaDeAsistencia(30);

        assertThat(mapa.dias()).hasSize(1);
        assertThat(mapa.dias().get(0).presentes()).isEqualTo(14);
        assertThat(mapa.dias().get(0).esperados()).isEqualTo(20);
        assertThat(mapa.dias().get(0).porcentaje()).isEqualByComparingTo("70.00");
    }

    @Test
    void mapaNoLlegaHastaHoyPorqueLaSesionDeHoyPuedeNoHaberOcurrido() {
        when(sesionRepository.resumenAsistenciaPorDia(any(), any())).thenReturn(List.of());

        asistenciaService.mapaDeAsistencia(30);

        ArgumentCaptor<LocalDate> desde = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> hasta = ArgumentCaptor.forClass(LocalDate.class);
        verify(sesionRepository).resumenAsistenciaPorDia(desde.capture(), hasta.capture());

        LocalDate ayer = LocalDate.now(Zonas.ECUADOR).minusDays(1);
        assertThat(hasta.getValue()).isEqualTo(ayer);
        assertThat(desde.getValue()).isEqualTo(ayer.minusDays(29));
    }

    @Test
    void mapaPromediaSoloSobreLosDiasQueTuvieronEntrenamiento() {
        LocalDate base = LocalDate.now(Zonas.ECUADOR).minusDays(5);
        when(sesionRepository.resumenAsistenciaPorDia(any(), any())).thenReturn(List.of(
                fila(base, 10, 10),
                fila(base.plusDays(1), 6, 10)
        ));

        var mapa = asistenciaService.mapaDeAsistencia(30);

        assertThat(mapa.promedio()).isEqualByComparingTo("80.00");
        assertThat(mapa.mejorDia().porcentaje()).isEqualByComparingTo("100.00");
        assertThat(mapa.peorDia().porcentaje()).isEqualByComparingTo("60.00");
    }

    @Test
    void mapaSinDatosNoRompeNiInventaExtremos() {
        when(sesionRepository.resumenAsistenciaPorDia(any(), any())).thenReturn(List.of());

        var mapa = asistenciaService.mapaDeAsistencia(30);

        assertThat(mapa.dias()).isEmpty();
        assertThat(mapa.promedio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(mapa.mejorDia()).isNull();
        assertThat(mapa.peorDia()).isNull();
    }
}
