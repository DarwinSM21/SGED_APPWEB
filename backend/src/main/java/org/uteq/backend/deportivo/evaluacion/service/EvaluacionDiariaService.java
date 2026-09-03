package org.uteq.backend.deportivo.evaluacion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos.*;
import org.uteq.backend.deportivo.evaluacion.entity.*;
import org.uteq.backend.deportivo.evaluacion.repository.*;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

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
public class EvaluacionDiariaService {
    private final EvaluacionDiariaRepository evaluacionRepository;
    private final EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    private final CriterioEvaluacionRepository criterioRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final SesionEntrenamientoRepository sesionRepository;
    private final LesionRepository lesionRepository;
    private final PosicionRepository posicionRepository;
    private final EstudianteRepository estudianteRepository;

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
     * @throws RecursoNoEncontradoException si la sesión no existe
     */
    @Transactional
    public EvaluacionSesionResponse abrir(Long idSesion) {
        SesionEntrenamiento sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sesion " + idSesion));

        EvaluacionDiaria evaluacion = evaluacionRepository.findBySesionIdSesion(idSesion)
                .orElseGet(() -> evaluacionRepository.save(EvaluacionDiaria.builder()
                        .sesion(sesion)
                        .entrenador(sesion.getEntrenador())
                        .fecha(sesion.getFecha())
                        .estado(EvaluacionDiaria.BORRADOR)
                        .build()));

        List<CriterioEvaluacion> criterios = criterioRepository.findByActivoTrueOrderByIdCriterioAsc();
        Map<Long, Long> lesionActivaPorEstudiante = new HashMap<>();
        for (Object[] fila : lesionRepository.idsYLesionActivaPorEstudiante()) {
            lesionActivaPorEstudiante.put((Long) fila[0], (Long) fila[1]);
        }

        Long idEvaluacionPrevia = buscarEvaluacionPrevia(sesion);

        Map<Long, Asistencia> asistenciaPorEstudiante = new HashMap<>();
        for (Asistencia asistencia : asistenciaRepository.findBySesionIdSesion(idSesion)) {
            asistenciaPorEstudiante.put(asistencia.getEstudiante().getIdEstudiante(), asistencia);
        }

        List<Estudiante> estudiantesCategoria = estudianteRepository
                .findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(sesion.getCategoria().getIdCategoria());

        List<JugadorEvaluableResponse> jugadores = new ArrayList<>();
        for (Estudiante estudiante : estudiantesCategoria) {
            jugadores.add(construirJugador(
                    estudiante, asistenciaPorEstudiante.get(estudiante.getIdEstudiante()),
                    evaluacion, idEvaluacionPrevia, lesionActivaPorEstudiante));
        }
        jugadores.sort(Comparator.comparing(JugadorEvaluableResponse::nombreCompleto));

        return new EvaluacionSesionResponse(
                evaluacion.getIdEvaluacion(),
                idSesion,
                evaluacion.getFecha(),
                sesion.getCategoria().getNombre(),
                evaluacion.getEstado(),
                criterios.stream().map(c -> new CriterioResponse(
                        c.getIdCriterio(), c.getNombre(), c.getDescripcion(), c.getPuntajeMaximo())).toList(),
                jugadores,
                evaluacion.getObservacionGeneral());
    }

    private JugadorEvaluableResponse construirJugador(Estudiante estudiante, Asistencia asistencia,
                                                      EvaluacionDiaria evaluacion,
                                                      Long idEvaluacionPrevia,
                                                      Map<Long, Long> lesionActivaPorEstudiante) {
        Long idEstudiante = estudiante.getIdEstudiante();
        var persona = estudiante.getPersona();
        String nombre = persona.getNombre() + " " + persona.getApellido();

        boolean puedeEvaluarse = asistencia != null && asistencia.habilitaEvaluacion();
        String motivo = puedeEvaluarse ? null
                : asistencia == null
                        ? "No marcó asistencia en esta sesión"
                        : "No se puede calificar: la asistencia figura como " + asistencia.getEstado();

        var yaEvaluado = evaluacionEstudianteRepository
                .findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(
                        evaluacion.getIdEvaluacion(), idEstudiante);

        Map<String, BigDecimal> puntajes = new LinkedHashMap<>();
        boolean precargado = false;

        Long idPosicion = estudiante.getPosicion() != null ? estudiante.getPosicion().getIdPosicion() : null;
        String posicion = estudiante.getPosicion() != null ? estudiante.getPosicion().getNombre() : null;

        if (yaEvaluado.isPresent()) {
            var ee = yaEvaluado.get();
            for (DetalleEvaluacion d : ee.getDetalles()) {
                puntajes.put(d.getCriterio().getNombre(), d.getPuntaje());
            }
        } else if (idEvaluacionPrevia != null && puedeEvaluarse) {
            for (Object[] fila : evaluacionEstudianteRepository
                    .puntajesDeEvaluacion(idEstudiante, idEvaluacionPrevia)) {
                puntajes.put((String) fila[0], (BigDecimal) fila[1]);
            }
            precargado = !puntajes.isEmpty();
        }

        Long idLesionActiva = lesionActivaPorEstudiante.get(idEstudiante);
        return new JugadorEvaluableResponse(
                idEstudiante, nombre,
                estudiante.getCategoria().getNombre(),
                idPosicion, posicion,
                asistencia == null ? null : asistencia.getEstado(),
                puntajes, precargado,
                idLesionActiva != null,
                idLesionActiva,
                puedeEvaluarse, motivo);
    }

    private Long buscarEvaluacionPrevia(SesionEntrenamiento sesion) {
        var previas = sesionRepository.findByCategoriaIdCategoriaAndFechaLessThanOrderByFechaDesc(
                sesion.getCategoria().getIdCategoria(), sesion.getFecha(),
                org.springframework.data.domain.PageRequest.of(0, 1));
        if (previas.isEmpty()) {
            return null;
        }
        return evaluacionRepository.findBySesionIdSesion(previas.get(0).getIdSesion())
                .map(EvaluacionDiaria::getIdEvaluacion)
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
     * @throws RecursoNoEncontradoException si la sesión no tiene evaluación
     *                                      abierta o la posición no existe
     * @throws IllegalArgumentException     si la evaluación ya fue
     *                                      finalizada, el estudiante no tiene
     *                                      asistencia habilitante, o un
     *                                      puntaje supera su máximo
     */
    @Auditado(accion = "EDITAR", entidad = "Estudiante", idSpel = "#p1.idEstudiante",
            descripcionSpel = "'editó estadísticas de estudiante #' + #p1.idEstudiante")
    @Transactional
    public void guardarJugador(Long idSesion, GuardarJugadorRequest request) {
        EvaluacionDiaria evaluacion = evaluacionRepository.findBySesionIdSesion(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La sesion " + idSesion + " no tiene evaluacion abierta"));

        if (evaluacion.estaFinalizada()) {
            throw new IllegalArgumentException(
                    "La evaluacion ya fue finalizada y no admite cambios");
        }

        Asistencia asistencia = asistenciaRepository
                .findBySesionIdSesionAndEstudianteIdEstudiante(idSesion, request.idEstudiante())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El estudiante no tiene asistencia registrada en esta sesion"));

        if (!asistencia.habilitaEvaluacion()) {
            throw new IllegalArgumentException(
                    "No se puede calificar a un estudiante cuya asistencia es "
                            + asistencia.getEstado());
        }

        var estudiante = asistencia.getEstudiante();

        EvaluacionEstudiante ee = evaluacionEstudianteRepository
                .findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(
                        evaluacion.getIdEvaluacion(), request.idEstudiante())
                .orElseGet(() -> EvaluacionEstudiante.builder()
                        .evaluacion(evaluacion)
                        .estudiante(estudiante)
                        // Regla 3: la categoría del día se congela al crear la
                        // fila, tomándola del estudiante en este momento.
                        .categoriaDia(estudiante.getCategoria())
                        .build());

        // El frontend siempre manda este campo: null es una instrucción
        // explícita de "quitar la posición", no "no tocar nada".
        if (request.idPosicionJugada() != null) {
            ee.setPosicionJugada(posicionRepository.findById(request.idPosicionJugada())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe la posicion " + request.idPosicionJugada())));
        } else {
            ee.setPosicionJugada(null);
        }

        lesionRepository.buscarActivaPorEstudiante(request.idEstudiante())
                .ifPresent(ee::setLesion);

        aplicarPuntajes(ee, request);
        evaluacionEstudianteRepository.save(ee);
    }

    private void aplicarPuntajes(EvaluacionEstudiante ee, GuardarJugadorRequest request) {
        Map<Long, CriterioEvaluacion> criterios = new HashMap<>();
        criterioRepository.findByActivoTrueOrderByIdCriterioAsc()
                .forEach(c -> criterios.put(c.getIdCriterio(), c));

        Map<Long, DetalleEvaluacion> existentes = new HashMap<>();
        ee.getDetalles().forEach(d -> existentes.put(d.getCriterio().getIdCriterio(), d));

        for (PuntajeCriterioRequest p : request.puntajes()) {
            CriterioEvaluacion criterio = criterios.get(p.idCriterio());
            if (criterio == null) {
                throw new IllegalArgumentException(
                        "El criterio " + p.idCriterio() + " no existe o esta desactivado");
            }
            if (p.puntaje().compareTo(BigDecimal.valueOf(criterio.getPuntajeMaximo())) > 0) {
                throw new IllegalArgumentException(
                        "El puntaje de " + criterio.getNombre() + " supera su maximo de "
                                + criterio.getPuntajeMaximo());
            }

            DetalleEvaluacion detalle = existentes.get(p.idCriterio());
            if (detalle == null) {
                detalle = DetalleEvaluacion.builder()
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

    /**
     * Cierra la evaluación. A partir de aquí no admite cambios.
     *
     * @param idSesion           identificador de la sesión
     * @param observacionGeneral observación general de la sesión; puede ser
     *                           {@code null}
     * @throws RecursoNoEncontradoException si la sesión no tiene evaluación
     *                                      abierta
     * @throws IllegalArgumentException     si ya estaba finalizada
     */
    @Auditado(accion = "EDITAR", entidad = "EvaluacionDiaria", idSpel = "#p0",
            descripcionSpel = "'finalizó la evaluación de la sesión #' + #p0")
    @Transactional
    public void finalizar(Long idSesion, String observacionGeneral) {
        EvaluacionDiaria evaluacion = evaluacionRepository.findBySesionIdSesion(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La sesion " + idSesion + " no tiene evaluacion abierta"));

        if (evaluacion.estaFinalizada()) {
            throw new IllegalArgumentException("La evaluacion ya estaba finalizada");
        }

        evaluacion.setObservacionGeneral(observacionGeneral);
        evaluacion.setEstado(EvaluacionDiaria.FINALIZADA);
        evaluacionRepository.save(evaluacion);
    }
}
