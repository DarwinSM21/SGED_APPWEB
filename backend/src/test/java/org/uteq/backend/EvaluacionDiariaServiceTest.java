package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos.*;
import org.uteq.backend.deportivo.evaluacion.entity.*;
import org.uteq.backend.deportivo.evaluacion.repository.*;
import org.uteq.backend.deportivo.evaluacion.service.EvaluacionDiariaService;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Se prueban las reglas de negocio del modulo, no el mapeo HTTP: son las que
 * pueden corromper datos si fallan.
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionDiariaServiceTest {

    @Mock private EvaluacionDiariaRepository evaluacionRepository;
    @Mock private EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    @Mock private CriterioEvaluacionRepository criterioRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private SesionEntrenamientoRepository sesionRepository;
    @Mock private LesionRepository lesionRepository;
    @Mock private PosicionRepository posicionRepository;

    @InjectMocks private EvaluacionDiariaService servicio;

    private static final Long ID_SESION = 10L;
    private static final Long ID_ESTUDIANTE = 5L;

    private EvaluacionDiaria evaluacionBorrador() {
        return EvaluacionDiaria.builder()
                .idEvaluacion(99L)
                .estado(EvaluacionDiaria.BORRADOR)
                .build();
    }

    private Estudiante estudiante() {
        return Estudiante.builder()
                .idEstudiante(ID_ESTUDIANTE)
                .categoria(Categoria.builder().idCategoria(3L).nombre("SUB-12").build())
                .build();
    }

    private Asistencia asistenciaCon(String estado) {
        return Asistencia.builder()
                .estado(estado)
                .estudiante(estudiante())
                .build();
    }

    private GuardarJugadorRequest peticion() {
        return new GuardarJugadorRequest(ID_ESTUDIANTE, null,
                List.of(new PuntajeCriterioRequest(1L, new BigDecimal("7.5"))));
    }

    @Test
    @DisplayName("No se puede calificar a quien figura AUSENTE")
    void ausenteNoSePuedeCalificar() {
        when(evaluacionRepository.findBySesionIdSesion(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.of(asistenciaCon("AUSENTE")));

        var e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarJugador(ID_SESION, peticion()));

        assertTrue(e.getMessage().contains("AUSENTE"));
        verify(evaluacionEstudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sin registro de asistencia tampoco se guarda nada")
    void sinAsistenciaNoSeGuarda() {
        when(evaluacionRepository.findBySesionIdSesion(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarJugador(ID_SESION, peticion()));

        verify(evaluacionEstudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quien llego TARDE si puede ser calificado")
    void tardeSiSePuedeCalificar() {
        when(evaluacionRepository.findBySesionIdSesion(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.of(asistenciaCon(Asistencia.ESTADO_TARDE)));
        when(evaluacionEstudianteRepository.findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(criterioRepository.findByActivoTrueOrderByIdCriterioAsc()).thenReturn(
                List.of(CriterioEvaluacion.builder()
                        .idCriterio(1L).nombre("Tecnica").puntajeMaximo((short) 10).build()));
        when(lesionRepository.buscarActivaPorEstudiante(ID_ESTUDIANTE)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> servicio.guardarJugador(ID_SESION, peticion()));

        verify(evaluacionEstudianteRepository).save(any(EvaluacionEstudiante.class));
    }

    @Test
    @DisplayName("La categoria que se guarda es la del estudiante en ese momento")
    void seCongelaLaCategoriaDelDia() {
        when(evaluacionRepository.findBySesionIdSesion(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.of(asistenciaCon(Asistencia.ESTADO_PRESENTE)));
        when(evaluacionEstudianteRepository.findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(criterioRepository.findByActivoTrueOrderByIdCriterioAsc()).thenReturn(
                List.of(CriterioEvaluacion.builder()
                        .idCriterio(1L).nombre("Tecnica").puntajeMaximo((short) 10).build()));
        when(lesionRepository.buscarActivaPorEstudiante(ID_ESTUDIANTE)).thenReturn(Optional.empty());

        servicio.guardarJugador(ID_SESION, peticion());

        var captor = org.mockito.ArgumentCaptor.forClass(EvaluacionEstudiante.class);
        verify(evaluacionEstudianteRepository).save(captor.capture());
        assertEquals("SUB-12", captor.getValue().getCategoriaDia().getNombre());
    }

    @Test
    @DisplayName("Un puntaje por encima del maximo del criterio se rechaza")
    void puntajeSobreElMaximoSeRechaza() {
        when(evaluacionRepository.findBySesionIdSesion(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(asistenciaRepository.findBySesionIdSesionAndEstudianteIdEstudiante(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.of(asistenciaCon(Asistencia.ESTADO_PRESENTE)));
        when(evaluacionEstudianteRepository.findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(criterioRepository.findByActivoTrueOrderByIdCriterioAsc()).thenReturn(
                List.of(CriterioEvaluacion.builder()
                        .idCriterio(1L).nombre("Tecnica").puntajeMaximo((short) 10).build()));
        when(lesionRepository.buscarActivaPorEstudiante(ID_ESTUDIANTE)).thenReturn(Optional.empty());

        var excesivo = new GuardarJugadorRequest(ID_ESTUDIANTE, null,
                List.of(new PuntajeCriterioRequest(1L, new BigDecimal("11.0"))));

        var e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarJugador(ID_SESION, excesivo));
        assertTrue(e.getMessage().contains("maximo"));
    }

    @Test
    @DisplayName("Una evaluacion finalizada ya no admite cambios")
    void finalizadaNoAdmiteCambios() {
        var finalizada = EvaluacionDiaria.builder()
                .idEvaluacion(99L).estado(EvaluacionDiaria.FINALIZADA).build();
        when(evaluacionRepository.findBySesionIdSesion(ID_SESION)).thenReturn(Optional.of(finalizada));

        var e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarJugador(ID_SESION, peticion()));

        assertTrue(e.getMessage().contains("finalizada"));
        verify(asistenciaRepository, never())
                .findBySesionIdSesionAndEstudianteIdEstudiante(anyLong(), anyLong());
    }

    @Test
    @DisplayName("No se puede finalizar dos veces")
    void noSeFinalizaDosVeces() {
        var finalizada = EvaluacionDiaria.builder()
                .idEvaluacion(99L).estado(EvaluacionDiaria.FINALIZADA).build();
        when(evaluacionRepository.findBySesionIdSesion(ID_SESION)).thenReturn(Optional.of(finalizada));

        assertThrows(IllegalArgumentException.class, () -> servicio.finalizar(ID_SESION, "ok"));
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abrir una sesion inexistente da 404, no crea nada")
    void sesionInexistente() {
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.abrir(ID_SESION));
        verify(evaluacionRepository, never()).save(any());
    }
}
