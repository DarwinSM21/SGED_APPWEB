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
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.evaluacion.service.PlantillaService;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * La propiedad central que se verifica aqui: <b>la seleccion de la alineacion
 * es deterministica y no depende del modelo de lenguaje</b>. Si algun dia
 * alguien mueve el ranking a la IA, estas pruebas fallan, que es justo lo que
 * se quiere: esa decision tiene que poder explicarsele a un padre que pregunte
 * por que su hijo no jugo.
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

    private Asistencia asistencia(long idEstudiante) {
        return Asistencia.builder()
                .estado(Asistencia.ESTADO_PRESENTE)
                .estudiante(Estudiante.builder()
                        .idEstudiante(idEstudiante)
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
        when(generadorFeedback.generarComentarioPlantilla(anyList()))
                .thenReturn(GeneradorFeedbackIA.ResultadoFeedback.noDisponible("sin IA"));

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
        when(generadorFeedback.generarComentarioPlantilla(anyList()))
                .thenReturn(GeneradorFeedbackIA.ResultadoFeedback.noDisponible("sin IA"));

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
        when(generadorFeedback.generarComentarioPlantilla(anyList()))
                .thenReturn(GeneradorFeedbackIA.ResultadoFeedback.noDisponible("sin IA"));

        var primera = servicio.sugerir(ID_SESION);
        var segunda = servicio.sugerir(ID_SESION);

        assertEquals(3L, primera.titulares().get(0).idEstudiante());
        assertEquals(primera.titulares().get(0).idEstudiante(),
                segunda.titulares().get(0).idEstudiante());
    }

    @Test
    @DisplayName("Sin asistencias no se arma plantilla ni se llama al modelo")
    void sinAsistenciasNoLlamaAlModelo() {
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION)).thenReturn(List.of());

        var r = servicio.sugerir(ID_SESION);

        assertTrue(r.titulares().isEmpty());
        verifyNoInteractions(generadorFeedback);
    }

    @Test
    @DisplayName("Si la IA no responde, la alineacion se entrega igual")
    void alineacionSobreviveSinIa() {
        titularesConfigurados(11);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(asistencia(1L)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(evaluacionEstudianteRepository.promedioGeneralPorEstudiante(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 6.0}));
        when(generadorFeedback.generarComentarioPlantilla(anyList()))
                .thenReturn(GeneradorFeedbackIA.ResultadoFeedback.noDisponible("Servicio caido"));

        var r = servicio.sugerir(ID_SESION);

        assertEquals(1, r.titulares().size());
        assertFalse(r.comentarioGeneradoPorIa());
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

        servicio.sugerir(ID_SESION);

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(generadorFeedback).generarComentarioPlantilla(captor.capture());

        var enviados = (List<org.uteq.backend.common.ia.PerfilJugadorAnonimo>) captor.getValue();
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
