package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.guardian.service.NotificationService;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.coach.entity.Coach;
import org.uteq.backend.deportivo.coach.repository.CoachRepository;
import org.uteq.backend.deportivo.injury.entity.Injury;
import org.uteq.backend.deportivo.injury.repository.InjuryRepository;
import org.uteq.backend.deportivo.injury.service.InjuryService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InjuryServiceTest {

    @Mock private InjuryRepository injuryRepository;
    @Mock private StudentRepository estudianteRepository;
    @Mock private CoachRepository coachRepository;
    @Mock private NotificationService notificacionService;

    @InjectMocks private InjuryService servicio;

    private static final Long ID_EST = 1L;
    private static final Long ID_ENT = 2L;

    private void existenAmbos() {
        when(estudianteRepository.findById(ID_EST))
                .thenReturn(Optional.of(Student.builder().id(ID_EST).build()));
        when(coachRepository.findById(ID_ENT))
                .thenReturn(Optional.of(Coach.builder().idEntrenador(ID_ENT).build()));
    }

    @Test
    @DisplayName("Registra una lesion y por defecto queda activa")
    void registraLesionActiva() {
        existenAmbos();
        when(injuryRepository.findActiveByStudent(ID_EST)).thenReturn(Optional.empty());
        when(injuryRepository.save(any(Injury.class))).thenAnswer(i -> i.getArgument(0));

        var lesion = servicio.register(ID_EST, ID_ENT, "Esguince de tobillo",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 20));

        assertTrue(lesion.isActive());
        assertNull(lesion.getFechaAlta());
    }

    @Test
    @DisplayName("No se permite una segunda lesion activa del mismo estudiante")
    void noPermiteDosLesionesActivas() {
        existenAmbos();
        when(injuryRepository.findActiveByStudent(ID_EST)).thenReturn(
                Optional.of(Injury.builder().fechaLesion(LocalDate.of(2026, 7, 1)).build()));

        var e = assertThrows(IllegalArgumentException.class,
                () -> servicio.register(ID_EST, ID_ENT, "Otra", null, null));

        assertTrue(e.getMessage().contains("ya tiene una lesion activa"));
        verify(injuryRepository, never()).save(any());
    }

    @Test
    @DisplayName("El retorno estimado no puede ser anterior a la lesion")
    void retornoAnteriorSeRechaza() {
        existenAmbos();
        when(injuryRepository.findActiveByStudent(ID_EST)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.register(ID_EST, ID_ENT, "Desgarro",
                        LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)));

        verify(injuryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Estudiante inexistente da 404")
    void estudianteInexistente() {
        when(estudianteRepository.findById(ID_EST)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.register(ID_EST, ID_ENT, "x", null, null));
    }

    @Test
    @DisplayName("Coach inexistente da 404")
    void entrenadorInexistente() {
        when(estudianteRepository.findById(ID_EST))
                .thenReturn(Optional.of(Student.builder().id(ID_EST).build()));
        when(coachRepository.findById(ID_ENT)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.register(ID_EST, ID_ENT, "x", null, null));

        verify(injuryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dar de alta cierra la lesion y el jugador vuelve a estar disponible")
    void altaCierraLaLesion() {
        var lesion = Injury.builder()
                .idLesion(9L).fechaLesion(LocalDate.of(2026, 8, 1)).build();
        when(injuryRepository.findById(9L)).thenReturn(Optional.of(lesion));
        when(injuryRepository.save(any(Injury.class))).thenAnswer(i -> i.getArgument(0));

        var resultado = servicio.discharge(9L, LocalDate.of(2026, 8, 15));

        assertFalse(resultado.isActive());
        assertEquals(LocalDate.of(2026, 8, 15), resultado.getFechaAlta());
    }

    @Test
    @DisplayName("No se puede dar de alta dos veces")
    void noSeDaDeAltaDosVeces() {
        var yaCerrada = Injury.builder()
                .idLesion(9L)
                .fechaLesion(LocalDate.of(2026, 8, 1))
                .fechaAlta(LocalDate.of(2026, 8, 10))
                .build();
        when(injuryRepository.findById(9L)).thenReturn(Optional.of(yaCerrada));

        assertThrows(IllegalArgumentException.class, () -> servicio.discharge(9L, null));
        verify(injuryRepository, never()).save(any());
    }

    @Test
    @DisplayName("El alta no puede ser anterior a la fecha de la lesion")
    void altaAnteriorSeRechaza() {
        var lesion = Injury.builder()
                .idLesion(9L).fechaLesion(LocalDate.of(2026, 8, 10)).build();
        when(injuryRepository.findById(9L)).thenReturn(Optional.of(lesion));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.discharge(9L, LocalDate.of(2026, 8, 1)));
    }

    @Test
    @DisplayName("Dar de alta una lesion inexistente da 404")
    void darDeAltaLesionInexistente() {
        when(injuryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.discharge(9L, null));
    }

    @Test
    @DisplayName("listarActivas delega la paginacion al repositorio")
    void listarActivasDelegaAlRepositorio() {
        var pageable = PageRequest.of(0, 20);
        var pagina = new PageImpl<Injury>(List.of());
        when(injuryRepository.findActive(pageable)).thenReturn(pagina);

        assertEquals(pagina, servicio.findActive(pageable));
    }

    @Test
    @DisplayName("historialDe delega la paginacion al repositorio, filtrando por estudiante")
    void historialDeDelegaAlRepositorio() {
        var pageable = PageRequest.of(0, 20);
        var pagina = new PageImpl<Injury>(List.of());
        when(injuryRepository.findByStudentOrderByInjuryDateDesc(ID_EST, pageable))
                .thenReturn(pagina);

        assertEquals(pagina, servicio.historyOf(ID_EST, pageable));
    }

    @Test
    @DisplayName("idsLesionados delega en el repositorio")
    void idsLesionadosDelegaAlRepositorio() {
        when(injuryRepository.injuredStudentIds()).thenReturn(List.of(ID_EST));

        assertEquals(List.of(ID_EST), servicio.injuredIds());
    }
}
