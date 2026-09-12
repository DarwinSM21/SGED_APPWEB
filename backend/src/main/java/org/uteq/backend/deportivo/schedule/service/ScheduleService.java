package org.uteq.backend.deportivo.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.category.repository.CategoryRepository;
import org.uteq.backend.deportivo.coach.entity.Coach;
import org.uteq.backend.deportivo.coach.repository.CoachRepository;
import org.uteq.backend.deportivo.schedule.dto.ScheduleRequest;
import org.uteq.backend.deportivo.schedule.dto.ScheduleResponse;
import org.uteq.backend.deportivo.schedule.entity.Schedule;
import org.uteq.backend.deportivo.schedule.repository.ScheduleRepository;
import org.uteq.backend.deportivo.session.entity.TrainingSession;
import org.uteq.backend.deportivo.attendance.repository.AttendanceRepository;
import org.uteq.backend.deportivo.evaluation.repository.DailyEvaluationRepository;
import org.uteq.backend.deportivo.session.repository.TrainingSessionRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Horarios fijos semanales del entrenador y la materialización de las
 * sesiones concretas que se derivan de ellos. Un entrenador no puede tener
 * dos horarios cruzados el mismo día; la cancha no se valida (dos grupos
 * pueden compartirla, una persona no se parte en dos). Al cambiar un
 * horario, la semana en curso se rehace salvo los entrenamientos que ya se
 * dictaron (tienen asistencia o evaluación).
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final CoachRepository coachRepository;
    private final CategoryRepository categoryRepository;
    private final TrainingSessionRepository sesionRepository;
    private final AttendanceRepository attendanceRepository;
    private final DailyEvaluationRepository evaluacionRepository;

    /**
     * Cuántos días hacia adelante se programan de una vez. Con 7 basta una
     * sola apertura de la pantalla para dejar cubierta la semana completa.
     */
    @Value("${sesiones.dias-programados:7}")
    private int diasProgramados;

    /** Id imposible, para el alta: no hay horario propio que excluir todavía. */
    private static final Long SIN_ID_TODAVIA = -1L;

    /**
     * Crea un horario fijo para el entrenador autenticado.
     *
     * @param username nombre de usuario del entrenador
     * @param request  categoría, día de semana, franja horaria y campo
     * @return el horario creado
     * @throws ResourceNotFoundException si la cuenta no tiene entrenador
     *                                      asociado o la categoría no existe
     * @throws IllegalArgumentException     si la hora de fin no es posterior
     *                                      a la de inicio, o el horario se
     *                                      cruza con otro suyo el mismo día
     */
    @Transactional
    public ScheduleResponse create(String username, ScheduleRequest request) {
        Coach entrenador = authenticatedCoach(username);

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Category categoria = categoryRepository.findById(request.idCategoria())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category no encontrada con id: " + request.idCategoria()));

        // SIN_ID_TODAVIA porque el horario aún no existe: no hay nada que excluir.
        validateNoOverlap(entrenador.getIdEntrenador(), request, SIN_ID_TODAVIA);

        Schedule horario = Schedule.builder()
                .entrenador(entrenador)
                .categoria(categoria)
                .diaSemana(request.diaSemana().shortValue())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .campo(request.campo())
                .descripcion(request.descripcion())
                .activo(true)
                .build();

        return toResponse(scheduleRepository.save(horario));
    }

    /**
     * Un entrenador no puede tener dos horarios cruzados el mismo día. Un
     * choque en el horario se materializa una vez por semana durante meses.
     * La cancha no se valida (dos grupos pueden compartirla). El mensaje
     * nombra el horario con el que choca.
     *
     * @param idEntrenador entrenador dueño de los horarios
     * @param request      horario que se pretende crear o editar
     * @param idExcluir    horario a excluir de la comprobación (el que se
     *                     edita), o {@link #SIN_ID_TODAVIA} en un alta
     * @throws IllegalArgumentException si hay un cruce
     */
    private void validateNoOverlap(Long idEntrenador, ScheduleRequest request, Long idExcluir) {
        List<Schedule> choques = scheduleRepository.overlapsWith(
                idEntrenador, request.diaSemana().shortValue(),
                request.horaInicio(), request.horaFin(), idExcluir);
        if (choques.isEmpty()) {
            return;
        }
        Schedule otro = choques.get(0);
        throw new IllegalArgumentException(
                "Ese día ya tenés " + otro.getCategoria().getNombre() + " de "
                        + otro.getHoraInicio() + " a " + otro.getHoraFin()
                        + ". No podés estar en dos canchas a la vez: movelo de hora.");
    }

    /**
     * Horarios activos del entrenador autenticado, cada uno con la
     * descripción del primer horario suyo que se le cruza (o {@code null}).
     *
     * @param username nombre de usuario del entrenador
     * @return la lista de horarios; vacía si la cuenta no tiene entrenador
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> mySchedules(String username) {
        return coachRepository.findByUserAccount_Username(username)
                .map(entrenador -> {
                    List<Schedule> horarios = scheduleRepository
                            .findActiveByCoachOrderByDayAndStartTime(
                                    entrenador.getIdEntrenador());

                    // Se comparan en memoria y no con una consulta por fila: la
                    // semana de un entrenador son unos pocos horarios.
                    return horarios.stream().map(h -> toResponse(h, overlapOf(h, horarios))).toList();
                })
                .orElseGet(List::of);
    }

    // El primer horario del mismo día que se cruza con este, descrito para
    // mostrarlo. null si no hay choque.
    private String overlapOf(Schedule horario, List<Schedule> todos) {
        return todos.stream()
                .filter(o -> !o.getIdHorario().equals(horario.getIdHorario()))
                .filter(o -> o.getDiaSemana().equals(horario.getDiaSemana()))
                .filter(o -> o.getHoraInicio().isBefore(horario.getHoraFin())
                        && o.getHoraFin().isAfter(horario.getHoraInicio()))
                .findFirst()
                .map(o -> o.getCategoria().getNombre() + " (" + o.getHoraInicio()
                        + "–" + o.getHoraFin() + ")")
                .orElse(null);
    }

    /**
     * Desactiva un horario fijo del entrenador autenticado. Responde
     * {@code 404} uniforme si no existe o no es suyo (criterio IDOR).
     *
     * @param username  nombre de usuario del entrenador
     * @param idHorario identificador del horario
     * @throws ResourceNotFoundException si el horario no existe o no es
     *                                      del entrenador
     */
    @Transactional
    public void deactivate(String username, Long idHorario) {
        Coach entrenador = authenticatedCoach(username);
        Schedule horario = scheduleRepository
                .findByIdAndCoachId(idHorario, entrenador.getIdEntrenador())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule no encontrado con id: " + idHorario));
        horario.setActivo(false);
        scheduleRepository.save(horario);
    }

    /**
     * Cambia un horario fijo del entrenador y rehace su ventana de sesiones.
     * Responde {@code 404} uniforme si no existe o no es suyo.
     *
     * @param username  nombre de usuario del entrenador
     * @param idHorario identificador del horario
     * @param request   datos nuevos
     * @return el horario actualizado
     * @throws ResourceNotFoundException si el horario no existe o no es
     *                                      suyo, o la categoría no existe
     * @throws IllegalArgumentException     si la franja es inválida o se
     *                                      cruza con otro horario suyo
     */
    @Transactional
    public ScheduleResponse update(String username, Long idHorario, ScheduleRequest request) {
        Coach entrenador = authenticatedCoach(username);
        Schedule horario = scheduleRepository
                .findByIdAndCoachId(idHorario, entrenador.getIdEntrenador())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule no encontrado con id: " + idHorario));

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Category categoria = categoryRepository.findById(request.idCategoria())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category no encontrada con id: " + request.idCategoria()));

        // Se excluye a sí mismo: mover un horario media hora no es chocar consigo.
        validateNoOverlap(entrenador.getIdEntrenador(), request, idHorario);

        horario.setCategoria(categoria);
        horario.setDiaSemana(request.diaSemana().shortValue());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setCampo(request.campo());
        horario.setDescripcion(request.descripcion());
        scheduleRepository.save(horario);

        rebuildFutureSessions(horario);
        return toResponse(horario);
    }

    // Vuelve a materializar la ventana de este horario tras un cambio. Solo
    // se borran las sesiones que aún no ocurrieron Y en las que nadie
    // registró nada: una sesión con asistencia o evaluación se queda como
    // está, son hechos que ya pasaron.
    private void rebuildFutureSessions(Schedule horario) {
        LocalDate hoy = LocalDate.now(Zones.ECUADOR);

        for (TrainingSession sesion : sesionRepository
                .findBySchedule_IdAndDateGreaterThanEqual(horario.getIdHorario(), hoy)) {
            boolean tieneAsistencia = !attendanceRepository.findBySession_Id(sesion.getIdSesion()).isEmpty();
            boolean tieneEvaluacion = evaluacionRepository.existsBySession_Id(sesion.getIdSesion());
            if (tieneAsistencia || tieneEvaluacion) {
                continue;
            }
            sesionRepository.delete(sesion);
        }

        generateScheduledSessions();
    }

    /**
     * Materializa las sesiones que faltan a partir de los horarios fijos
     * activos, desde hoy y hasta {@code sesiones.dias-programados} días hacia
     * adelante. Idempotente a propósito: se llama en cada
     * {@code GET /api/sesiones/hoy} y {@code /mias}, y si la sesión de ese
     * horario ya existe para esa fecha no crea otra. No se generan fechas
     * pasadas: una sesión creada después de su día, sin asistencia ni
     * evaluación, se leería como un entrenamiento al que no fue nadie.
     */
    @Transactional
    public void generateScheduledSessions() {
        LocalDate hoy = LocalDate.now(Zones.ECUADOR);

        for (int desplazamiento = 0; desplazamiento <= Math.max(0, diasProgramados); desplazamiento++) {
            LocalDate fecha = hoy.plusDays(desplazamiento);
            short diaSemana = (short) fecha.getDayOfWeek().getValue();

            for (Schedule horario : scheduleRepository.findActiveByDayOfWeek(diaSemana)) {
                if (sesionRepository.existsBySchedule_IdAndDate(horario.getIdHorario(), fecha)) {
                    continue;
                }
                sesionRepository.save(TrainingSession.builder()
                        .horario(horario)
                        .entrenador(horario.getEntrenador())
                        .categoria(horario.getCategoria())
                        .fecha(fecha)
                        .horaInicio(horario.getHoraInicio())
                        .horaFin(horario.getHoraFin())
                        .campo(horario.getCampo())
                        .estado("PROGRAMADA")
                        .build());
            }
        }
    }

    private Coach authenticatedCoach(String username) {
        return coachRepository.findByUserAccount_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un entrenador asociado a esta cuenta"));
    }

    private ScheduleResponse toResponse(Schedule h) {
        return toResponse(h, null);
    }

    private ScheduleResponse toResponse(Schedule h, String chocaCon) {
        return new ScheduleResponse(
                h.getIdHorario(), h.getCategoria().getIdCategoria(), h.getCategoria().getNombre(),
                h.getDiaSemana().intValue(),
                h.getHoraInicio(), h.getHoraFin(), h.getCampo(), h.getDescripcion(), h.getActivo(),
                chocaCon);
    }
}
