package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.entity.Alineacion;
import org.uteq.backend.deportivo.evaluacion.repository.AlineacionRepository;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.partido.dto.AlineacionDtos.GuardarAlineacionRequest;
import org.uteq.backend.deportivo.partido.dto.AlineacionDtos.JugadorEnCancha;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.VentanaRendimiento;
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.partido.service.AlineacionService;
import org.uteq.backend.deportivo.partido.service.ConvocatoriaService;
import org.uteq.backend.deportivo.partido.service.PartidoService;
import org.uteq.backend.deportivo.partido.service.ConvocatoriaService.Convocatoria;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlineacionServiceTest {
    @Mock private AlineacionRepository alineacionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PosicionRepository posicionRepository;
    @Mock private LesionRepository lesionRepository;
    @Mock private ConvocatoriaService convocatoriaService;
    @Mock private PartidoService partidoService;

    @InjectMocks private AlineacionService servicio;

    private static final Long ID_PARTIDO = 7L;
    private static final Long ID_CATEGORIA = 3L;

    private final Categoria categoria = Categoria.builder()
            .idCategoria(ID_CATEGORIA).nombre("SUB-14").activo(true).build();
    private final Categoria otraCategoria = Categoria.builder()
            .idCategoria(99L).nombre("SUB-17").activo(true).build();

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(servicio, "cupoTitulares", 11);
    }

    private Partido partido() {
        return Partido.builder().idPartido(ID_PARTIDO).categoria(categoria)
                .fecha(LocalDate.of(2026, 8, 29)).build();
    }

    private Convocatoria convocatoriaVacia() {
        return new Convocatoria(partido(),
                new VentanaRendimiento(4, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 29), 8L),
                List.of(), List.of(), List.of(), Map.of(), Map.of(), 8L);
    }

    private Estudiante jugador(long id, Categoria suCategoria) {
        return Estudiante.builder()
                .idEstudiante(id).activo(true).categoria(suCategoria)
                .persona(Persona.builder().idPersona(id).nombre("Jugador").apellido("N" + id).build())
                .build();
    }

    private Posicion posicion(long id) {
        return Posicion.builder().idPosicion(id).abreviatura("P" + id).nombre("Puesto " + id).build();
    }

    private GuardarAlineacionRequest conJugadores(List<JugadorEnCancha> jugadores) {
        return new GuardarAlineacionRequest(jugadores, null, null);
    }

    @Test
    @DisplayName("no deja pasar de once titulares: el defecto que dejaba doce en la cancha")
    void topeDeOnceTitulares() {
        when(convocatoriaService.calcular(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        for (long i = 1; i <= 12; i++) {
            when(estudianteRepository.findByIdEstudianteAndActivoTrue(i))
                    .thenReturn(Optional.of(jugador(i, categoria)));
            when(posicionRepository.findById(i)).thenReturn(Optional.of(posicion(i)));
        }

        var request = conJugadores(java.util.stream.LongStream.rangeClosed(1, 12)
                .mapToObj(i -> new JugadorEnCancha(i, i, true)).toList());

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardar(ID_PARTIDO, request));
        assertTrue(error.getMessage().contains("11"));
        verify(alineacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("dos titulares no pueden ocupar el mismo puesto")
    void puestoUnicoEntreTitulares() {
        when(convocatoriaService.calcular(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(anyLong()))
                .thenAnswer(inv -> Optional.of(jugador(inv.getArgument(0), categoria)));
        when(posicionRepository.findById(1L)).thenReturn(Optional.of(posicion(1L)));

        var request = conJugadores(List.of(
                new JugadorEnCancha(1L, 1L, true),
                new JugadorEnCancha(2L, 1L, true)));

        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(ID_PARTIDO, request));
        verify(alineacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se alinea a alguien de otra categoria")
    void categoriaAjena() {
        when(convocatoriaService.calcular(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(1L))
                .thenReturn(Optional.of(jugador(1L, otraCategoria)));

        var request = conJugadores(List.of(new JugadorEnCancha(1L, null, true)));

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardar(ID_PARTIDO, request));
        assertTrue(error.getMessage().contains("SUB-14"));
    }

    @Test
    @DisplayName("un parte medico abierto si bloquea")
    void lesionadoNoJuega() {
        when(convocatoriaService.calcular(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of(1L));
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(1L))
                .thenReturn(Optional.of(jugador(1L, categoria)));

        var request = conJugadores(List.of(new JugadorEnCancha(1L, null, true)));

        var error = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardar(ID_PARTIDO, request));
        assertTrue(error.getMessage().contains("lesión"));
        verify(alineacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("no admite al mismo jugador dos veces")
    void sinRepetidos() {
        when(convocatoriaService.calcular(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(1L))
                .thenReturn(Optional.of(jugador(1L, categoria)));

        var request = conJugadores(List.of(
                new JugadorEnCancha(1L, null, true),
                new JugadorEnCancha(1L, null, false)));

        assertThrows(IllegalArgumentException.class, () -> servicio.guardar(ID_PARTIDO, request));
    }

    @Test
    @DisplayName("sin nada guardado devuelve la sugerencia, y marca que NO es decision del entrenador")
    void sinGuardarDevuelveLaSugerencia() {
        when(convocatoriaService.calcular(ID_PARTIDO)).thenReturn(convocatoriaVacia());
        when(alineacionRepository.findByPartido_IdPartido(ID_PARTIDO)).thenReturn(Optional.empty());

        var respuesta = servicio.ver(ID_PARTIDO);

        assertFalse(respuesta.guardada(),
                "confundir la sugerencia con la decision del entrenador borraria la diferencia "
                        + "entre 'jugo con este once' y 'el sistema lo propuso y nadie miro'");
        assertEquals("SUB-14", respuesta.categoria());
        assertEquals(11, respuesta.cupoTitulares());
    }

    @Test
    @DisplayName("restablecer borra la alineacion guardada y vuelve a la sugerencia")
    void restablecerBorra() {
        Alineacion guardada = Alineacion.builder().idAlineacion(1L).partido(partido()).build();
        when(alineacionRepository.findByPartido_IdPartido(ID_PARTIDO))
                .thenReturn(Optional.of(guardada), Optional.empty());
        when(convocatoriaService.calcular(ID_PARTIDO)).thenReturn(convocatoriaVacia());

        var respuesta = servicio.restablecer(ID_PARTIDO);

        verify(alineacionRepository).delete(guardada);
        assertFalse(respuesta.guardada());
    }
}
