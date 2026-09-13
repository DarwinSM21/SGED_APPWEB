package org.uteq.backend.deportivo.match.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.evaluation.entity.Lineup;
import org.uteq.backend.deportivo.evaluation.entity.LineupPlayer;
import org.uteq.backend.deportivo.evaluation.repository.LineupRepository;
import org.uteq.backend.deportivo.injury.repository.InjuryRepository;
import org.uteq.backend.deportivo.match.dto.LineupDtos.SaveLineupRequest;
import org.uteq.backend.deportivo.match.dto.LineupDtos.PlayerOnField;
import org.uteq.backend.deportivo.match.dto.RosterDtos.LineupResponse;
import org.uteq.backend.deportivo.match.dto.RosterDtos.LineupFeedbackResponse;
import org.uteq.backend.deportivo.match.dto.RosterDtos.CalledUpPlayer;
import org.uteq.backend.deportivo.match.entity.Match;
import org.uteq.backend.deportivo.match.service.RosterService.Roster;
import org.uteq.backend.deportivo.position.entity.Position;
import org.uteq.backend.deportivo.position.repository.PositionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El once con el que se jugó un partido. Convive con
 * {@link RosterService}, que calcula la <b>sugerencia</b>: si el
 * entrenador guardó una alineación se devuelve esa, si no, la sugerida. Así
 * el historial sale gratis sin duplicar el cálculo ni congelar sugerencias
 * que nadie usó. La sugerencia no se guarda nunca por su cuenta.
 */
@Service
@RequiredArgsConstructor
public class LineupService {
    private final LineupRepository lineupRepository;
    private final StudentRepository estudianteRepository;
    private final PositionRepository positionRepository;
    private final InjuryRepository injuryRepository;
    private final RosterService rosterService;
    private final MatchService matchService;

    @Value("${plantilla.titulares:11}")
    private int cupoTitulares;

    /**
     * Alineación de un partido: la guardada si existe, o la sugerencia del
     * sistema.
     *
     * @param idPartido identificador del partido
     * @return el once, con la bandera {@code guardada} y los jugadores
     *         disponibles para cambios
     * @throws ResourceNotFoundException si el partido no existe
     */
    @Transactional(readOnly = true)
    public LineupResponse view(Long idPartido) {
        Roster convocatoria = rosterService.calculate(idPartido);
        return lineupRepository.findByMatch_Id(idPartido)
                .map(a -> fromSaved(convocatoria, a))
                .orElseGet(() -> fromSuggestion(convocatoria));
    }

    /**
     * Guarda el once que el entrenador decidió. Reemplaza por completo el
     * anterior en vez de mezclar. Se valida <em>todo</em> antes de tocar la
     * base: si algo falla, la alineación anterior queda intacta.
     *
     * @param idPartido identificador del partido
     * @param request   jugadores, su puesto, valoración y observación
     * @return el once guardado
     * @throws ResourceNotFoundException si el partido, un estudiante o una
     *                                      posición no existen
     * @throws IllegalArgumentException     si el partido está cerrado, un
     *                                      jugador aparece dos veces, no es
     *                                      de la categoría, arrastra una
     *                                      lesión activa, dos titulares
     *                                      comparten puesto, o se superan los
     *                                      titulares permitidos
     */
    @Transactional
    public LineupResponse save(Long idPartido, SaveLineupRequest request) {
        Roster convocatoria = rosterService.calculate(idPartido);
        Match partido = convocatoria.match();
        matchService.requireOpen(partido);
        Long idCategoria = partido.getCategoria().getCategoryId();

        Set<Long> lesionados = new HashSet<>(injuryRepository.injuredStudentIds());
        Set<Long> vistos = new LinkedHashSet<>();
        Map<Long, Long> puestoOcupado = new HashMap<>();
        int titulares = 0;

        List<LineupPlayer> nuevos = new ArrayList<>();
        for (PlayerOnField j : request.players()) {
            if (!vistos.add(j.studentId())) {
                throw new IllegalArgumentException("Un jugador no puede estar dos veces en la alineación");
            }

            Student estudiante = estudianteRepository.findByIdAndActiveTrue(j.studentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estudiante no encontrado o inactivo: " + j.studentId()));
            String nombre = RosterService.nameOf(estudiante);

            Long categoriaDelJugador = estudiante.getCategory() == null
                    ? null : estudiante.getCategory().getCategoryId();
            if (!idCategoria.equals(categoriaDelJugador)) {
                throw new IllegalArgumentException(
                        nombre + " no pertenece a la categoría " + partido.getCategoria().getNombre());
            }

            if (lesionados.contains(j.studentId())) {
                throw new IllegalArgumentException(nombre + " arrastra una lesión activa y no puede jugar");
            }

            boolean esTitular = Boolean.TRUE.equals(j.starter());
            Position posicion = null;
            if (j.positionId() != null) {
                posicion = positionRepository.findById(j.positionId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Position no encontrada: " + j.positionId()));
                if (esTitular && puestoOcupado.put(j.positionId(), j.studentId()) != null) {
                    throw new IllegalArgumentException(
                            "Dos titulares no pueden ocupar el puesto " + posicion.getAbreviatura());
                }
            }
            if (esTitular && ++titulares > cupoTitulares) {
                throw new IllegalArgumentException(
                        "No podés poner más de " + cupoTitulares + " titulares en la cancha");
            }

            nuevos.add(LineupPlayer.builder()
                    .estudiante(estudiante)
                    .posicion(posicion)
                    .titular(esTitular)
                    .build());
        }

        Lineup alineacion = lineupRepository.findByMatch_Id(idPartido)
                .orElseGet(() -> Lineup.builder().partido(partido).build());
        alineacion.setValoracion(request.rating());
        alineacion.setObservacion(request.note());

        if (!alineacion.getJugadores().isEmpty()) {
            // Vaciar y volver a llenar en el mismo flush hace que Hibernate
            // intente insertar antes de borrar y choque contra
            // uq_alineacion_jugador. El saveAndFlush intermedio fuerza que los
            // DELETE salgan primero.
            alineacion.getJugadores().clear();
            lineupRepository.saveAndFlush(alineacion);
        }
        for (LineupPlayer j : nuevos) {
            j.setAlineacion(alineacion);
            alineacion.getJugadores().add(j);
        }
        lineupRepository.save(alineacion);

        return view(idPartido);
    }

    /**
     * Descarta los cambios manuales y vuelve a la sugerencia del sistema.
     *
     * @param idPartido identificador del partido
     * @return la sugerencia recalculada
     * @throws ResourceNotFoundException si el partido no existe
     * @throws IllegalArgumentException     si el partido está cerrado
     */
    @Transactional
    public LineupResponse reset(Long idPartido) {
        matchService.requireOpen(rosterService.calculate(idPartido).match());
        lineupRepository.findByMatch_Id(idPartido).ifPresent(lineupRepository::delete);
        return view(idPartido);
    }

    /**
     * Comentario de IA sobre el once que hoy está en pantalla.
     *
     * @param idPartido identificador del partido
     * @return el comentario generado, o un texto por defecto si no hay
     *         alineación
     * @throws ResourceNotFoundException si el partido no existe
     */
    @Transactional(readOnly = true)
    public LineupFeedbackResponse feedback(Long idPartido) {
        LineupResponse actual = view(idPartido);
        if (actual.starters().isEmpty()) {
            return new LineupFeedbackResponse(null, false, "No hay alineación que comentar");
        }
        var resultado = rosterService.comment(actual.starters(), actual.category());
        return new LineupFeedbackResponse(
                resultado.text(), resultado.isAvailable(), resultado.reason());
    }

    private LineupResponse fromSaved(Roster c, Lineup a) {
        Map<Long, BigDecimal> promedios = c.averages();
        Map<Long, Long> presencias = c.attendanceRecords();

        List<CalledUpPlayer> titulares = new ArrayList<>();
        List<CalledUpPlayer> suplentes = new ArrayList<>();
        Set<Long> yaEstan = new HashSet<>();

        for (LineupPlayer j : a.getJugadores()) {
            Student e = j.getEstudiante();
            yaEstan.add(e.getId());
            boolean titular = Boolean.TRUE.equals(j.getTitular());
            Long idPosicion = j.getPosicion() == null ? null : j.getPosicion().getIdPosicion();
            CalledUpPlayer fila = rosterService.toCalledUpPlayer(
                    e, idPosicion, titular, promedios, presencias, c.trainingSessions());

            if (j.getPosicion() != null) {
                fila = new CalledUpPlayer(fila.studentId(), fila.fullName(),
                        j.getPosicion().getAbreviatura(), idPosicion, titular,
                        fila.average(), fila.attendanceRecords(), fila.trainingSessions());
            }
            (titular ? titulares : suplentes).add(fila);
        }

        return response(c, true, a.getValoracion(), a.getObservacion(),
                titulares, suplentes, yaEstan);
    }

    private LineupResponse fromSuggestion(Roster c) {
        Set<Long> yaEstan = new HashSet<>();
        c.starters().forEach(t -> yaEstan.add(t.studentId()));
        c.substitutes().forEach(s -> yaEstan.add(s.studentId()));
        return response(c, false, null, null, c.starters(), c.substitutes(), yaEstan);
    }

    private LineupResponse response(Roster c, boolean guardada, Short valoracion,
                                         String observacion, List<CalledUpPlayer> titulares,
                                         List<CalledUpPlayer> suplentes, Set<Long> yaEstan) {
        List<CalledUpPlayer> disponibles = new ArrayList<>();
        for (CalledUpPlayer j : c.starters()) {
            if (!yaEstan.contains(j.studentId())) {
                disponibles.add(withoutStarterFlag(j));
            }
        }
        for (CalledUpPlayer j : c.substitutes()) {
            if (!yaEstan.contains(j.studentId())) {
                disponibles.add(withoutStarterFlag(j));
            }
        }

        Match p = c.match();
        return new LineupResponse(
                p.getIdPartido(), p.getCategoria().getCategoryId(), p.getCategoria().getNombre(),
                p.getFecha(), guardada, valoracion, observacion, c.window(),
                titulares, suplentes, disponibles, c.notCallable(), cupoTitulares,
                p.isClosed());
    }

    private CalledUpPlayer withoutStarterFlag(CalledUpPlayer j) {
        return new CalledUpPlayer(j.studentId(), j.fullName(), j.position(),
                j.positionId(), false, j.average(), j.attendanceRecords(), j.trainingSessions());
    }
}
