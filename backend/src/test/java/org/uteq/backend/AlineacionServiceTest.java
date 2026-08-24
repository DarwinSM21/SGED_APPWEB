package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.dto.AlineacionDtos.GuardarAlineacionRequest;
import org.uteq.backend.deportivo.evaluacion.dto.AlineacionDtos.JugadorEnCancha;
import org.uteq.backend.deportivo.evaluacion.entity.Alineacion;
import org.uteq.backend.deportivo.evaluacion.repository.AlineacionRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.evaluacion.service.AlineacionService;
import org.uteq.backend.deportivo.evaluacion.service.PlantillaService;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La alineacion que el entrenador pone en cancha.
 *
 * <p>Lo que estas pruebas fijan es la regla que hace que el modulo tenga
 * sentido: la sugerencia y la decision del entrenador son cosas distintas, y
 * solo se guarda la segunda.
 */
@ExtendWith(MockitoExtension.class)
class AlineacionServiceTest {

    @Mock private AlineacionRepository alineacionRepository;
    @Mock private SesionEntrenamientoRepository sesionRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PosicionRepository posicionRepository;
    @Mock private EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    @Mock private LesionRepository lesionRepository;
    @Mock private PlantillaService plantillaService;

    @InjectMocks private AlineacionService service;

    private static final Long ID_SESION = 50L;

    private Estudiante estudiante(long id, String apellido) {
        return Estudiante.builder()
                .idEstudiante(id)
                .persona(Persona.builder().nombre("Jugador").apellido(apellido).build())
                .build();
    }

    private SesionEntrenamiento sesion() {
        return SesionEntrenamiento.builder()
                .idSesion(ID_SESION)
                .fecha(LocalDate.now())
                .categoria(Categoria.builder().idCategoria(3L).nombre("SUB-16").build())
                .build();
    }

    private Asistencia presente(Estudiante e) {
        return Asistencia.builder().estudiante(e).estado(Asistencia.ESTADO_PRESENTE).build();
    }

    private GuardarAlineacionRequest pide(JugadorEnCancha... jugadores) {
        return new GuardarAlineacionRequest(List.of(jugadores), null, null);
    }

    @Test
    @DisplayName("guardar registra el once que decidio el entrenador")
    void guardar_registra_la_decision() {
        var uno = estudiante(1L, "Uno");
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(presente(uno)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(1L)).thenReturn(Optional.of(uno));
        // La segunda lectura devuelve lo ya guardado: guardar() relee al final
        // para responder siempre el estado real, no lo que se creia haber escrito.
        var guardada = Alineacion.builder().idAlineacion(1L).sesion(sesion()).build();
        when(alineacionRepository.findBySesion_IdSesion(ID_SESION))
                .thenReturn(Optional.empty(), Optional.of(guardada));

        var r = service.guardar(ID_SESION, pide(new JugadorEnCancha(1L, null, true)));

        var capturada = org.mockito.ArgumentCaptor.forClass(Alineacion.class);
        verify(alineacionRepository).save(capturada.capture());
        assertThat(capturada.getValue().getJugadores()).hasSize(1);
        assertThat(capturada.getValue().getJugadores().get(0).getEstudiante().getIdEstudiante())
                .isEqualTo(1L);
        assertThat(r.guardada()).isTrue();
    }

    @Test
    @DisplayName("no se puede alinear a quien no asistio")
    void guardar_rechaza_al_ausente() {
        var ausente = estudiante(9L, "Ausente");
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION)).thenReturn(List.of());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(9L)).thenReturn(Optional.of(ausente));

        // Alinear a quien no estuvo rompe la relacion entre lo que se midio y
        // lo que se jugo, que es lo que este modulo existe para sostener.
        assertThatThrownBy(() -> service.guardar(ID_SESION, pide(new JugadorEnCancha(9L, null, true))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no tiene asistencia registrada");

        verify(alineacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se puede alinear a un lesionado aunque haya asistido")
    void guardar_rechaza_al_lesionado() {
        var lesionado = estudiante(7L, "Lesionado");
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(presente(lesionado)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of(7L));
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(7L)).thenReturn(Optional.of(lesionado));

        assertThatThrownBy(() -> service.guardar(ID_SESION, pide(new JugadorEnCancha(7L, null, true))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lesión activa");

        verify(alineacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("dos titulares no pueden ocupar el mismo puesto")
    void guardar_rechaza_puesto_duplicado() {
        var uno = estudiante(1L, "Uno");
        var dos = estudiante(2L, "Dos");
        var portero = Posicion.builder().idPosicion(1L).abreviatura("POR").nombre("Portero").build();

        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION))
                .thenReturn(List.of(presente(uno), presente(dos)));
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(1L)).thenReturn(Optional.of(uno));
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(2L)).thenReturn(Optional.of(dos));
        when(posicionRepository.findById(1L)).thenReturn(Optional.of(portero));

        assertThatThrownBy(() -> service.guardar(ID_SESION,
                pide(new JugadorEnCancha(1L, 1L, true), new JugadorEnCancha(2L, 1L, true))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pueden ocupar el puesto POR");

        verify(alineacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("el mismo jugador no puede estar dos veces")
    void guardar_rechaza_jugador_repetido() {
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION)).thenReturn(List.of());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(1L))
                .thenReturn(Optional.of(estudiante(1L, "Uno")));

        assertThatThrownBy(() -> service.guardar(ID_SESION,
                pide(new JugadorEnCancha(1L, null, true), new JugadorEnCancha(1L, null, false))))
                .isInstanceOf(IllegalArgumentException.class);

        verify(alineacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("sin alineacion guardada se devuelve la sugerencia del sistema")
    void ver_sin_guardar_devuelve_la_sugerida() {
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(alineacionRepository.findBySesion_IdSesion(ID_SESION)).thenReturn(Optional.empty());
        when(plantillaService.sugerir(ID_SESION)).thenReturn(
                new org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos.PlantillaResponse(
                        ID_SESION, "SUB-16", List.of(), List.of(), List.of()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION)).thenReturn(List.of());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());

        var r = service.verDeSesion(ID_SESION);

        // La bandera es lo que permite a la pantalla decir "sugerida por el
        // sistema" frente a "puesta por el entrenador".
        assertThat(r.guardada()).isFalse();
    }

    @Test
    @DisplayName("restablecer borra la alineacion y vuelve a la sugerida")
    void restablecer_vuelve_a_la_sugerencia() {
        var guardada = Alineacion.builder().idAlineacion(1L).sesion(sesion()).build();
        when(alineacionRepository.findBySesion_IdSesion(ID_SESION))
                .thenReturn(Optional.of(guardada), Optional.empty());
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion()));
        when(plantillaService.sugerir(ID_SESION)).thenReturn(
                new org.uteq.backend.deportivo.evaluacion.dto.EvaluacionDtos.PlantillaResponse(
                        ID_SESION, "SUB-16", List.of(), List.of(), List.of()));
        when(asistenciaRepository.listarHabilitadosParaEvaluar(ID_SESION)).thenReturn(List.of());
        when(lesionRepository.idsEstudiantesLesionados()).thenReturn(List.of());

        var r = service.restablecer(ID_SESION);

        verify(alineacionRepository).delete(guardada);
        assertThat(r.guardada()).isFalse();
    }
}
