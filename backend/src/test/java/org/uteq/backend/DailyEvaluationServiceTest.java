package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.attendance.entity.Attendance;
import org.uteq.backend.deportivo.attendance.repository.AttendanceRepository;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.evaluation.dto.EvaluationDtos.*;
import org.uteq.backend.deportivo.evaluation.entity.*;
import org.uteq.backend.deportivo.evaluation.repository.*;
import org.uteq.backend.deportivo.evaluation.service.DailyEvaluationService;
import org.uteq.backend.deportivo.injury.repository.InjuryRepository;
import org.uteq.backend.deportivo.position.repository.PositionRepository;
import org.uteq.backend.deportivo.session.entity.TrainingSession;
import org.uteq.backend.deportivo.session.repository.TrainingSessionRepository;
import org.uteq.backend.seguridad.person.entity.Person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyEvaluationServiceTest {
    @Mock private DailyEvaluationRepository evaluacionRepository;
    @Mock private StudentEvaluationRepository studentEvaluationRepository;
    @Mock private EvaluationCriterionRepository criterioRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private TrainingSessionRepository sesionRepository;
    @Mock private InjuryRepository injuryRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private StudentRepository estudianteRepository;

    @InjectMocks private DailyEvaluationService servicio;

    private static final Long ID_SESION = 10L;
    private static final Long ID_ESTUDIANTE = 5L;

    private DailyEvaluation evaluacionBorrador() {
        return DailyEvaluation.builder()
                .idEvaluacion(99L)
                .estado(DailyEvaluation.BORRADOR)
                .build();
    }

    private Student estudiante() {
        return Student.builder()
                .id(ID_ESTUDIANTE)
                .person(Person.builder().name("Juan").lastName("Perez").build())
                .category(Category.builder().idCategoria(3L).nombre("SUB-12").build())
                .build();
    }

    private Attendance asistenciaCon(String estado) {
        return Attendance.builder()
                .estado(estado)
                .estudiante(estudiante())
                .build();
    }

    private SavePlayerRequest peticion() {
        return new SavePlayerRequest(ID_ESTUDIANTE, null,
                List.of(new CriterionScoreRequest(1L, new BigDecimal("7.5"))));
    }

    @Test
    @DisplayName("No se puede calificar a quien figura AUSENTE")
    void ausenteNoSePuedeCalificar() {
        when(evaluacionRepository.findBySession_Id(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(attendanceRepository.findBySession_IdAndStudent_Id(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.of(asistenciaCon("AUSENTE")));

        var e = assertThrows(IllegalArgumentException.class,
                () -> servicio.savePlayer(ID_SESION, peticion()));

        assertTrue(e.getMessage().contains("AUSENTE"));
        verify(studentEvaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sin registro de asistencia tampoco se guarda nada")
    void sinAsistenciaNoSeGuarda() {
        when(evaluacionRepository.findBySession_Id(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(attendanceRepository.findBySession_IdAndStudent_Id(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.savePlayer(ID_SESION, peticion()));

        verify(studentEvaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Quien llego TARDE si puede ser calificado")
    void tardeSiSePuedeCalificar() {
        when(evaluacionRepository.findBySession_Id(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(attendanceRepository.findBySession_IdAndStudent_Id(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.of(asistenciaCon(Attendance.ESTADO_TARDE)));
        when(studentEvaluationRepository.findByEvaluation_IdAndStudent_Id(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(criterioRepository.findActiveOrderByIdAsc()).thenReturn(
                List.of(EvaluationCriterion.builder()
                        .idCriterio(1L).nombre("Tecnica").puntajeMaximo((short) 10).build()));
        when(injuryRepository.findActiveByStudent(ID_ESTUDIANTE)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> servicio.savePlayer(ID_SESION, peticion()));

        verify(studentEvaluationRepository).save(any(StudentEvaluation.class));
    }

    @Test
    @DisplayName("La categoria que se guarda es la del estudiante en ese momento")
    void seCongelaLaCategoriaDelDia() {
        when(evaluacionRepository.findBySession_Id(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(attendanceRepository.findBySession_IdAndStudent_Id(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.of(asistenciaCon(Attendance.ESTADO_PRESENTE)));
        when(studentEvaluationRepository.findByEvaluation_IdAndStudent_Id(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(criterioRepository.findActiveOrderByIdAsc()).thenReturn(
                List.of(EvaluationCriterion.builder()
                        .idCriterio(1L).nombre("Tecnica").puntajeMaximo((short) 10).build()));
        when(injuryRepository.findActiveByStudent(ID_ESTUDIANTE)).thenReturn(Optional.empty());

        servicio.savePlayer(ID_SESION, peticion());

        var captor = org.mockito.ArgumentCaptor.forClass(StudentEvaluation.class);
        verify(studentEvaluationRepository).save(captor.capture());
        assertEquals("SUB-12", captor.getValue().getCategoriaDia().getNombre());
    }

    @Test
    @DisplayName("Un puntaje por encima del maximo del criterio se rechaza")
    void puntajeSobreElMaximoSeRechaza() {
        when(evaluacionRepository.findBySession_Id(ID_SESION))
                .thenReturn(Optional.of(evaluacionBorrador()));
        when(attendanceRepository.findBySession_IdAndStudent_Id(ID_SESION, ID_ESTUDIANTE))
                .thenReturn(Optional.of(asistenciaCon(Attendance.ESTADO_PRESENTE)));
        when(studentEvaluationRepository.findByEvaluation_IdAndStudent_Id(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(criterioRepository.findActiveOrderByIdAsc()).thenReturn(
                List.of(EvaluationCriterion.builder()
                        .idCriterio(1L).nombre("Tecnica").puntajeMaximo((short) 10).build()));
        when(injuryRepository.findActiveByStudent(ID_ESTUDIANTE)).thenReturn(Optional.empty());

        var excesivo = new SavePlayerRequest(ID_ESTUDIANTE, null,
                List.of(new CriterionScoreRequest(1L, new BigDecimal("11.0"))));

        var e = assertThrows(IllegalArgumentException.class,
                () -> servicio.savePlayer(ID_SESION, excesivo));
        assertTrue(e.getMessage().contains("maximo"));
    }

    @Test
    @DisplayName("Una evaluacion finalizada ya no admite cambios")
    void finalizadaNoAdmiteCambios() {
        var finalizada = DailyEvaluation.builder()
                .idEvaluacion(99L).estado(DailyEvaluation.FINALIZADA).build();
        when(evaluacionRepository.findBySession_Id(ID_SESION)).thenReturn(Optional.of(finalizada));

        var e = assertThrows(IllegalArgumentException.class,
                () -> servicio.savePlayer(ID_SESION, peticion()));

        assertTrue(e.getMessage().contains("finalizada"));
        verify(attendanceRepository, never())
                .findBySession_IdAndStudent_Id(anyLong(), anyLong());
    }

    @Test
    @DisplayName("No se puede finalizar dos veces")
    void noSeFinalizaDosVeces() {
        var finalizada = DailyEvaluation.builder()
                .idEvaluacion(99L).estado(DailyEvaluation.FINALIZADA).build();
        when(evaluacionRepository.findBySession_Id(ID_SESION)).thenReturn(Optional.of(finalizada));

        assertThrows(IllegalArgumentException.class, () -> servicio.finish(ID_SESION, "ok"));
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("RNF-25/H-02 - la observacion general no puede superar 2000 caracteres")
    void observacionGeneralConTopeDeLongitud() {
        String largo = "x".repeat(2001);

        assertThrows(IllegalArgumentException.class, () -> servicio.finish(ID_SESION, largo));
        verify(evaluacionRepository, never()).findBySession_Id(any());
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abrir una sesion inexistente da 404, no crea nada")
    void sesionInexistente() {
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.open(ID_SESION));
        verify(evaluacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("abrir trae el idLesion del jugador con lesion activa, y null para el que no la tiene")
    void abrirTraeIdLesionDelJugadorLesionado() {
        var categoria = Category.builder().idCategoria(3L).nombre("SUB-12").build();
        var sesion = TrainingSession.builder()
                .idSesion(ID_SESION).categoria(categoria).fecha(LocalDate.of(2026, 8, 14)).build();

        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion));
        when(evaluacionRepository.findBySession_Id(ID_SESION)).thenReturn(Optional.of(evaluacionBorrador()));
        when(criterioRepository.findActiveOrderByIdAsc()).thenReturn(List.of());
        when(injuryRepository.activeInjuryIdsByStudent())
                .thenReturn(List.<Object[]>of(new Object[]{ID_ESTUDIANTE, 77L}));
        when(sesionRepository.findByCategoryAndDateBeforeOrderByDateDesc(eq(3L), any(), any()))
                .thenReturn(List.of());
        when(attendanceRepository.findBySession_Id(ID_SESION))
                .thenReturn(List.of(asistenciaCon(Attendance.ESTADO_PRESENTE)));
        when(estudianteRepository.findByCategory_IdCategoriaAndActiveTrueOrderByPerson_LastNameAsc(3L))
                .thenReturn(List.of(estudiante()));
        when(studentEvaluationRepository.findByEvaluation_IdAndStudent_Id(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        var respuesta = servicio.open(ID_SESION);

        assertEquals(1, respuesta.jugadores().size());
        var jugador = respuesta.jugadores().get(0);
        assertTrue(jugador.lesionado());
        assertEquals(77L, jugador.idLesion());
    }
}
