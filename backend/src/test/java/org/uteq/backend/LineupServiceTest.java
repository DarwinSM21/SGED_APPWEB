package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.evaluation.entity.Lineup;
import org.uteq.backend.deportivo.evaluation.repository.LineupRepository;
import org.uteq.backend.deportivo.injury.repository.InjuryRepository;
import org.uteq.backend.deportivo.match.dto.LineupDtos.SaveLineupRequest;
import org.uteq.backend.deportivo.match.dto.LineupDtos.PlayerOnField;
import org.uteq.backend.deportivo.match.dto.RosterDtos.PerformanceWindow;
import org.uteq.backend.deportivo.match.entity.Match;
import org.uteq.backend.deportivo.match.service.LineupService;
import org.uteq.backend.deportivo.match.service.RosterService;
import org.uteq.backend.deportivo.match.service.MatchService;
import org.uteq.backend.deportivo.match.service.RosterService.Roster;
import org.uteq.backend.deportivo.position.entity.Position;
import org.uteq.backend.deportivo.position.repository.PositionRepository;
import org.uteq.backend.seguridad.person.entity.Person;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LineupServiceTest {
    @Mock private LineupRepository lineupRepository;
    @Mock private StudentRepository estudianteRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private InjuryRepository injuryRepository;
    @Mock private RosterService rosterService;
    @Mock private MatchService matchService;

    @InjectMocks private LineupService servicio;

    private static final Long ID_PARTIDO = 7L;
    private static final Long ID_CATEGORIA = 3L;

    private final Category categoria = Category.builder()
            .idCategoria(ID_CATEGORIA).nombre("SUB-14").activo(true).build();
    private final Category otraCategoria = Category.builder()
            .idCategoria(99L).nombre("SUB-17").activo(true).build();

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(servicio, "cupoTitulares", 11);
    }

    private Match partido() {
        return Match.builder().idPartido(ID_PARTIDO).categoria(categoria)
                .fecha(LocalDate.of(2026, 8, 29)).build();
    }

    private Roster convocatoriaVacia() {
        return new Roster(partido(),
                new PerformanceWindow(4, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 29), 8L),
                List.of(), List.of(), List.of(), Map.of(), Map.of(), 8L);
    }

    private Student jugador(long id, Category suCategoria) {
        return Student.builder()
                .id(id).active(true).category(suCategoria)
                .person(Person.builder().id(id).name("Jugador").lastName("N" + id).build())
                .build();
    }

    private Position posicion(long id) {
        return Position.builder().idPosicion(id).abreviatura("P" + id).nombre("Puesto " + id).build();
    }

    private SaveLineupRequest conJugadores(List<PlayerOnField> jugadores) {
        return new SaveLineupRequest(jugadores, null, null);
    }

    @Test
    @DisplayName("no deja pasar de once titulares: el defecto que dejaba doce en la cancha")
    void topeDeOnceTitulares() {
        when(rosterService.calculate(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(injuryRepository.injuredStudentIds()).thenReturn(List.of());
        for (long i = 1; i <= 12; i++) {
            when(estudianteRepository.findByIdAndActiveTrue(i))
                    .thenReturn(Optional.of(jugador(i, categoria)));
            when(positionRepository.findById(i)).thenReturn(Optional.of(posicion(i)));
        }

        var request = conJugadores(java.util.stream.LongStream.rangeClosed(1, 12)
                .mapToObj(i -> new PlayerOnField(i, i, true)).toList());

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.save(ID_PARTIDO, request));
        assertTrue(error.getMessage().contains("11"));
        verify(lineupRepository, never()).save(any());
    }

    @Test
    @DisplayName("dos titulares no pueden ocupar el mismo puesto")
    void puestoUnicoEntreTitulares() {
        when(rosterService.calculate(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(injuryRepository.injuredStudentIds()).thenReturn(List.of());
        when(estudianteRepository.findByIdAndActiveTrue(anyLong()))
                .thenAnswer(inv -> Optional.of(jugador(inv.getArgument(0), categoria)));
        when(positionRepository.findById(1L)).thenReturn(Optional.of(posicion(1L)));

        var request = conJugadores(List.of(
                new PlayerOnField(1L, 1L, true),
                new PlayerOnField(2L, 1L, true)));

        assertThrows(IllegalArgumentException.class, () -> servicio.save(ID_PARTIDO, request));
        verify(lineupRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se alinea a alguien de otra categoria")
    void categoriaAjena() {
        when(rosterService.calculate(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(injuryRepository.injuredStudentIds()).thenReturn(List.of());
        when(estudianteRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(jugador(1L, otraCategoria)));

        var request = conJugadores(List.of(new PlayerOnField(1L, null, true)));

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.save(ID_PARTIDO, request));
        assertTrue(error.getMessage().contains("SUB-14"));
    }

    @Test
    @DisplayName("un parte medico abierto si bloquea")
    void lesionadoNoJuega() {
        when(rosterService.calculate(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(injuryRepository.injuredStudentIds()).thenReturn(List.of(1L));
        when(estudianteRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(jugador(1L, categoria)));

        var request = conJugadores(List.of(new PlayerOnField(1L, null, true)));

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.save(ID_PARTIDO, request));
        assertTrue(error.getMessage().contains("lesión"));
        verify(lineupRepository, never()).save(any());
    }

    @Test
    @DisplayName("no admite al mismo jugador dos veces")
    void sinRepetidos() {
        when(rosterService.calculate(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(injuryRepository.injuredStudentIds()).thenReturn(List.of());
        when(estudianteRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(jugador(1L, categoria)));

        var request = conJugadores(List.of(
                new PlayerOnField(1L, null, true),
                new PlayerOnField(1L, null, false)));

        assertThrows(IllegalArgumentException.class, () -> servicio.save(ID_PARTIDO, request));
    }

    @Test
    @DisplayName("sin nada guardado devuelve la sugerencia, y marca que NO es decision del entrenador")
    void sinGuardarDevuelveLaSugerencia() {
        when(rosterService.calculate(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(lineupRepository.findByMatch_Id(ID_PARTIDO)).thenReturn(Optional.empty());

        var respuesta = servicio.view(ID_PARTIDO);

        assertFalse(respuesta.guardada(),
                "confundir la sugerencia con la decision del entrenador borraria la diferencia "
                        + "entre 'jugo con este once' y 'el sistema lo propuso y nadie miro'");
        assertEquals("SUB-14", respuesta.categoria());
        assertEquals(11, respuesta.cupoTitulares());
    }

    @Test
    @DisplayName("restablecer borra la alineacion guardada y vuelve a la sugerencia")
    void restablecerBorra() {
        Lineup guardada = Lineup.builder().idAlineacion(1L).partido(partido()).build();
        when(lineupRepository.findByMatch_Id(ID_PARTIDO))
                .thenReturn(Optional.of(guardada), Optional.empty());
        when(rosterService.calculate(ID_PARTIDO)).thenReturn(convocatoriaVacia());

        var respuesta = servicio.reset(ID_PARTIDO);

        verify(lineupRepository).delete(guardada);
        assertFalse(respuesta.guardada());
    }
}
