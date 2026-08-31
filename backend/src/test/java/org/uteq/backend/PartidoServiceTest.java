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
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.evaluacion.repository.AlineacionRepository;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.CrearPartidoRequest;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.ResultadoRequest;
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.partido.repository.PartidoRepository;
import org.uteq.backend.deportivo.partido.service.PartidoService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartidoServiceTest {
    @Mock private PartidoRepository partidoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private AlineacionRepository alineacionRepository;

    @InjectMocks private PartidoService servicio;

    private static final Long ID_PARTIDO = 7L;
    private final Categoria categoria = Categoria.builder()
            .idCategoria(3L).nombre("SUB-14").activo(true).build();

    private Partido conMarcador(Short favor, Short contra) {
        return Partido.builder().idPartido(ID_PARTIDO).categoria(categoria)
                .fecha(LocalDate.of(2026, 8, 29)).golesFavor(favor).golesContra(contra).build();
    }

    private void devuelve(Partido p) {
        when(partidoRepository.findWithCategoriaByIdPartido(ID_PARTIDO)).thenReturn(Optional.of(p));
        when(alineacionRepository.contarTitularesPorPartido(List.of(ID_PARTIDO))).thenReturn(List.of());
    }

    @Test
    @DisplayName("sin marcador el partido esta PENDIENTE, no empatado 0-0")
    void sinMarcadorEsPendiente() {
        devuelve(conMarcador(null, null));

        assertEquals("PENDIENTE", servicio.buscarPorId(ID_PARTIDO).resultado());
    }

    @Test
    @DisplayName("el resultado se deduce del marcador")
    void resultadoSeDeduce() {
        devuelve(conMarcador((short) 3, (short) 1));
        assertEquals("GANADO", servicio.buscarPorId(ID_PARTIDO).resultado());

        reset(partidoRepository, alineacionRepository);
        devuelve(conMarcador((short) 2, (short) 2));
        assertEquals("EMPATADO", servicio.buscarPorId(ID_PARTIDO).resultado());

        reset(partidoRepository, alineacionRepository);
        devuelve(conMarcador((short) 0, (short) 1));
        assertEquals("PERDIDO", servicio.buscarPorId(ID_PARTIDO).resultado());
    }

    @Test
    @DisplayName("no se agenda un partido de una categoria dada de baja")
    void categoriaInactiva() {
        Categoria inactiva = Categoria.builder().idCategoria(4L).nombre("SUB-9").activo(false).build();
        when(categoriaRepository.findById(4L)).thenReturn(Optional.of(inactiva));

        var request = new CrearPartidoRequest(4L, LocalDate.of(2026, 8, 29), null, null);

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(request));
        verify(partidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cargar el resultado de un partido que no existe da 404")
    void resultadoDePartidoInexistente() {
        when(partidoRepository.findWithCategoriaByIdPartido(99L)).thenReturn(Optional.empty());
        var request = new ResultadoRequest((short) 1, (short) 0, null);
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.registrarResultado(99L, request));
    }

    @Test
    @DisplayName("la lista cuenta los titulares de toda la pagina en una consulta, no una por fila")
    void titularesEnUnaConsulta() {
        Partido a = conMarcador(null, null);
        Partido b = Partido.builder().idPartido(8L).categoria(categoria)
                .fecha(LocalDate.of(2026, 8, 22)).build();
        Page<Partido> page = new PageImpl<>(List.of(a, b), Pageable.ofSize(20), 2);
        when(partidoRepository.findAllByOrderByFechaDescHoraDesc(any())).thenReturn(page);
        when(alineacionRepository.contarTitularesPorPartido(List.of(ID_PARTIDO, 8L)))
                .thenReturn(List.<Object[]>of(new Object[]{ID_PARTIDO, 11L}));

        var respuesta = servicio.listar(null, 0, 20);

        verify(alineacionRepository, times(1)).contarTitularesPorPartido(any());
        assertTrue(respuesta.contenido().get(0).tieneAlineacion());
        assertEquals(11, respuesta.contenido().get(0).titulares());
        assertFalse(respuesta.contenido().get(1).tieneAlineacion());
    }
}
