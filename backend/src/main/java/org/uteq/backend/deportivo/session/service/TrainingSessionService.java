package org.uteq.backend.deportivo.session.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.category.repository.CategoryRepository;
import org.uteq.backend.deportivo.coach.entity.Coach;
import org.uteq.backend.deportivo.coach.repository.CoachRepository;
import org.uteq.backend.deportivo.evaluation.repository.DailyEvaluationRepository;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.deportivo.attendance.entity.Attendance;
import org.uteq.backend.deportivo.attendance.repository.AttendanceRepository;
import org.uteq.backend.deportivo.schedule.service.ScheduleService;
import org.uteq.backend.deportivo.session.dto.SessionCreateRequest;
import org.uteq.backend.deportivo.session.dto.SessionHistoryResponse;
import org.uteq.backend.deportivo.session.dto.SessionTodayResponse;
import org.uteq.backend.deportivo.session.entity.TrainingSession;
import org.uteq.backend.deportivo.session.repository.TrainingSessionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de sesiones de entrenamiento, antes embebida en
 * {@code TrainingSessionController} (hallazgo D-03 del informe de
 * evaluación de calidad). El controlador conserva la resolución de identidad
 * desde el contexto de seguridad y delega aquí el resto; este servicio se
 * prueba con un {@code String} cualquiera, sin simular contexto.
 */
@Service
@RequiredArgsConstructor
public class TrainingSessionService {
    private final TrainingSessionRepository sesionRepository;
    private final CoachRepository coachRepository;
    private final DailyEvaluationRepository evaluacionRepository;
    private final CategoryRepository categoryRepository;
    private final ScheduleService scheduleService;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository estudianteRepository;

    /**
     * Sesiones de hoy. Antes de consultar, materializa las sesiones
     * programadas de los horarios fijos.
     *
     * @param username           usuario autenticado
     * @param veTodasLasSesiones  {@code true} para {@code ADMINISTRADOR} /
     *                            {@code RECEPCIONISTA} (todas); {@code false}
     *                            filtra por el entrenador del username
     * @return las sesiones de hoy visibles
     */
    @Transactional
    public List<SessionTodayResponse> todaysSessions(String username, boolean veTodasLasSesiones) {
        scheduleService.generateScheduledSessions();
        LocalDate hoy = LocalDate.now(Zones.ECUADOR);

        List<TrainingSession> sesiones;
        if (veTodasLasSesiones) {
            sesiones = sesionRepository.findByDateOrderByStartTimeAsc(hoy);
        } else {
            Coach entrenador = coachByUsername(username);
            sesiones = entrenador == null
                    ? List.of()
                    : sesionRepository.findByDateOrderByStartTimeAsc(hoy).stream()
                        .filter(s -> s.getEntrenador().getIdEntrenador().equals(entrenador.getIdEntrenador()))
                        .toList();
        }

        return sesiones.stream().map(this::toResponse).toList();
    }

    /**
     * Historial de sesiones (pasadas y futuras), paginado y ordenado por
     * fecha descendente.
     *
     * @param username           usuario autenticado
     * @param veTodasLasSesiones  {@code true} para {@code ADMINISTRADOR}
     *                            (todas); {@code false} filtra por el
     *                            entrenador del username
     * @param page               número de página (desde 0)
     * @param size               tamaño de página
     * @return la página de sesiones
     */
    @Transactional
    public List<SessionTodayResponse> mySessions(String username, boolean veTodasLasSesiones, int page, int size) {
        scheduleService.generateScheduledSessions();

        if (veTodasLasSesiones) {
            Page<TrainingSession> todas = sesionRepository.findAll(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha")));
            return todas.map(this::toResponse).getContent();
        }

        Coach entrenador = coachByUsername(username);
        if (entrenador == null) {
            return List.of();
        }

        Page<TrainingSession> pagina = sesionRepository.sessionsByCoach(
                entrenador.getIdEntrenador(), PageRequest.of(page, size));
        return pagina.map(this::toResponse).getContent();
    }

    /**
     * Alta de una sesión propia. El {@code idEntrenador} nunca viene del
     * cliente: se resuelve del username, para que un entrenador no cree una
     * sesión "a nombre" de otro.
     *
     * @param username usuario autenticado (entrenador)
     * @param request  categoría, fecha, franja horaria y campo
     * @return la sesión creada
     * @throws ResourceNotFoundException si la cuenta no tiene entrenador
     *                                      asociado o la categoría no existe
     * @throws IllegalArgumentException     si la franja es inválida o se
     *                                      solapa con otra sesión de la misma
     *                                      categoría
     */
    @Transactional
    public SessionTodayResponse create(String username, SessionCreateRequest request) {
        Coach entrenador = coachByUsername(username);
        if (entrenador == null) {
            throw new ResourceNotFoundException("No hay un entrenador asociado a esta cuenta");
        }

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Category categoria = categoryRepository.findById(request.idCategoria())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category no encontrada con id: " + request.idCategoria()));

        if (sesionRepository.hasOverlap(request.idCategoria(), request.fecha(),
                                          request.horaInicio(), request.horaFin())) {
            throw new IllegalArgumentException(
                    "Ya hay una sesión de esa categoría ese día en ese horario");
        }

        TrainingSession sesion = TrainingSession.builder()
                .entrenador(entrenador)
                .categoria(categoria)
                .fecha(request.fecha())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .campo(request.campo())
                .estado("PROGRAMADA")
                .build();

        sesion = sesionRepository.save(sesion);
        return toResponse(sesion);
    }

    /**
     * Qué pasó en una sesión: quién estuvo, quién faltó y quién no tiene
     * registro. Se parte del plantel de la categoría y no de las filas de
     * asistencia: si nadie pasó lista, "no se registró la asistencia de
     * nadie" es distinto de "no había nadie convocado". Las marcas de chicos
     * que ya no están en la categoría se conservan: estuvieron ese día.
     *
     * @param idSesion identificador de la sesión
     * @return el resumen por estado y la fila de cada estudiante del plantel
     * @throws ResourceNotFoundException si la sesión no existe
     */
    @Transactional(readOnly = true)
    public SessionHistoryResponse history(Long idSesion) {
        TrainingSession s = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la sesion " + idSesion));

        Map<Long, Attendance> porEstudiante = new HashMap<>();
        for (Attendance a : attendanceRepository.sessionHistory(idSesion)) {
            porEstudiante.put(a.getEstudiante().getId(), a);
        }

        List<Student> plantel = estudianteRepository
                .findByCategory_IdCategoriaAndActiveTrueOrderByPerson_LastNameAsc(
                        s.getCategoria().getIdCategoria());

        List<SessionHistoryResponse.AttendanceRow> filas = new ArrayList<>();
        int presentes = 0, tarde = 0, ausentes = 0, justificados = 0, sinRegistro = 0;
        for (Student e : plantel) {
            Attendance a = porEstudiante.remove(e.getId());
            String estado = a == null ? "SIN_REGISTRO" : a.getEstado();
            switch (estado) {
                case Attendance.ESTADO_PRESENTE -> presentes++;
                case Attendance.ESTADO_TARDE -> tarde++;
                case Attendance.ESTADO_AUSENTE -> ausentes++;
                case Attendance.ESTADO_JUSTIFICADO -> justificados++;
                default -> sinRegistro++;
            }
            filas.add(new SessionHistoryResponse.AttendanceRow(
                    e.getId(),
                    e.getPerson().getName() + " " + e.getPerson().getLastName(),
                    e.getPosition() == null ? null : e.getPosition().getAbreviatura(),
                    estado,
                    a == null ? null : a.getHoraEntrada(),
                    a == null ? null : a.getMetodo(),
                    a == null ? null : a.getObservacion()));
        }

        for (Attendance a : porEstudiante.values()) {
            Student e = a.getEstudiante();
            switch (a.getEstado()) {
                case Attendance.ESTADO_PRESENTE -> presentes++;
                case Attendance.ESTADO_TARDE -> tarde++;
                case Attendance.ESTADO_AUSENTE -> ausentes++;
                case Attendance.ESTADO_JUSTIFICADO -> justificados++;
                default -> { }
            }
            filas.add(new SessionHistoryResponse.AttendanceRow(
                    e.getId(),
                    e.getPerson().getName() + " " + e.getPerson().getLastName(),
                    e.getPosition() == null ? null : e.getPosition().getAbreviatura(),
                    a.getEstado(), a.getHoraEntrada(), a.getMetodo(), a.getObservacion()));
        }

        var evaluacion = evaluacionRepository.findBySession_Id(idSesion);
        var persona = s.getEntrenador().getPersona();
        return new SessionHistoryResponse(
                s.getIdSesion(),
                s.getCategoria().getNombre(),
                persona.getName() + " " + persona.getLastName(),
                s.getFecha(), s.getHoraInicio(), s.getHoraFin(), s.getCampo(), s.getEstado(),
                evaluacion.isPresent(),
                evaluacion.map(ev -> ev.getEstado()).orElse(null),
                new SessionHistoryResponse.Summary(filas.size(), presentes, tarde,
                        ausentes, justificados, sinRegistro),
                filas);
    }

    private Coach coachByUsername(String username) {
        return coachRepository.findByUserAccount_Username(username).orElse(null);
    }

    private SessionTodayResponse toResponse(TrainingSession s) {
        var persona = s.getEntrenador().getPersona();
        return new SessionTodayResponse(
                s.getIdSesion(),
                s.getCategoria().getNombre(),
                persona.getName() + " " + persona.getLastName(),
                s.getFecha(),
                s.getHoraInicio(),
                s.getHoraFin(),
                s.getCampo(),
                s.getEstado(),
                evaluacionRepository.existsBySession_Id(s.getIdSesion()));
    }
}
