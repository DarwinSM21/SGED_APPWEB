package org.uteq.backend.deportivo.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.attendance.entity.Attendance;
import org.uteq.backend.deportivo.attendance.repository.AttendanceRepository;
import org.uteq.backend.deportivo.evaluation.dto.EvaluationDtos.*;
import org.uteq.backend.deportivo.evaluation.entity.*;
import org.uteq.backend.deportivo.evaluation.repository.*;
import org.uteq.backend.deportivo.injury.repository.InjuryRepository;
import org.uteq.backend.deportivo.position.repository.PositionRepository;
import org.uteq.backend.seguridad.audit.aop.Audited;
import org.uteq.backend.deportivo.session.entity.TrainingSession;
import org.uteq.backend.deportivo.session.repository.TrainingSessionRepository;

import java.math.BigDecimal;
import java.util.*;

/**
 * Evaluación diaria del entrenador. Concentra tres reglas que no son
 * evidentes desde el esquema:
 * <ol>
 *   <li><b>Sin asistencia no hay calificación.</b> Solo se puede evaluar a
 *       quien marcó {@code PRESENTE} o {@code TARDE}: evita registros de
 *       desempeño de alguien que no fue.</li>
 *   <li><b>Cada día arranca con los valores del anterior.</b> El entrenador
 *       ajusta lo que cambió en vez de recalificar desde cero. Los valores
 *       heredados se marcan como precargados.</li>
 *   <li><b>Se guarda la categoría del día.</b> Si un jugador cambia de
 *       categoría en marzo, sus evaluaciones de febrero siguen diciendo
 *       SUB-12, que es donde estaba.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class DailyEvaluationService {
    private final DailyEvaluationRepository evaluacionRepository;
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final EvaluationCriterionRepository criterioRepository;
    private final AttendanceRepository attendanceRepository;
    private final TrainingSessionRepository sesionRepository;
    private final InjuryRepository injuryRepository;
    private final PositionRepository positionRepository;
    private final StudentRepository estudianteRepository;

    /**
     * Abre la pantalla de evaluación de una sesión. Si todavía no existe la
     * cabecera, la crea en {@code BORRADOR}. Lista <em>todos</em> los
     * estudiantes activos de la categoría (no solo quien marcó asistencia):
     * el entrenador necesita ver a los que faltaron, aunque no pueda
     * calificarlos.
     *
     * @param idSesion identificador de la sesión
     * @return criterios activos, jugadores evaluables con su precarga y el
     *         estado de la evaluación
     * @throws ResourceNotFoundException si la sesión no existe
     */
    @Transactional
    public SessionEvaluationResponse open(Long idSesion) {
        TrainingSession sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la sesion " + idSesion));

        DailyEvaluation evaluacion = evaluacionRepository.findBySession_Id(idSesion)
                .orElseGet(() -> evaluacionRepository.save(DailyEvaluation.builder()
                        .sesion(sesion)
                        .entrenador(sesion.getEntrenador())
                        .fecha(sesion.getFecha())
                        .estado(DailyEvaluation.BORRADOR)
                        .build()));

        List<EvaluationCriterion> criterios = criterioRepository.findActiveOrderByIdAsc();
        Map<Long, Long> lesionActivaPorEstudiante = new HashMap<>();
        for (Object[] fila : injuryRepository.activeInjuryIdsByStudent()) {
            lesionActivaPorEstudiante.put((Long) fila[0], (Long) fila[1]);
        }

        Long idEvaluacionPrevia = findPreviousEvaluation(sesion);

        Map<Long, Attendance> asistenciaPorEstudiante = new HashMap<>();
        for (Attendance asistencia : attendanceRepository.findBySession_Id(idSesion)) {
            asistenciaPorEstudiante.put(asistencia.getEstudiante().getId(), asistencia);
        }

        List<Student> estudiantesCategoria = estudianteRepository
                .findByCategory_IdCategoriaAndActiveTrueOrderByPerson_LastNameAsc(sesion.getCategoria().getIdCategoria());

        List<EvaluablePlayerResponse> jugadores = new ArrayList<>();
        for (Student estudiante : estudiantesCategoria) {
            jugadores.add(buildPlayer(
                    estudiante, asistenciaPorEstudiante.get(estudiante.getId()),
                    evaluacion, idEvaluacionPrevia, lesionActivaPorEstudiante));
        }
        jugadores.sort(Comparator.comparing(EvaluablePlayerResponse::nombreCompleto));

        return new SessionEvaluationResponse(
                evaluacion.getIdEvaluacion(),
                idSesion,
                evaluacion.getFecha(),
                sesion.getCategoria().getNombre(),
                evaluacion.getEstado(),
                criterios.stream().map(c -> new CriterionResponse(
                        c.getIdCriterio(), c.getNombre(), c.getDescripcion(), c.getPuntajeMaximo())).toList(),
                jugadores,
                evaluacion.getObservacionGeneral());
    }

    private EvaluablePlayerResponse buildPlayer(Student estudiante, Attendance asistencia,
                                                      DailyEvaluation evaluacion,
                                                      Long idEvaluacionPrevia,
                                                      Map<Long, Long> lesionActivaPorEstudiante) {
        Long idEstudiante = estudiante.getId();
        var persona = estudiante.getPerson();
        String nombre = persona.getName() + " " + persona.getLastName();

        boolean puedeEvaluarse = asistencia != null && asistencia.enablesEvaluation();
        String motivo = puedeEvaluarse ? null
                : asistencia == null
                        ? "No marcó asistencia en esta sesión"
                        : "No se puede calificar: la asistencia figura como " + asistencia.getEstado();

        var yaEvaluado = studentEvaluationRepository
                .findByEvaluation_IdAndStudent_Id(
                        evaluacion.getIdEvaluacion(), idEstudiante);

        Map<String, BigDecimal> puntajes = new LinkedHashMap<>();
        boolean precargado = false;

        Long idPosicion = estudiante.getPosition() != null ? estudiante.getPosition().getIdPosicion() : null;
        String posicion = estudiante.getPosition() != null ? estudiante.getPosition().getNombre() : null;

        if (yaEvaluado.isPresent()) {
            var ee = yaEvaluado.get();
            for (EvaluationDetail d : ee.getDetalles()) {
                puntajes.put(d.getCriterio().getNombre(), d.getPuntaje());
            }
        } else if (idEvaluacionPrevia != null && puedeEvaluarse) {
            for (Object[] fila : studentEvaluationRepository
                    .scoresForEvaluation(idEstudiante, idEvaluacionPrevia)) {
                puntajes.put((String) fila[0], (BigDecimal) fila[1]);
            }
            precargado = !puntajes.isEmpty();
        }

        Long idLesionActiva = lesionActivaPorEstudiante.get(idEstudiante);
        return new EvaluablePlayerResponse(
                idEstudiante, nombre,
                estudiante.getCategory().getNombre(),
                idPosicion, posicion,
                asistencia == null ? null : asistencia.getEstado(),
                puntajes, precargado,
                idLesionActiva != null,
                idLesionActiva,
                puedeEvaluarse, motivo);
    }

    private Long findPreviousEvaluation(TrainingSession sesion) {
        var previas = sesionRepository.findByCategoryAndDateBeforeOrderByDateDesc(
                sesion.getCategoria().getIdCategoria(), sesion.getFecha(),
                org.springframework.data.domain.PageRequest.of(0, 1));
        if (previas.isEmpty()) {
            return null;
        }
        return evaluacionRepository.findBySession_Id(previas.get(0).getIdSesion())
                .map(DailyEvaluation::getIdEvaluacion)
                .orElse(null);
    }

    /**
     * Guarda los puntajes de un jugador. Es la operación del autoguardado:
     * se invoca muchas veces por sesión y es idempotente (reescribe en vez
     * de acumular). La auditoría queda deliberadamente genérica, sin el
     * detalle de qué criterio cambió.
     *
     * @param idSesion identificador de la sesión
     * @param request  posición jugada ({@code null} = quitarla) y puntajes
     *                 por criterio
     * @throws ResourceNotFoundException si la sesión no tiene evaluación
     *                                      abierta o la posición no existe
     * @throws IllegalArgumentException     si la evaluación ya fue
     *                                      finalizada, el estudiante no tiene
     *                                      asistencia habilitante, o un
     *                                      puntaje supera su máximo
     */
    @Audited(action = "EDITAR", entity = "Estudiante", idSpel = "#p1.idEstudiante",
            descriptionSpel = "'editó estadísticas de estudiante #' + #p1.idEstudiante")
    @Transactional
    public void savePlayer(Long idSesion, SavePlayerRequest request) {
        DailyEvaluation evaluacion = evaluacionRepository.findBySession_Id(idSesion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La sesion " + idSesion + " no tiene evaluacion abierta"));

        if (evaluacion.isFinished()) {
            throw new IllegalArgumentException(
                    "La evaluacion ya fue finalizada y no admite cambios");
        }

        Attendance asistencia = attendanceRepository
                .findBySession_IdAndStudent_Id(idSesion, request.idEstudiante())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El estudiante no tiene asistencia registrada en esta sesion"));

        if (!asistencia.enablesEvaluation()) {
            throw new IllegalArgumentException(
                    "No se puede calificar a un estudiante cuya asistencia es "
                            + asistencia.getEstado());
        }

        var estudiante = asistencia.getEstudiante();

        StudentEvaluation ee = studentEvaluationRepository
                .findByEvaluation_IdAndStudent_Id(
                        evaluacion.getIdEvaluacion(), request.idEstudiante())
                .orElseGet(() -> StudentEvaluation.builder()
                        .evaluacion(evaluacion)
                        .estudiante(estudiante)
                        // Regla 3: la categoría del día se congela al crear la
                        // fila, tomándola del estudiante en este momento.
                        .categoriaDia(estudiante.getCategory())
                        .build());

        // El frontend siempre manda este campo: null es una instrucción
        // explícita de "quitar la posición", no "no tocar nada".
        if (request.idPosicionJugada() != null) {
            ee.setPosicionJugada(positionRepository.findById(request.idPosicionJugada())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No existe la posicion " + request.idPosicionJugada())));
        } else {
            ee.setPosicionJugada(null);
        }

        injuryRepository.findActiveByStudent(request.idEstudiante())
                .ifPresent(ee::setLesion);

        applyScores(ee, request);
        studentEvaluationRepository.save(ee);
    }

    private void applyScores(StudentEvaluation ee, SavePlayerRequest request) {
        Map<Long, EvaluationCriterion> criterios = new HashMap<>();
        criterioRepository.findActiveOrderByIdAsc()
                .forEach(c -> criterios.put(c.getIdCriterio(), c));

        Map<Long, EvaluationDetail> existentes = new HashMap<>();
        ee.getDetalles().forEach(d -> existentes.put(d.getCriterio().getIdCriterio(), d));

        for (CriterionScoreRequest p : request.puntajes()) {
            EvaluationCriterion criterio = criterios.get(p.idCriterio());
            if (criterio == null) {
                throw new IllegalArgumentException(
                        "El criterio " + p.idCriterio() + " no existe o esta desactivado");
            }
            if (p.puntaje().compareTo(BigDecimal.valueOf(criterio.getPuntajeMaximo())) > 0) {
                throw new IllegalArgumentException(
                        "El puntaje de " + criterio.getNombre() + " supera su maximo de "
                                + criterio.getPuntajeMaximo());
            }

            EvaluationDetail detalle = existentes.get(p.idCriterio());
            if (detalle == null) {
                detalle = EvaluationDetail.builder()
                        .evaluacionEstudiante(ee)
                        .criterio(criterio)
                        .puntaje(p.puntaje())
                        .build();
                ee.getDetalles().add(detalle);
            } else {
                detalle.setPuntaje(p.puntaje());
            }
        }
    }

    /** RNF-25 / hallazgo H-02: tope de longitud del texto libre sobre un menor. */
    static final int MAX_OBSERVACION_GENERAL = 2000;

    /**
     * Cierra la evaluación. A partir de aquí no admite cambios.
     *
     * @param idSesion           identificador de la sesión
     * @param observacionGeneral observación general de la sesión; puede ser
     *                           {@code null}
     * @throws ResourceNotFoundException si la sesión no tiene evaluación
     *                                      abierta
     * @throws IllegalArgumentException     si ya estaba finalizada o si la
     *                                      observación supera el tope
     */
    @Audited(action = "EDITAR", entity = "DailyEvaluation", idSpel = "#p0",
            descriptionSpel = "'finalizó la evaluación de la sesión #' + #p0")
    @Transactional
    public void finish(Long idSesion, String observacionGeneral) {
        if (observacionGeneral != null && observacionGeneral.length() > MAX_OBSERVACION_GENERAL) {
            throw new IllegalArgumentException(
                    "La observación general no puede superar " + MAX_OBSERVACION_GENERAL + " caracteres");
        }

        DailyEvaluation evaluacion = evaluacionRepository.findBySession_Id(idSesion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La sesion " + idSesion + " no tiene evaluacion abierta"));

        if (evaluacion.isFinished()) {
            throw new IllegalArgumentException("La evaluacion ya estaba finalizada");
        }

        evaluacion.setObservacionGeneral(observacionGeneral);
        evaluacion.setEstado(DailyEvaluation.FINALIZADA);
        evaluacionRepository.save(evaluacion);
    }
}
