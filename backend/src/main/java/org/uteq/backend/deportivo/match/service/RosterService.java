package org.uteq.backend.deportivo.match.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.common.ia.AIFeedbackGenerator;
import org.uteq.backend.common.ia.AnonymousPlayerProfile;
import org.uteq.backend.deportivo.attendance.repository.AttendanceRepository;
import org.uteq.backend.deportivo.evaluation.repository.StudentEvaluationRepository;
import org.uteq.backend.deportivo.injury.repository.InjuryRepository;
import org.uteq.backend.deportivo.match.dto.RosterDtos.CalledUpPlayer;
import org.uteq.backend.deportivo.match.dto.RosterDtos.UnavailablePlayer;
import org.uteq.backend.deportivo.match.dto.RosterDtos.PerformanceWindow;
import org.uteq.backend.deportivo.match.entity.Match;
import org.uteq.backend.deportivo.match.repository.MatchRepository;
import org.uteq.backend.deportivo.session.repository.TrainingSessionRepository;

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
public class RosterService {
    private final MatchRepository matchRepository;
    private final StudentRepository estudianteRepository;
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final AttendanceRepository attendanceRepository;
    private final TrainingSessionRepository sesionRepository;
    private final InjuryRepository injuryRepository;
    private final AIFeedbackGenerator generadorFeedback;

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
    public int starterQuota() {
        return cantidadTitulares;
    }

    /**
     * Calcula la convocatoria sugerida para un partido por su identificador.
     *
     * @param idPartido identificador del partido
     * @return la convocatoria sugerida (no se guarda sola)
     * @throws ResourceNotFoundException si el partido no existe
     */
    @Transactional(readOnly = true)
    public Roster calculate(Long idPartido) {
        Match partido = matchRepository.findWithCategoryById(idPartido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el partido " + idPartido));
        return calculate(partido);
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
    public Roster calculate(Match partido) {
        Long idCategoria = partido.getCategoria().getIdCategoria();
        LocalDate hasta = partido.getFecha();
        LocalDate desde = hasta.minusWeeks(semanasRendimiento);

        long entrenamientos = sesionRepository
                .countByCategoryAndDateBetween(idCategoria, desde, hasta);
        PerformanceWindow ventana =
                new PerformanceWindow(semanasRendimiento, desde, hasta, entrenamientos);

        List<Student> plantel = estudianteRepository
                .findByCategory_IdCategoriaAndActiveTrueOrderByPerson_LastNameAsc(idCategoria);
        if (plantel.isEmpty()) {
            return new Roster(partido, ventana, List.of(), List.of(), List.of(),
                    Map.of(), Map.of(), entrenamientos);
        }

        List<Long> ids = plantel.stream().map(Student::getId).toList();
        Map<Long, BigDecimal> promedios = averagesOf(ids, desde, hasta);
        Map<Long, Long> presencias = presencesOf(ids, desde, hasta);
        Set<Long> lesionados = new HashSet<>(injuryRepository.injuredStudentIds());

        List<Student> convocables = new ArrayList<>();
        List<UnavailablePlayer> fuera = new ArrayList<>();
        for (Student e : plantel) {
            Long id = e.getId();
            if (lesionados.contains(id)) {
                fuera.add(new UnavailablePlayer(id, nameOf(e), "Lesión activa"));
            // Si la categoría no tuvo entrenamientos en la ventana, nadie pudo
            // asistir; castigar por eso a todo el plantel dejaría al entrenador
            // sin nadie a quien alinear.
            } else if (entrenamientos > 0 && presencias.getOrDefault(id, 0L) == 0L) {
                fuera.add(new UnavailablePlayer(id, nameOf(e),
                        "No entrenó en las últimas " + semanasRendimiento + " semanas"));
            } else {
                convocables.add(e);
            }
        }

        convocables.sort(byPerformance(promedios, presencias));

        Map<Long, CalledUpPlayer> titularPorPuesto = new LinkedHashMap<>();
        List<CalledUpPlayer> suplentes = new ArrayList<>();
        for (Student e : convocables) {
            Long idPosicion = e.getPosition() == null ? null : e.getPosition().getIdPosicion();
            boolean hayCupo = titularPorPuesto.size() < cantidadTitulares;
            boolean titulariza = idPosicion != null && hayCupo && !titularPorPuesto.containsKey(idPosicion);
            CalledUpPlayer fila =
                    toCalledUpPlayer(e, idPosicion, titulariza, promedios, presencias, entrenamientos);
            if (titulariza) {
                titularPorPuesto.put(idPosicion, fila);
            } else {
                suplentes.add(fila);
            }
        }

        return new Roster(partido, ventana,
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
    public AIFeedbackGenerator.FeedbackResult comment(
            List<CalledUpPlayer> titulares, String categoria) {
        List<AnonymousPlayerProfile> perfiles = new ArrayList<>();
        for (int i = 0; i < titulares.size(); i++) {
            CalledUpPlayer t = titulares.get(i);
            double promedio = t.promedio() == null ? 0.0 : t.promedio().doubleValue();
            perfiles.add(new AnonymousPlayerProfile(
                    "Jugador " + (i + 1), categoria, t.posicion(),
                    Map.of("Promedio acumulado", promedio,
                            "Entrenamientos asistidos", (double) t.presencias()),
                    Map.of(), null, false));
        }
        return generadorFeedback.generateLineupComment(perfiles);
    }

    // Promedio primero, presencias después, id al final. El desempate por id
    // no es cosmético: sin él, dos llamadas con los mismos datos podrían
    // devolver onces distintos.
    private Comparator<Student> byPerformance(Map<Long, BigDecimal> promedios,
                                                  Map<Long, Long> presencias) {
        return Comparator
                .comparing((Student e) -> promedios.getOrDefault(
                        e.getId(), BigDecimal.ZERO)).reversed()
                .thenComparing(Comparator.comparingLong(
                        (Student e) -> presencias.getOrDefault(e.getId(), 0L)).reversed())
                .thenComparing(Student::getId);
    }

    private Map<Long, BigDecimal> averagesOf(List<Long> ids, LocalDate desde, LocalDate hasta) {
        Map<Long, BigDecimal> promedios = new HashMap<>();
        for (Object[] fila : studentEvaluationRepository.averageInWindow(ids, desde, hasta)) {
            if (fila[1] == null) {
                continue;
            }
            promedios.put((Long) fila[0], BigDecimal.valueOf(((Number) fila[1]).doubleValue())
                    .setScale(1, RoundingMode.HALF_UP));
        }
        return promedios;
    }

    private Map<Long, Long> presencesOf(List<Long> ids, LocalDate desde, LocalDate hasta) {
        Map<Long, Long> presencias = new HashMap<>();
        for (Object[] fila : attendanceRepository.presencesInWindow(ids, desde, hasta)) {
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
    public CalledUpPlayer toCalledUpPlayer(Student e, Long idPosicion, boolean titular,
                                       Map<Long, BigDecimal> promedios, Map<Long, Long> presencias,
                                       long entrenamientos) {
        String abreviatura = null;
        if (idPosicion != null) {
            var nominal = e.getPosition();
            abreviatura = nominal != null && idPosicion.equals(nominal.getIdPosicion())
                    ? nominal.getAbreviatura() : null;
        }
        return new CalledUpPlayer(
                e.getId(), nameOf(e), abreviatura, idPosicion, titular,
                promedios.get(e.getId()),
                presencias.getOrDefault(e.getId(), 0L),
                entrenamientos);
    }

    /**
     * Nombre completo de un estudiante ({@code "Nombre Apellido"}).
     *
     * @param e estudiante
     * @return el nombre completo
     */
    public static String nameOf(Student e) {
        return e.getPerson().getName() + " " + e.getPerson().getLastName();
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
    public record Roster(
            Match partido,
            PerformanceWindow ventana,
            List<CalledUpPlayer> titulares,
            List<CalledUpPlayer> suplentes,
            List<UnavailablePlayer> noConvocables,
            Map<Long, BigDecimal> promedios,
            Map<Long, Long> presencias,
            long entrenamientos
    ) {}
}
