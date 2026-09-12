package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.attendance.entity.Attendance;
import org.uteq.backend.deportivo.attendance.repository.AttendanceRepository;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.category.repository.CategoryRepository;
import org.uteq.backend.deportivo.coach.entity.Coach;
import org.uteq.backend.deportivo.coach.repository.CoachRepository;
import org.uteq.backend.deportivo.evaluation.entity.DailyEvaluation;
import org.uteq.backend.deportivo.evaluation.repository.DailyEvaluationRepository;
import org.uteq.backend.deportivo.schedule.service.ScheduleService;
import org.uteq.backend.deportivo.position.entity.Position;
import org.uteq.backend.deportivo.session.dto.SessionCreateRequest;
import org.uteq.backend.deportivo.session.dto.SessionHistoryResponse;
import org.uteq.backend.deportivo.session.dto.SessionTodayResponse;
import org.uteq.backend.deportivo.session.entity.TrainingSession;
import org.uteq.backend.deportivo.session.repository.TrainingSessionRepository;
import org.uteq.backend.deportivo.session.service.TrainingSessionService;
import org.uteq.backend.seguridad.person.entity.Person;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingSessionServiceTest {
    @Mock private TrainingSessionRepository sesionRepository;
    @Mock private CoachRepository coachRepository;
    @Mock private DailyEvaluationRepository evaluacionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ScheduleService scheduleService;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private StudentRepository estudianteRepository;

    @InjectMocks private TrainingSessionService sesionService;

    private Coach entrenador(long id, String nombre) {
        return Coach.builder()
                .idEntrenador(id)
                .persona(Person.builder().name(nombre).lastName("Apellido").build())
                .build();
    }

    private TrainingSession sesionDe(Coach e) {
        return TrainingSession.builder()
                .idSesion(e.getIdEntrenador() * 100)
                .entrenador(e)
                .categoria(Category.builder().idCategoria(1L).nombre("SUB-12").build())
                .estado("PROGRAMADA")
                .build();
    }

    @Test
    @DisplayName("Un entrenador solo ve sus propias sesiones, no las de otro")
    void entrenadorSoloVeLasPropias() {
        var yo = entrenador(1L, "Carlos");
        var otro = entrenador(2L, "Marta");

        when(coachRepository.findByUserAccount_Username("carlos@sged.test"))
                .thenReturn(Optional.of(yo));
        when(sesionRepository.findByDateOrderByStartTimeAsc(any()))
                .thenReturn(List.of(sesionDe(yo), sesionDe(otro)));
        when(evaluacionRepository.existsBySession_Id(anyLong())).thenReturn(false);

        List<SessionTodayResponse> resultado = sesionService.todaysSessions("carlos@sged.test", false);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).entrenador()).isEqualTo("Carlos Apellido");
    }

    @Test
    @DisplayName("Un administrador ve las sesiones de todos")
    void administradorVeTodas() {
        var e1 = entrenador(1L, "Carlos");
        var e2 = entrenador(2L, "Marta");

        when(sesionRepository.findByDateOrderByStartTimeAsc(any()))
                .thenReturn(List.of(sesionDe(e1), sesionDe(e2)));
        when(evaluacionRepository.existsBySession_Id(anyLong())).thenReturn(false);

        List<SessionTodayResponse> resultado = sesionService.todaysSessions("admin@sged.test", true);

        assertThat(resultado).hasSize(2);
        verify(coachRepository, never()).findByUserAccount_Username(any());
    }

    @Test
    @DisplayName("Una cuenta ENTRENADOR sin fila de entrenador asociada no ve nada, no falla")
    void sinEntrenadorAsociadoListaVacia() {
        when(coachRepository.findByUserAccount_Username("huerfano@sged.test"))
                .thenReturn(Optional.empty());

        List<SessionTodayResponse> resultado = sesionService.todaysSessions("huerfano@sged.test", false);

        assertThat(resultado).isEmpty();

        verify(sesionRepository, never()).findByDateOrderByStartTimeAsc(any());
    }

    @Test
    @DisplayName("El indicador tieneEvaluacion refleja si ya existe la cabecera")
    void indicaSiYaTieneEvaluacion() {
        var yo = entrenador(1L, "Carlos");
        var sesion = sesionDe(yo);

        when(sesionRepository.findByDateOrderByStartTimeAsc(any())).thenReturn(List.of(sesion));
        when(evaluacionRepository.existsBySession_Id(sesion.getIdSesion())).thenReturn(true);

        List<SessionTodayResponse> resultado = sesionService.todaysSessions("admin@sged.test", true);

        assertThat(resultado.get(0).tieneEvaluacion()).isTrue();
    }

    @Test
    @DisplayName("hoy genera antes de leer, para que una sesion de horario fijo aparezca sin que nadie la cree a mano")
    void hoyGeneraLasSesionesDelHorarioFijoAntesDeListar() {
        when(sesionRepository.findByDateOrderByStartTimeAsc(any())).thenReturn(List.of());

        sesionService.todaysSessions("admin@sged.test", true);

        verify(scheduleService).generateScheduledSessions();
    }

    @Test
    @DisplayName("mias devuelve el historial completo del entrenador, no solo las de hoy")
    void miasDevuelveHistorialCompleto() {
        var yo = entrenador(1L, "Carlos");
        var pasada = sesionDe(yo);
        pasada.setFecha(LocalDate.now().minusDays(4));

        when(coachRepository.findByUserAccount_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(sesionRepository.sessionsByCoach(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(pasada)));
        when(evaluacionRepository.existsBySession_Id(any())).thenReturn(false);

        List<SessionTodayResponse> resultado = sesionService.mySessions("carlos@sged.test", false, 0, 20);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("mias con veTodasLasSesiones=true devuelve el historial de todos, no requiere un Coach propio")
    void miasVeTodasNoRequiereEntrenadorPropio() {
        var yo = entrenador(1L, "Carlos");
        var sesion = sesionDe(yo);

        when(sesionRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(sesion)));
        when(evaluacionRepository.existsBySession_Id(any())).thenReturn(false);

        List<SessionTodayResponse> resultado = sesionService.mySessions("admin@sged.test", true, 0, 20);

        assertThat(resultado).hasSize(1);
        verify(coachRepository, never()).findByUserAccount_Username("admin@sged.test");
    }

    @Test
    @DisplayName("crear persiste la sesion a nombre del entrenador dueno del username, no de uno enviado en el request")
    void crearUsaElEntrenadorDelUsername() {
        var yo = entrenador(1L, "Carlos");
        var categoria = Category.builder().idCategoria(5L).nombre("SUB-15").build();

        when(coachRepository.findByUserAccount_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(categoria));
        when(evaluacionRepository.existsBySession_Id(any())).thenReturn(false);
        when(sesionRepository.save(any(TrainingSession.class))).thenAnswer(inv -> {
            TrainingSession s = inv.getArgument(0);
            s.setIdSesion(99L);
            return s;
        });

        var request = new SessionCreateRequest(5L, LocalDate.of(2026, 8, 10),
                LocalTime.of(16, 0), LocalTime.of(17, 30), "Cancha 1");

        SessionTodayResponse creada = sesionService.create("carlos@sged.test", request);

        assertThat(creada.categoria()).isEqualTo("SUB-15");
        assertThat(creada.entrenador()).isEqualTo("Carlos Apellido");
    }

    @Test
    @DisplayName("crear rechaza una hora de fin que no es posterior a la de inicio")
    void crearRechazaHoraFinNoPosterior() {
        var yo = entrenador(1L, "Carlos");
        when(coachRepository.findByUserAccount_Username("carlos@sged.test")).thenReturn(Optional.of(yo));

        var request = new SessionCreateRequest(5L, LocalDate.of(2026, 8, 10),
                LocalTime.of(17, 0), LocalTime.of(16, 0), null);

        assertThatThrownBy(() -> sesionService.create("carlos@sged.test", request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(sesionRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear lanza 404 (ResourceNotFoundException) si la categoria no existe")
    void crearCategoriaInexistenteLanzaExcepcion() {
        var yo = entrenador(1L, "Carlos");
        when(coachRepository.findByUserAccount_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        var request = new SessionCreateRequest(999L, LocalDate.of(2026, 8, 10),
                LocalTime.of(16, 0), LocalTime.of(17, 0), null);

        assertThatThrownBy(() -> sesionService.create("carlos@sged.test", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crear rechaza una sesion que se solapa con otra de la misma categoria")
    void crearRechazaSiYaHayUnaSesionSuperpuesta() {
        var yo = entrenador(1L, "Carlos");
        when(coachRepository.findByUserAccount_Username("carlos@sged.test")).thenReturn(Optional.of(yo));
        when(categoryRepository.findById(5L))
                .thenReturn(Optional.of(Category.builder().idCategoria(5L).nombre("SUB-15").build()));

        var request = new SessionCreateRequest(5L, LocalDate.of(2026, 8, 10),
                LocalTime.of(16, 0), LocalTime.of(17, 0), null);
        when(sesionRepository.hasOverlap(5L, request.fecha(), request.horaInicio(), request.horaFin()))
                .thenReturn(true);

        assertThatThrownBy(() -> sesionService.create("carlos@sged.test", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya hay una sesión");

        verify(sesionRepository, never()).save(any());
    }

    private Student estudianteDe(long id, String nombre, Position posicion) {
        return Student.builder()
                .id(id)
                .person(Person.builder().name(nombre).lastName("Apellido").build())
                .position(posicion)
                .build();
    }

    @Test
    @DisplayName("historial lanza 404 si la sesion no existe")
    void historialSesionInexistenteLanza404() {
        when(sesionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sesionService.history(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("historial cuenta presentes, tarde, ausentes, justificados y sin registro por separado")
    void historialCuentaCadaEstadoPorSeparado() {
        var yo = entrenador(1L, "Carlos");
        var categoria = Category.builder().idCategoria(1L).nombre("SUB-12").build();
        var sesion = TrainingSession.builder()
                .idSesion(500L).entrenador(yo).categoria(categoria).estado("PROGRAMADA")
                .fecha(LocalDate.of(2026, 8, 10))
                .build();
        when(sesionRepository.findById(500L)).thenReturn(Optional.of(sesion));

        var arquero = Position.builder().abreviatura("POR").build();
        Student presente = estudianteDe(1L, "Ana", arquero);
        Student tarde = estudianteDe(2L, "Beto", null);
        Student ausente = estudianteDe(3L, "Cindy", null);
        Student justificado = estudianteDe(4L, "Dario", null);
        Student sinRegistro = estudianteDe(5L, "Eva", null);
        when(estudianteRepository.findByCategory_IdCategoriaAndActiveTrueOrderByPerson_LastNameAsc(1L))
                .thenReturn(List.of(presente, tarde, ausente, justificado, sinRegistro));

        when(attendanceRepository.sessionHistory(500L)).thenReturn(List.of(
                Attendance.builder().estudiante(presente).estado(Attendance.ESTADO_PRESENTE)
                        .horaEntrada(LocalTime.of(16, 1)).metodo("QR").build(),
                Attendance.builder().estudiante(tarde).estado(Attendance.ESTADO_TARDE).build(),
                Attendance.builder().estudiante(ausente).estado(Attendance.ESTADO_AUSENTE).build(),
                Attendance.builder().estudiante(justificado).estado(Attendance.ESTADO_JUSTIFICADO)
                        .observacion("Cita medica").build()
                // 'sinRegistro' no tiene fila de asistencia -> cae en el caso por defecto
        ));
        when(evaluacionRepository.findBySession_Id(500L)).thenReturn(Optional.empty());

        SessionHistoryResponse r = sesionService.history(500L);

        assertThat(r.resumen().convocados()).isEqualTo(5);
        assertThat(r.resumen().presentes()).isEqualTo(1);
        assertThat(r.resumen().tarde()).isEqualTo(1);
        assertThat(r.resumen().ausentes()).isEqualTo(1);
        assertThat(r.resumen().justificados()).isEqualTo(1);
        assertThat(r.resumen().sinRegistro()).isEqualTo(1);
        assertThat(r.tieneEvaluacion()).isFalse();
        assertThat(r.estadoEvaluacion()).isNull();

        var filaPresente = r.asistencias().stream()
                .filter(f -> f.idEstudiante().equals(1L)).findFirst().orElseThrow();
        assertThat(filaPresente.posicion()).isEqualTo("POR");
        assertThat(filaPresente.metodo()).isEqualTo("QR");

        var filaSinRegistro = r.asistencias().stream()
                .filter(f -> f.idEstudiante().equals(5L)).findFirst().orElseThrow();
        assertThat(filaSinRegistro.estado()).isEqualTo("SIN_REGISTRO");
        assertThat(filaSinRegistro.posicion()).isNull();
    }

    @Test
    @DisplayName("historial tambien incluye a quien registro asistencia pero ya no esta en el plantel activo")
    void historialIncluyeAQuienYaNoEstaEnElPlantelActivo() {
        var yo = entrenador(1L, "Carlos");
        var categoria = Category.builder().idCategoria(1L).nombre("SUB-12").build();
        var sesion = TrainingSession.builder()
                .idSesion(501L).entrenador(yo).categoria(categoria).estado("FINALIZADA")
                .build();
        when(sesionRepository.findById(501L)).thenReturn(Optional.of(sesion));

        // Plantel activo vacio: el estudiante se dio de baja despues de la sesion.
        when(estudianteRepository.findByCategory_IdCategoriaAndActiveTrueOrderByPerson_LastNameAsc(1L))
                .thenReturn(List.of());

        Student deBaja = estudianteDe(9L, "Fabio", null);
        when(attendanceRepository.sessionHistory(501L)).thenReturn(List.of(
                Attendance.builder().estudiante(deBaja).estado(Attendance.ESTADO_PRESENTE).build()));
        when(evaluacionRepository.findBySession_Id(501L))
                .thenReturn(Optional.of(DailyEvaluation.builder().estado("CERRADA").build()));

        SessionHistoryResponse r = sesionService.history(501L);

        assertThat(r.resumen().convocados()).isEqualTo(1);
        assertThat(r.resumen().presentes()).isEqualTo(1);
        assertThat(r.asistencias()).hasSize(1);
        assertThat(r.asistencias().get(0).idEstudiante()).isEqualTo(9L);
        assertThat(r.tieneEvaluacion()).isTrue();
        assertThat(r.estadoEvaluacion()).isEqualTo("CERRADA");
    }
}
