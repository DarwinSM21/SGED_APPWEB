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
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.common.ia.GeneradorFeedbackIA;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.partido.repository.PartidoRepository;
import org.uteq.backend.deportivo.partido.service.ConvocatoriaService;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConvocatoriaServiceTest {
    @Mock private PartidoRepository partidoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private SesionEntrenamientoRepository sesionRepository;
    @Mock private LesionRepository lesionRepository;
    @Mock private GeneradorFeedbackIA generadorFeedback;

    @InjectMocks private ConvocatoriaService servicio;

    private static final Long ID_PARTIDO = 7L;
    private static final Long ID_CATEGORIA = 3L;
    private static final LocalDate FECHA = LocalDate.of(2026, 8, 29);

    private final Categoria categoria = Categoria.builder()
            .idCategoria(ID_CATEGORIA).nombre("SUB-14").activo(true).build();

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(servicio, "cantidadTitulares", 11);
        ReflectionTestUtils.setField(servicio, "semanasRendimiento", 4);
    }

    private Partido partido() {
        return Partido.builder().idPartido(ID_PARTIDO).categoria(categoria).fecha(FECHA).build();
    }

    private Estudiante jugador(long id, String apellido, Long idPosicion, String abreviatura) {
        Posicion posicion = idPosicion == null ? null
                : Posicion.builder().idPosicion(idPosicion).abreviatura(abreviatura).nombre(abreviatura).build();
        return Estudiante.builder()
                .idEstudiante(id)
                .activo(true)
                .categoria(categoria)
                .posicion(posicion)
                .persona(Persona.builder().idPersona(id).nombre("Jugador").apellido(apellido).build())
                .build();
    }

    private void plantel(List<Estudiante> estudiantes) {
        when(partidoRepository.findWithCategoriaByIdPartido(ID_PARTIDO)).thenReturn(Optional.of(partido()));
        when(estudianteRepository
                .findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(ID_CATEGORIA))
                .thenReturn(estudiantes);
        when(sesionRepository.countByCategoriaIdCategoriaAndFechaBetween(eq(ID_CATEGORIA), any(), any()))
                .thenReturn(8L);
    }

    @Test
    @DisplayName("titulariza al mejor de cada puesto, no a los mejores promedios sin mirar posicion")
    void unTitularPorPuesto() {
        plantel(List.of(
                jugador(1L, "Alfa", 1L, "POR"),
                jugador(2L, "Bravo", 1L, "POR"),
                jugador(3L, "Charlie", 3L, "DCI")));
        when(evaluacionEstudianteRepository.promedioEnVentana(any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 9.0}, new Object[]{2L, 8.5}, new Object[]{3L, 4.0}));
        when(asistenciaRepository.presenciasEnVentana(any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 8L}, new Object[]{2L, 8L}, new Object[]{3L, 8L}));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());

        var convocatoria = servicio.calcular(ID_PARTIDO);

        assertEquals(2, convocatoria.titulares().size(), "un portero y un defensa");
        assertEquals(List.of(1L, 3L),
                convocatoria.titulares().stream().map(j -> j.idEstudiante()).toList());
        assertEquals(1, convocatoria.suplentes().size());
        assertEquals(2L, convocatoria.suplentes().get(0).idEstudiante(), "el segundo portero al banco");
    }

    @Test
    @DisplayName("el lesionado queda fuera CON el motivo, no desaparece de la lista")
    void lesionadoFueraConMotivo() {
        plantel(List.of(jugador(1L, "Alfa", 1L, "POR"), jugador(2L, "Bravo", 3L, "DCI")));
        when(evaluacionEstudianteRepository.promedioEnVentana(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 9.0}, new Object[]{2L, 8.0}));
        when(asistenciaRepository.presenciasEnVentana(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 8L}, new Object[]{2L, 8L}));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of(1L));

        var convocatoria = servicio.calcular(ID_PARTIDO);

        assertEquals(1, convocatoria.titulares().size());
        assertEquals(2L, convocatoria.titulares().get(0).idEstudiante());
        assertEquals(1, convocatoria.noConvocables().size());
        assertEquals(1L, convocatoria.noConvocables().get(0).idEstudiante());
        assertEquals("Lesión activa", convocatoria.noConvocables().get(0).motivo());
    }

    @Test
    @DisplayName("quien no entreno en la ventana no se convoca, pero se dice por que")
    void sinEntrenarNoSeConvoca() {
        plantel(List.of(jugador(1L, "Alfa", 1L, "POR"), jugador(2L, "Bravo", 3L, "DCI")));
        when(evaluacionEstudianteRepository.promedioEnVentana(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 9.5}));

        when(asistenciaRepository.presenciasEnVentana(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 6L}));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());

        var convocatoria = servicio.calcular(ID_PARTIDO);

        assertEquals(1, convocatoria.titulares().size());
        assertEquals(1, convocatoria.noConvocables().size());
        assertTrue(convocatoria.noConvocables().get(0).motivo().contains("No entrenó"),
                "el motivo tiene que ser legible, no un codigo");
    }

    @Test
    @DisplayName("si la categoria no tuvo entrenamientos, no se castiga a nadie por no asistir")
    void sinEntrenamientosNadieQuedaFuera() {
        when(partidoRepository.findWithCategoriaByIdPartido(ID_PARTIDO)).thenReturn(Optional.of(partido()));
        when(estudianteRepository
                .findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(ID_CATEGORIA))
                .thenReturn(List.of(jugador(1L, "Alfa", 1L, "POR")));
        when(sesionRepository.countByCategoriaIdCategoriaAndFechaBetween(eq(ID_CATEGORIA), any(), any()))
                .thenReturn(0L);
        when(evaluacionEstudianteRepository.promedioEnVentana(any(), any(), any())).thenReturn(List.of());
        when(asistenciaRepository.presenciasEnVentana(any(), any(), any())).thenReturn(List.of());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());

        var convocatoria = servicio.calcular(ID_PARTIDO);

        assertTrue(convocatoria.noConvocables().isEmpty(),
                "nadie pudo asistir a entrenamientos que no existieron");
        assertEquals(1, convocatoria.titulares().size());
    }

    @Test
    @DisplayName("sin evaluaciones el promedio viaja en null, no en cero")
    void promedioNuloNoEsCero() {
        plantel(List.of(jugador(1L, "Alfa", 1L, "POR")));
        when(evaluacionEstudianteRepository.promedioEnVentana(any(), any(), any())).thenReturn(List.of());
        when(asistenciaRepository.presenciasEnVentana(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 5L}));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());

        var convocatoria = servicio.calcular(ID_PARTIDO);

        assertNull(convocatoria.titulares().get(0).promedio());
        assertEquals(5L, convocatoria.titulares().get(0).presencias());
    }

    @Test
    @DisplayName("ante el mismo promedio manda la asistencia, y despues el id: el orden es reproducible")
    void desempateDeterministico() {
        plantel(List.of(
                jugador(1L, "Alfa", 1L, "POR"),
                jugador(2L, "Bravo", 1L, "POR"),
                jugador(3L, "Charlie", 1L, "POR")));
        when(evaluacionEstudianteRepository.promedioEnVentana(any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 7.0}, new Object[]{2L, 7.0}, new Object[]{3L, 7.0}));
        when(asistenciaRepository.presenciasEnVentana(any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{1L, 2L}, new Object[]{2L, 8L}, new Object[]{3L, 8L}));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());

        var convocatoria = servicio.calcular(ID_PARTIDO);

        assertEquals(2L, convocatoria.titulares().get(0).idEstudiante());
        assertEquals(List.of(3L, 1L),
                convocatoria.suplentes().stream().map(j -> j.idEstudiante()).toList());
    }

    @Test
    @DisplayName("partido inexistente da 404, no una convocatoria vacia")
    void partidoInexistente() {
        when(partidoRepository.findWithCategoriaByIdPartido(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.calcular(99L));
        verifyNoInteractions(estudianteRepository);
    }

    @Test
    @DisplayName("no llama a la IA para calcular: la seleccion es una regla, no una opinion del modelo")
    void laIaNoDecideQuienJuega() {
        plantel(List.of(jugador(1L, "Alfa", 1L, "POR")));
        when(evaluacionEstudianteRepository.promedioEnVentana(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 8.0}));
        when(asistenciaRepository.presenciasEnVentana(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 4L}));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());

        servicio.calcular(ID_PARTIDO);

        verifyNoInteractions(generadorFeedback);
    }
}
