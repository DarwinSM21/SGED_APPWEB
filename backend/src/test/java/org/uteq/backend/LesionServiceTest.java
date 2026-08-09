package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.service.NotificacionService;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.lesion.service.LesionService;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LesionServiceTest {

    @Mock private LesionRepository lesionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private EntrenadorRepository entrenadorRepository;
    @Mock private NotificacionService notificacionService;

    @InjectMocks private LesionService servicio;

    private static final Long ID_EST = 1L;
    private static final Long ID_ENT = 2L;

    private void existenAmbos() {
        when(estudianteRepository.findById(ID_EST))
                .thenReturn(Optional.of(Estudiante.builder().idEstudiante(ID_EST).build()));
        when(entrenadorRepository.findById(ID_ENT))
                .thenReturn(Optional.of(Entrenador.builder().idEntrenador(ID_ENT).build()));
    }

    @Test
    @DisplayName("Registra una lesion y por defecto queda activa")
    void registraLesionActiva() {
        existenAmbos();
        when(lesionRepository.buscarActivaPorEstudiante(ID_EST)).thenReturn(Optional.empty());
        when(lesionRepository.save(any(Lesion.class))).thenAnswer(i -> i.getArgument(0));

        var lesion = servicio.registrar(ID_EST, ID_ENT, "Esguince de tobillo",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 20));

        assertTrue(lesion.estaActiva());
        assertNull(lesion.getFechaAlta());
    }

    @Test
    @DisplayName("No se permite una segunda lesion activa del mismo estudiante")
    void noPermiteDosLesionesActivas() {
        existenAmbos();
        when(lesionRepository.buscarActivaPorEstudiante(ID_EST)).thenReturn(
                Optional.of(Lesion.builder().fechaLesion(LocalDate.of(2026, 7, 1)).build()));

        var e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(ID_EST, ID_ENT, "Otra", null, null));

        assertTrue(e.getMessage().contains("ya tiene una lesion activa"));
        verify(lesionRepository, never()).save(any());
    }

    @Test
    @DisplayName("El retorno estimado no puede ser anterior a la lesion")
    void retornoAnteriorSeRechaza() {
        existenAmbos();
        when(lesionRepository.buscarActivaPorEstudiante(ID_EST)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(ID_EST, ID_ENT, "Desgarro",
                        LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)));

        verify(lesionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Estudiante inexistente da 404")
    void estudianteInexistente() {
        when(estudianteRepository.findById(ID_EST)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrar(ID_EST, ID_ENT, "x", null, null));
    }

    @Test
    @DisplayName("Dar de alta cierra la lesion y el jugador vuelve a estar disponible")
    void altaCierraLaLesion() {
        var lesion = Lesion.builder()
                .idLesion(9L).fechaLesion(LocalDate.of(2026, 8, 1)).build();
        when(lesionRepository.findById(9L)).thenReturn(Optional.of(lesion));
        when(lesionRepository.save(any(Lesion.class))).thenAnswer(i -> i.getArgument(0));

        var resultado = servicio.darDeAlta(9L, LocalDate.of(2026, 8, 15));

        assertFalse(resultado.estaActiva());
        assertEquals(LocalDate.of(2026, 8, 15), resultado.getFechaAlta());
    }

    @Test
    @DisplayName("No se puede dar de alta dos veces")
    void noSeDaDeAltaDosVeces() {
        var yaCerrada = Lesion.builder()
                .idLesion(9L)
                .fechaLesion(LocalDate.of(2026, 8, 1))
                .fechaAlta(LocalDate.of(2026, 8, 10))
                .build();
        when(lesionRepository.findById(9L)).thenReturn(Optional.of(yaCerrada));

        assertThrows(IllegalArgumentException.class, () -> servicio.darDeAlta(9L, null));
        verify(lesionRepository, never()).save(any());
    }

    @Test
    @DisplayName("El alta no puede ser anterior a la fecha de la lesion")
    void altaAnteriorSeRechaza() {
        var lesion = Lesion.builder()
                .idLesion(9L).fechaLesion(LocalDate.of(2026, 8, 10)).build();
        when(lesionRepository.findById(9L)).thenReturn(Optional.of(lesion));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.darDeAlta(9L, LocalDate.of(2026, 8, 1)));
    }
}
