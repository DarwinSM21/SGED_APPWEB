package org.uteq.backend.deportivo.evaluacion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.common.ia.GeneradorFeedbackIA;
import org.uteq.backend.common.ia.PerfilJugadorAnonimo;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos.*;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Sugerencia de alineacion.
 *
 * <p><b>La IA no elige a los jugadores.</b> La seleccion y el orden salen de
 * una regla explicita y auditable: se excluye a los lesionados, se ordena por
 * promedio acumulado y se cortan los primeros N. Cualquiera puede reproducir
 * el resultado a mano con los mismos datos, y el entrenador puede ajustar la
 * sugerencia.
 *
 * <p>El modelo de lenguaje solo redacta un comentario sobre una alineacion ya
 * decidida, y solo cuando se le pide explicitamente ({@link #feedback}): dejar
 * que un modelo generativo decida quien juega haria la decision inauditable,
 * irreproducible y dificilmente explicable a un padre de familia que pregunte
 * por que su hijo quedo fuera; y llamarlo automaticamente en cada apertura de
 * pantalla gastaria cuota de un servicio externo sin que nadie lo pidiera.
 */
@Service
@RequiredArgsConstructor
public class PlantillaService {

    private final SesionEntrenamientoRepository sesionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    private final LesionRepository lesionRepository;
    private final GeneradorFeedbackIA generadorFeedback;

    @Value("${plantilla.titulares:11}")
    private int cantidadTitulares;

    @Transactional(readOnly = true)
    public PlantillaResponse sugerir(Long idSesion) {
        var r = calcular(idSesion);
        return new PlantillaResponse(idSesion, r.categoria(), r.titulares(), r.suplentes(), r.excluidos());
    }

    /** Recalcula la misma alineacion y, sobre ella, pide el comentario a la IA. */
    @Transactional(readOnly = true)
    public FeedbackPlantillaResponse feedback(Long idSesion) {
        var r = calcular(idSesion);
        if (r.titulares().isEmpty()) {
            return new FeedbackPlantillaResponse(null, false, "No hay alineacion que comentar");
        }
        var resultado = comentarDe(r.titulares(), r.categoria());
        return new FeedbackPlantillaResponse(resultado.texto(), resultado.disponible(), resultado.motivo());
    }

    private ResultadoCalculo calcular(Long idSesion) {
        var sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sesion " + idSesion));

        String categoria = sesion.getCategoria().getNombre();

        // Solo entran quienes asistieron a la sesion.
        var asistencias = asistenciaRepository.listarHabilitadosParaEvaluar(idSesion);
        if (asistencias.isEmpty()) {
            return new ResultadoCalculo(categoria, List.of(), List.of(), List.of());
        }

        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());

        List<Estudiante> disponibles = new ArrayList<>();
        List<Long> excluidos = new ArrayList<>();
        for (var a : asistencias) {
            Long id = a.getEstudiante().getIdEstudiante();
            if (lesionados.contains(id)) {
                excluidos.add(id);
            } else {
                disponibles.add(a.getEstudiante());
            }
        }

        Map<Long, BigDecimal> promedios = promediosDe(disponibles);

        // Orden deterministico: promedio descendente y, ante empate, por id.
        // El desempate por id evita que dos llamadas con los mismos datos
        // devuelvan alineaciones distintas.
        disponibles.sort(Comparator
                .comparing((Estudiante e) -> promedios.getOrDefault(
                        e.getIdEstudiante(), BigDecimal.ZERO)).reversed()
                .thenComparing(Estudiante::getIdEstudiante));

        List<JugadorPlantillaResponse> titulares = new ArrayList<>();
        List<JugadorPlantillaResponse> suplentes = new ArrayList<>();
        for (int i = 0; i < disponibles.size(); i++) {
            var jugador = aResponse(disponibles.get(i), promedios);
            if (i < cantidadTitulares) {
                titulares.add(jugador);
            } else {
                suplentes.add(jugador);
            }
        }

        return new ResultadoCalculo(categoria, titulares, suplentes, excluidos);
    }

    private Map<Long, BigDecimal> promediosDe(List<Estudiante> estudiantes) {
        if (estudiantes.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = estudiantes.stream().map(Estudiante::getIdEstudiante).toList();
        Map<Long, BigDecimal> promedios = new HashMap<>();
        for (Object[] fila : evaluacionEstudianteRepository.promedioGeneralPorEstudiante(ids)) {
            Long id = (Long) fila[0];
            Double promedio = fila[1] == null ? 0.0 : ((Number) fila[1]).doubleValue();
            promedios.put(id, BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP));
        }
        return promedios;
    }

    private JugadorPlantillaResponse aResponse(Estudiante e, Map<Long, BigDecimal> promedios) {
        var p = e.getPersona();
        // Posicion nominal del estudiante (academico.estudiantes.id_posicion),
        // no la jugada en un dia concreto: es la mejor aproximacion disponible
        // sin pedirle al entrenador que la tag ee cada vez que evalua.
        String posicion = e.getPosicion() != null ? e.getPosicion().getAbreviatura() : null;
        return new JugadorPlantillaResponse(
                e.getIdEstudiante(),
                p.getNombre() + " " + p.getApellido(),
                posicion,
                promedios.getOrDefault(e.getIdEstudiante(), BigDecimal.ZERO));
    }

    /**
     * Pide el comentario al modelo. Solo se le envian datos seudonimizados:
     * "Jugador 1", su categoria y su promedio. Ningun nombre sale del sistema.
     */
    private GeneradorFeedbackIA.ResultadoFeedback comentarDe(
            List<JugadorPlantillaResponse> titulares, String categoria) {

        List<PerfilJugadorAnonimo> perfiles = new ArrayList<>();
        for (int i = 0; i < titulares.size(); i++) {
            var t = titulares.get(i);
            perfiles.add(new PerfilJugadorAnonimo(
                    "Jugador " + (i + 1),
                    categoria,
                    t.posicion(),
                    Map.of("Promedio acumulado", t.promedioAcumulado().doubleValue()),
                    Map.of(),
                    null,
                    false));
        }
        return generadorFeedback.generarComentarioPlantilla(perfiles);
    }

    private record ResultadoCalculo(
            String categoria,
            List<JugadorPlantillaResponse> titulares,
            List<JugadorPlantillaResponse> suplentes,
            List<Long> excluidos
    ) {}
}
