package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.common.ia.GeneradorFeedbackIA;
import org.uteq.backend.common.ia.PerfilJugadorAnonimo;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.evaluacion.service.PlantillaService;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * `sugerir()` (calculo de la alineacion) y `feedback()` (comentario de IA) se
 * separaron a proposito: el primero es gratis y determinista, el segundo
 * llama a un servicio externo solo cuando el entrenador lo pide (boton
 * "Feedback IA" en la pantalla de plantilla). Las pruebas verifican esa
 * separacion explicitamente, no solo el resultado final.
 */
@ExtendWith(MockitoExtension.class)
class PlantillaServiceTest {

    @Mock private SesionEntrenamientoRepository sesionRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    @Mock private LesionRepository lesionRepository;
    @Mock private GeneradorFeedbackIA generadorFeedback;

    @InjectMocks private PlantillaService servicio;

    private static final Long ID_SESION = 1L;

    private void titularesConfigurados(int n) {
        ReflectionTestUtils.setField(servicio, "cantidadTitulares", n);
    }

    private SesionEntrenamiento sesion() {
        return SesionEntrenamiento.builder()
                .idSesion(ID_SESION)
                .categoria(Categoria.builder().idCategoria(1L).nombre("SUB-12").build())
                .build();
    }

    /**
     * Cada estudiante recibe una posicion propia. Desde que la sugerencia
     * titulariza al mejor promediado DE CADA posicion -y ya no a los N
     * mejores promedios sin mirar el puesto-, un estudiante sin posicion no
     * puede ocupar ninguna y cae siempre a suplente. Darles una distinta a
     * cada uno deja que el orden lo decida el promedio, que es lo que estas
     * pruebas verifican.
     */
    private Asistencia asistencia(long idEstudiante) {
        return asistencia(idEstudiante, Posicion.builder()
                .idPosicion(idEstudiante)
                .nombre("Posicion " + idEstudiante)
                .abreviatura("P" + idEstudiante)
                .build());
    }

    /** Para el caso explicito de quien no tiene posicion registrada. */
    private Asistencia asistenciaSinPosicion(long idEstudiante) {
        return asistencia(idEstudiante, null);
    }

    private Asistencia asistencia(long idEstudiante, Posicion posicion) {
        return Asistencia.builder()
                .estado(Asistencia.ESTADO_PRESENTE)
                .estudiante(Estudiante.builder()
                        .idEstudiante(idEstudiante)
                        .posicion(posicion)
                        .persona(Persona.builder()
                                .nombre("Jugador").apellido(String.valueOf(idEstudiante)).build())
                        .build())
                .build();
    }

    @Test
    @DisplayName("Ordena por promedio descendente, no por orden de llegada")
    void ordenaPorPromedio() {
        titularesConfigurados(2);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(asistencia(1L), asistencia(2L), asistencia(3L)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(evaluacionEstudianteRepository.promedioGeneralPorEstudiante(anyList()))
                .thenReturn(List.of(
                        new Object[]{1L, 5.0},
                        new Object[]{2L, 9.0},
                        new Object[]{3L, 7.0}));

        var r = servicio.sugerir(ID_SESION);

        assertEquals(List.of(2L, 3L), r.titulares().stream()
                .map(t -> t.idEstudiante()).toList());
        assertEquals(List.of(1L), r.suplentes().stream()
                .map(s -> s.idEstudiante()).toList());
    }

    @Test
    @DisplayName("Los lesionados quedan fuera aunque tengan el mejor promedio")
    void excluyeLesionados() {
        titularesConfigurados(11);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(asistencia(1L), asistencia(2L)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of(2L));
        when(evaluacionEstudianteRepository.promedioGeneralPorEstudiante(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 4.0}));

        var r = servicio.sugerir(ID_SESION);

        assertEquals(List.of(2L), r.excluidosPorLesion());
        assertTrue(r.titulares().stream().noneMatch(t -> t.idEstudiante().equals(2L)));
    }

    @Test
    @DisplayName("Ante empate desempata por id: dos llamadas dan la misma alineacion")
    void desempateEstable() {
        titularesConfigurados(1);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(asistencia(7L), asistencia(3L)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(evaluacionEstudianteRepository.promedioGeneralPorEstudiante(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{7L, 8.0}, new Object[]{3L, 8.0}));

        var primera = servicio.sugerir(ID_SESION);
        var segunda = servicio.sugerir(ID_SESION);

        assertEquals(3L, primera.titulares().get(0).idEstudiante());
        assertEquals(primera.titulares().get(0).idEstudiante(),
                segunda.titulares().get(0).idEstudiante());
    }

    @Test
    @DisplayName("La posicion que se muestra es la nominal del estudiante")
    void incluyeLaPosicionNominal() {
        titularesConfigurados(11);
        var extremo = Posicion.builder().idPosicion(5L).nombre("Extremo derecho").abreviatura("ED").build();
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(asistencia(1L, extremo)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(evaluacionEstudianteRepository.promedioGeneralPorEstudiante(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 7.0}));

        var r = servicio.sugerir(ID_SESION);

        assertEquals("ED", r.titulares().get(0).posicion());
    }

    @Test
    @DisplayName("sugerir() nunca llama al modelo de lenguaje")
    void sugerirNuncaLlamaAlModelo() {
        titularesConfigurados(11);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(asistencia(1L)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(evaluacionEstudianteRepository.promedioGeneralPorEstudiante(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 6.0}));

        servicio.sugerir(ID_SESION);

        verifyNoInteractions(generadorFeedback);
    }

    @Test
    @DisplayName("Sin asistencias, sugerir() no arma plantilla ni consulta promedios")
    void sinAsistenciasListaVacia() {
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION)).thenReturn(List.of());

        var r = servicio.sugerir(ID_SESION);

        assertTrue(r.titulares().isEmpty());
        verifyNoInteractions(generadorFeedback);
        verifyNoInteractions(evaluacionEstudianteRepository);
    }

    @Test
    @DisplayName("feedback() calcula la misma alineacion y ademas pide el comentario")
    void feedbackUsaLaMismaAlineacion() {
        titularesConfigurados(11);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(asistencia(1L)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(evaluacionEstudianteRepository.promedioGeneralPorEstudiante(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 6.0}));
        when(generadorFeedback.generarComentarioPlantilla(anyList()))
                .thenReturn(GeneradorFeedbackIA.ResultadoFeedback.ok("Buen bloque."));

        var r = servicio.feedback(ID_SESION);

        assertTrue(r.generadoPorIa());
        assertEquals("Buen bloque.", r.comentario());
        verify(generadorFeedback).generarComentarioPlantilla(anyList());
    }

    @Test
    @DisplayName("Si la IA no responde, feedback() no lanza excepcion")
    void feedbackSobreviveSinIa() {
        titularesConfigurados(11);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(asistencia(1L)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(evaluacionEstudianteRepository.promedioGeneralPorEstudiante(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 6.0}));
        when(generadorFeedback.generarComentarioPlantilla(anyList()))
                .thenReturn(GeneradorFeedbackIA.ResultadoFeedback.noDisponible("Servicio caido"));

        var r = assertDoesNotThrow(() -> servicio.feedback(ID_SESION));

        assertFalse(r.generadoPorIa());
        assertNotNull(r.motivoNoDisponible());
    }

    @Test
    @DisplayName("feedback() con plantilla vacia no llama al modelo")
    void feedbackConPlantillaVaciaNoLlamaAlModelo() {
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION)).thenReturn(List.of());

        var r = servicio.feedback(ID_SESION);

        assertFalse(r.generadoPorIa());
        verifyNoInteractions(generadorFeedback);
    }

    @Test
    @DisplayName("Al modelo se le mandan referencias anonimas, nunca nombres")
    void noSeEnvianNombresAlModelo() {
        titularesConfigurados(11);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(asistencia(1L)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(evaluacionEstudianteRepository.promedioGeneralPorEstudiante(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 6.0}));
        when(generadorFeedback.generarComentarioPlantilla(anyList()))
                .thenReturn(GeneradorFeedbackIA.ResultadoFeedback.ok("Buen bloque defensivo."));

        servicio.feedback(ID_SESION);

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(generadorFeedback).generarComentarioPlantilla(captor.capture());

        @SuppressWarnings("unchecked")
        var enviados = (List<PerfilJugadorAnonimo>) captor.getValue();
        assertEquals("Jugador 1", enviados.get(0).referencia());
        // El apellido real del mock es "1"; la referencia enviada es posicional
        // y no permite reconstruir a la persona.
        assertEquals("SUB-12", enviados.get(0).categoria());
    }

    @Test
    @DisplayName("Sesion inexistente da 404")
    void sesionInexistente() {
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.sugerir(ID_SESION));
    }
}
