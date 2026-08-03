package org.uteq.backend.deportivo.evaluacion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos.*;
import org.uteq.backend.deportivo.evaluacion.entity.*;
import org.uteq.backend.deportivo.evaluacion.repository.*;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.math.BigDecimal;
import java.util.*;

/**
 * Evaluacion diaria del entrenador.
 *
 * <p>Concentra las tres reglas que el documento del modulo describe y que no
 * son evidentes desde el esquema:
 *
 * <ol>
 *   <li><b>Sin asistencia no hay calificacion.</b> Solo se puede evaluar a
 *       quien marco PRESENTE o TARDE. No es una comodidad de interfaz: evita
 *       que queden registros de desempeno de alguien que no fue.</li>
 *   <li><b>Cada dia arranca con los valores del anterior.</b> El entrenador
 *       ajusta lo que cambio en vez de recalificar treinta jugadores desde
 *       cero. Los valores heredados se marcan como precargados para que sepa
 *       cuales todavia no confirmo.</li>
 *   <li><b>Se guarda la categoria del dia.</b> Si un jugador cambia de
 *       categoria en marzo, sus evaluaciones de febrero siguen diciendo
 *       SUB-12, que es donde realmente estaba.</li>
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

    /**
     * Abre la pantalla de evaluacion de una sesion. Si todavia no existe la
     * cabecera, la crea en BORRADOR.
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
        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());

        // Evaluacion previa de la misma categoria: fuente de la precarga.
        Long idEvaluacionPrevia = buscarEvaluacionPrevia(sesion);

        List<JugadorEvaluableResponse> jugadores = new ArrayList<>();
        for (Asistencia asistencia : asistenciaRepository.findBySesionIdSesion(idSesion)) {
            jugadores.add(construirJugador(
                    asistencia, evaluacion, idEvaluacionPrevia, lesionados));
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

    private JugadorEvaluableResponse construirJugador(Asistencia asistencia,
                                                      EvaluacionDiaria evaluacion,
                                                      Long idEvaluacionPrevia,
                                                      Set<Long> lesionados) {
        var estudiante = asistencia.getEstudiante();
        Long idEstudiante = estudiante.getIdEstudiante();
        var persona = estudiante.getPersona();
        String nombre = persona.getNombre() + " " + persona.getApellido();

        boolean puedeEvaluarse = asistencia.habilitaEvaluacion();
        String motivo = puedeEvaluarse ? null
                : "No se puede calificar: la asistencia figura como " + asistencia.getEstado();

        // Lo ya guardado hoy tiene prioridad sobre cualquier precarga.
        var yaEvaluado = evaluacionEstudianteRepository
                .findByEvaluacionIdEvaluacionAndEstudianteIdEstudiante(
                        evaluacion.getIdEvaluacion(), idEstudiante);

        Map<String, BigDecimal> puntajes = new LinkedHashMap<>();
        boolean precargado = false;
        Long idPosicion = null;
        String posicion = null;

        if (yaEvaluado.isPresent()) {
            var ee = yaEvaluado.get();
            for (DetalleEvaluacion d : ee.getDetalles()) {
                puntajes.put(d.getCriterio().getNombre(), d.getPuntaje());
            }
            if (ee.getPosicionJugada() != null) {
                idPosicion = ee.getPosicionJugada().getIdPosicion();
                posicion = ee.getPosicionJugada().getNombre();
            }
        } else if (idEvaluacionPrevia != null && puedeEvaluarse) {
            for (Object[] fila : evaluacionEstudianteRepository
                    .puntajesDeEvaluacion(idEstudiante, idEvaluacionPrevia)) {
                puntajes.put((String) fila[0], (BigDecimal) fila[1]);
            }
            precargado = !puntajes.isEmpty();
        }

        return new JugadorEvaluableResponse(
                idEstudiante, nombre,
                estudiante.getCategoria().getNombre(),
                idPosicion, posicion,
                puntajes, precargado,
                lesionados.contains(idEstudiante),
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
     * Guarda los puntajes de un jugador. Es la operacion del autoguardado, asi
     * que se invoca muchas veces por sesion y debe ser idempotente: vuelve a
     * escribir sobre lo que ya habia en vez de acumular filas.
     */
    @Transactional
    public void guardarJugador(Long idSesion, GuardarJugadorRequest request) {
        EvaluacionDiaria evaluacion = evaluacionRepository.findBySesionIdSesion(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La sesion " + idSesion + " no tiene evaluacion abierta"));

        if (evaluacion.estaFinalizada()) {
            throw new IllegalArgumentException(
                    "La evaluacion ya fue finalizada y no admite cambios");
        }

        // Regla 1: sin asistencia habilitante no se guarda nada.
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
                        // Regla 3: la categoria del dia se congela al crear la
                        // fila, tomandola del estudiante en este momento.
                        .categoriaDia(estudiante.getCategoria())
                        .build());

        if (request.idPosicionJugada() != null) {
            ee.setPosicionJugada(posicionRepository.findById(request.idPosicionJugada())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe la posicion " + request.idPosicionJugada())));
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

    /** Cierra la evaluacion. A partir de aqui no admite cambios. */
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
