package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.asistencia.service.AsistenciaService;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * El umbral PRESENTE/TARDE se prueba con horaInicio a +-1h de LocalTime.now()
 * en vez de valores fijos: el servicio llama LocalTime.now() el mismo, y este
 * proyecto no usa un Clock inyectable en ningun lado (introducir uno solo
 * para esto seria una desviacion de convencion para un beneficio menor). Un
 * margen de una hora hace el resultado determinista frente a los pocos
 * milisegundos que tarda en ejecutarse la prueba.
 */
@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private SesionEntrenamientoRepository sesionRepository;

    @InjectMocks
    private AsistenciaService asistenciaService;

    private Estudiante estudiante() {
        return Estudiante.builder().idEstudiante(6L).build();
    }

    private SesionEntrenamiento sesionConHoraInicio(LocalTime horaInicio) {
        return SesionEntrenamiento.builder().idSesion(1L).horaInicio(horaInicio).build();
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
        SesionEntrenamiento sesion = sesionConHoraInicio(LocalTime.now().plusHours(1));

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
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
        SesionEntrenamiento sesion = sesionConHoraInicio(LocalTime.now().minusHours(1));

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
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
        // 5 minutos despues del inicio, con solo 1 minuto de tolerancia -> TARDE.
        SesionEntrenamiento sesion = sesionConHoraInicio(LocalTime.now().minusMinutes(5));

        when(estudianteRepository.findByUsuario_Username("andres@sged.test")).thenReturn(Optional.of(e));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(1L, 6L)).thenReturn(Optional.empty());
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(sesion));
        when(asistenciaRepository.save(org.mockito.ArgumentMatchers.any(Asistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Asistencia resultado = asistenciaService.marcarPorQr("andres@sged.test", 1L);

        assertThat(resultado.getEstado()).isEqualTo(Asistencia.ESTADO_TARDE);
    }
}
