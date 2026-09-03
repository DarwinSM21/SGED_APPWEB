package org.uteq.backend.deportivo.partido.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.common.ia.GeneradorFeedbackIA;
import org.uteq.backend.common.ia.PerfilJugadorAnonimo;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.JugadorConvocado;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.NoConvocable;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.VentanaRendimiento;
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.partido.repository.PartidoRepository;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sugerencia de once para un partido. <b>La IA no elige a los jugadores.</b>
 * La selección sale de una regla explícita y reproducible a mano:
 * <ol>
 *   <li>El universo es el plantel activo de la categoría que juega.</li>
 *   <li>Queda fuera quien arrastra una lesión activa y quien no pisó un solo
 *       entrenamiento en la ventana; los dos se muestran con el motivo.</li>
 *   <li>Se ordena por el promedio de las últimas semanas (no el histórico),
 *       desempatando por presencias y después por id, para que dos llamadas
 *       con los mismos datos devuelvan lo mismo.</li>
 *   <li>Se titulariza al mejor de cada posición nominal, no a los once
 *       mejores promedios (eso podía sugerir dos porteros).</li>
 * </ol>
 *
 * <p>El modelo de lenguaje solo redacta un comentario sobre un once ya
 * decidido, y solo cuando se le pide.
 */
@Service
@RequiredArgsConstructor
public class ConvocatoriaService {
    private final PartidoRepository partidoRepository;
    private final EstudianteRepository estudianteRepository;
    private final EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final SesionEntrenamientoRepository sesionRepository;
    private final LesionRepository lesionRepository;
    private final GeneradorFeedbackIA generadorFeedback;

    /** Cupo de titulares del once. */
    @Value("${plantilla.titulares:11}")
    private int cantidadTitulares;

    /** Cuántas semanas hacia atrás se miran. Cuatro es un mes de entrenamientos. */
    @Value("${plantilla.semanas-rendimiento:4}")
    private int semanasRendimiento;

    /**
     * Cupo de titulares configurado.
     *
     * @return el número de titulares del once
     */
    public int cupoTitulares() {
        return cantidadTitulares;
    }

    /**
     * Calcula la convocatoria sugerida para un partido por su identificador.
     *
     * @param idPartido identificador del partido
     * @return la convocatoria sugerida (no se guarda sola)
     * @throws RecursoNoEncontradoException si el partido no existe
     */
    @Transactional(readOnly = true)
    public Convocatoria calcular(Long idPartido) {
        Partido partido = partidoRepository.findWithCategoriaByIdPartido(idPartido)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el partido " + idPartido));
        return calcular(partido);
    }

    /**
     * Calcula la convocatoria sugerida para un partido ya cargado. Es la
     * <em>sugerencia</em>: no se guarda sola, para no convertir una
     * recomendación en hecho histórico sin que nadie lo decida.
     *
     * @param partido partido para el que se calcula
     * @return titulares por puesto, suplentes, no convocables con su motivo,
     *         y los promedios y presencias ya consultados
     */
    @Transactional(readOnly = true)
    public Convocatoria calcular(Partido partido) {
        Long idCategoria = partido.getCategoria().getIdCategoria();
        LocalDate hasta = partido.getFecha();
        LocalDate desde = hasta.minusWeeks(semanasRendimiento);

        long entrenamientos = sesionRepository
                .countByCategoriaIdCategoriaAndFechaBetween(idCategoria, desde, hasta);
        VentanaRendimiento ventana =
                new VentanaRendimiento(semanasRendimiento, desde, hasta, entrenamientos);

        List<Estudiante> plantel = estudianteRepository
                .findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(idCategoria);
        if (plantel.isEmpty()) {
            return new Convocatoria(partido, ventana, List.of(), List.of(), List.of(),
                    Map.of(), Map.of(), entrenamientos);
        }

        List<Long> ids = plantel.stream().map(Estudiante::getIdEstudiante).toList();
        Map<Long, BigDecimal> promedios = promediosDe(ids, desde, hasta);
        Map<Long, Long> presencias = presenciasDe(ids, desde, hasta);
        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());

        List<Estudiante> convocables = new ArrayList<>();
        List<NoConvocable> fuera = new ArrayList<>();
        for (Estudiante e : plantel) {
            Long id = e.getIdEstudiante();
            if (lesionados.contains(id)) {
                fuera.add(new NoConvocable(id, nombreDe(e), "Lesión activa"));
            // Si la categoría no tuvo entrenamientos en la ventana, nadie pudo
            // asistir; castigar por eso a todo el plantel dejaría al entrenador
            // sin nadie a quien alinear.
            } else if (entrenamientos > 0 && presencias.getOrDefault(id, 0L) == 0L) {
                fuera.add(new NoConvocable(id, nombreDe(e),
                        "No entrenó en las últimas " + semanasRendimiento + " semanas"));
            } else {
                convocables.add(e);
            }
        }

        convocables.sort(porRendimiento(promedios, presencias));

        Map<Long, JugadorConvocado> titularPorPuesto = new LinkedHashMap<>();
        List<JugadorConvocado> suplentes = new ArrayList<>();
        for (Estudiante e : convocables) {
            Long idPosicion = e.getPosicion() == null ? null : e.getPosicion().getIdPosicion();
            boolean hayCupo = titularPorPuesto.size() < cantidadTitulares;
            boolean titulariza = idPosicion != null && hayCupo && !titularPorPuesto.containsKey(idPosicion);
            JugadorConvocado fila =
                    aConvocado(e, idPosicion, titulariza, promedios, presencias, entrenamientos);
            if (titulariza) {
                titularPorPuesto.put(idPosicion, fila);
            } else {
                suplentes.add(fila);
            }
        }

        return new Convocatoria(partido, ventana,
                new ArrayList<>(titularPorPuesto.values()), suplentes, fuera,
                promedios, presencias, entrenamientos);
    }

    /**
     * Comentario del modelo sobre un once ya decidido. Se pide a demanda.
     * Solo se envían datos seudonimizados ("Jugador 1", su categoría, su
     * puesto y sus números): ningún nombre sale del sistema.
     *
     * @param titulares once sobre el que comentar
     * @param categoria categoría del equipo
     * @return el comentario generado, con su disponibilidad y motivo
     */
    public GeneradorFeedbackIA.ResultadoFeedback comentar(
            List<JugadorConvocado> titulares, String categoria) {
        List<PerfilJugadorAnonimo> perfiles = new ArrayList<>();
        for (int i = 0; i < titulares.size(); i++) {
            JugadorConvocado t = titulares.get(i);
            double promedio = t.promedio() == null ? 0.0 : t.promedio().doubleValue();
            perfiles.add(new PerfilJugadorAnonimo(
                    "Jugador " + (i + 1), categoria, t.posicion(),
                    Map.of("Promedio acumulado", promedio,
                            "Entrenamientos asistidos", (double) t.presencias()),
                    Map.of(), null, false));
        }
        return generadorFeedback.generarComentarioPlantilla(perfiles);
    }

    // Promedio primero, presencias después, id al final. El desempate por id
    // no es cosmético: sin él, dos llamadas con los mismos datos podrían
    // devolver onces distintos.
    private Comparator<Estudiante> porRendimiento(Map<Long, BigDecimal> promedios,
                                                  Map<Long, Long> presencias) {
        return Comparator
                .comparing((Estudiante e) -> promedios.getOrDefault(
                        e.getIdEstudiante(), BigDecimal.ZERO)).reversed()
                .thenComparing(Comparator.comparingLong(
                        (Estudiante e) -> presencias.getOrDefault(e.getIdEstudiante(), 0L)).reversed())
                .thenComparing(Estudiante::getIdEstudiante);
    }

    private Map<Long, BigDecimal> promediosDe(List<Long> ids, LocalDate desde, LocalDate hasta) {
        Map<Long, BigDecimal> promedios = new HashMap<>();
        for (Object[] fila : evaluacionEstudianteRepository.promedioEnVentana(ids, desde, hasta)) {
            if (fila[1] == null) {
                continue;
            }
            promedios.put((Long) fila[0], BigDecimal.valueOf(((Number) fila[1]).doubleValue())
                    .setScale(1, RoundingMode.HALF_UP));
        }
        return promedios;
    }

    private Map<Long, Long> presenciasDe(List<Long> ids, LocalDate desde, LocalDate hasta) {
        Map<Long, Long> presencias = new HashMap<>();
        for (Object[] fila : asistenciaRepository.presenciasEnVentana(ids, desde, hasta)) {
            presencias.put((Long) fila[0], ((Number) fila[1]).longValue());
        }
        return presencias;
    }

    /**
     * Construye la fila de un jugador convocado.
     *
     * @param e              estudiante
     * @param idPosicion     puesto de <em>ese</em> partido, que no tiene por
     *                       qué ser la posición nominal del estudiante
     * @param titular        {@code true} si entra como titular
     * @param promedios      promedios por estudiante ya consultados
     * @param presencias     presencias por estudiante ya consultadas
     * @param entrenamientos entrenamientos de la categoría en la ventana
     * @return la fila del jugador ({@code promedio} es {@code null}, no
     *         {@code 0.0}, si no lo evaluaron)
     */
    public JugadorConvocado aConvocado(Estudiante e, Long idPosicion, boolean titular,
                                       Map<Long, BigDecimal> promedios, Map<Long, Long> presencias,
                                       long entrenamientos) {
        String abreviatura = null;
        if (idPosicion != null) {
            var nominal = e.getPosicion();
            abreviatura = nominal != null && idPosicion.equals(nominal.getIdPosicion())
                    ? nominal.getAbreviatura() : null;
        }
        return new JugadorConvocado(
                e.getIdEstudiante(), nombreDe(e), abreviatura, idPosicion, titular,
                promedios.get(e.getIdEstudiante()),
                presencias.getOrDefault(e.getIdEstudiante(), 0L),
                entrenamientos);
    }

    /**
     * Nombre completo de un estudiante ({@code "Nombre Apellido"}).
     *
     * @param e estudiante
     * @return el nombre completo
     */
    public static String nombreDe(Estudiante e) {
        return e.getPersona().getNombre() + " " + e.getPersona().getApellido();
    }

    /**
     * Resultado del cálculo de convocatoria. Lleva además los promedios y
     * presencias ya consultados para que quien tenga que rearmar filas no
     * vuelva a golpear la base con las mismas dos consultas.
     *
     * @param partido        partido para el que se calculó
     * @param ventana        ventana de rendimiento evaluada
     * @param titulares      once sugerido
     * @param suplentes      convocables que no entraron al once
     * @param noConvocables  jugadores fuera, con su motivo
     * @param promedios      promedio en la ventana por estudiante
     * @param presencias     presencias en la ventana por estudiante
     * @param entrenamientos entrenamientos de la categoría en la ventana
     */
    public record Convocatoria(
            Partido partido,
            VentanaRendimiento ventana,
            List<JugadorConvocado> titulares,
            List<JugadorConvocado> suplentes,
            List<NoConvocable> noConvocables,
            Map<Long, BigDecimal> promedios,
            Map<Long, Long> presencias,
            long entrenamientos
    ) {}
}
