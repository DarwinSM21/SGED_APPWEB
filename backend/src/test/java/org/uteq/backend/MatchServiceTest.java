package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.category.repository.CategoryRepository;
import org.uteq.backend.deportivo.evaluation.repository.LineupRepository;
import org.uteq.backend.deportivo.match.dto.MatchDtos.CreateMatchRequest;
import org.uteq.backend.deportivo.match.dto.MatchDtos.ResultRequest;
import org.uteq.backend.deportivo.match.entity.Match;
import org.uteq.backend.deportivo.match.repository.MatchRepository;
import org.uteq.backend.deportivo.match.service.MatchService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {
    @Mock private MatchRepository matchRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private LineupRepository lineupRepository;

    @InjectMocks private MatchService servicio;

    private static final Long ID_PARTIDO = 7L;
    private final Category categoria = Category.builder()
            .idCategoria(3L).nombre("SUB-14").activo(true).build();

    private Match conMarcador(Short favor, Short contra) {
        return Match.builder().idPartido(ID_PARTIDO).categoria(categoria)
                .fecha(LocalDate.of(2026, 8, 29)).golesFavor(favor).golesContra(contra).build();
    }

    private void devuelve(Match p) {
        when(matchRepository.findWithCategoryById(ID_PARTIDO)).thenReturn(Optional.of(p));
        when(lineupRepository.countStartersByMatch(List.of(ID_PARTIDO))).thenReturn(List.of());
    }

    @Test
    @DisplayName("sin marcador el partido esta PENDIENTE, no empatado 0-0")
    void sinMarcadorEsPendiente() {
        devuelve(conMarcador(null, null));

        assertEquals("PENDIENTE", servicio.findById(ID_PARTIDO).resultado());
    }

    @Test
    @DisplayName("el resultado se deduce del marcador")
    void resultadoSeDeduce() {
        devuelve(conMarcador((short) 3, (short) 1));
        assertEquals("GANADO", servicio.findById(ID_PARTIDO).resultado());

        reset(matchRepository, lineupRepository);
        devuelve(conMarcador((short) 2, (short) 2));
        assertEquals("EMPATADO", servicio.findById(ID_PARTIDO).resultado());

        reset(matchRepository, lineupRepository);
        devuelve(conMarcador((short) 0, (short) 1));
        assertEquals("PERDIDO", servicio.findById(ID_PARTIDO).resultado());
    }

    @Test
    @DisplayName("no se agenda un partido de una categoria dada de baja")
    void categoriaInactiva() {
        Category inactiva = Category.builder().idCategoria(4L).nombre("SUB-9").activo(false).build();
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(inactiva));

        var request = new CreateMatchRequest(4L, LocalDate.of(2026, 8, 29), null, null);

        assertThrows(IllegalArgumentException.class, () -> servicio.create(request));
        verify(matchRepository, never()).save(any());
    }

    @Test
    @DisplayName("cargar el resultado de un partido que no existe da 404")
    void resultadoDePartidoInexistente() {
        when(matchRepository.findWithCategoryById(99L)).thenReturn(Optional.empty());
        var request = new ResultRequest((short) 1, (short) 0, null);
        assertThrows(ResourceNotFoundException.class, () -> servicio.registerResult(99L, request));
    }

    @Test
    @DisplayName("la lista cuenta los titulares de toda la pagina en una consulta, no una por fila")
    void titularesEnUnaConsulta() {
        Match a = conMarcador(null, null);
        Match b = Match.builder().idPartido(8L).categoria(categoria)
                .fecha(LocalDate.of(2026, 8, 22)).build();
        Page<Match> page = new PageImpl<>(List.of(a, b), Pageable.ofSize(20), 2);
        when(matchRepository.findAllOrderByDateDescTimeDesc(any())).thenReturn(page);
        when(lineupRepository.countStartersByMatch(List.of(ID_PARTIDO, 8L)))
                .thenReturn(List.<Object[]>of(new Object[]{ID_PARTIDO, 11L}));

        var respuesta = servicio.list(null, 0, 20);

        verify(lineupRepository, times(1)).countStartersByMatch(any());
        assertTrue(respuesta.contenido().get(0).tieneAlineacion());
        assertEquals(11, respuesta.contenido().get(0).titulares());
        assertFalse(respuesta.contenido().get(1).tieneAlineacion());
    }
}
