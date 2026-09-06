package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.guardian.repository.NotificationRepository;
import org.uteq.backend.academico.guardian.repository.GuardianStudentRepository;
import org.uteq.backend.academico.guardian.repository.GuardianRepository;
import org.uteq.backend.academico.guardian.service.NotificationService;
import org.uteq.backend.seguridad.person.entity.Person;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.uteq.backend.academico.guardian.entity.Consent;
import org.uteq.backend.academico.guardian.entity.Notification;
import org.uteq.backend.academico.guardian.entity.Guardian;
import org.uteq.backend.academico.guardian.entity.GuardianStudent;
import org.uteq.backend.academico.guardian.repository.ConsentRepository;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock private NotificationRepository notificacionRepository;
    @Mock private GuardianStudentRepository vinculoRepository;
    @Mock private GuardianRepository representanteRepository;
    @Mock private ConsentRepository consentimientoRepository;

    @InjectMocks
    private NotificationService notificacionService;

    private Estudiante estudianteValido() {
        Person persona = Person.builder().nombre("Andres").apellido("Rivas").build();
        return Estudiante.builder().idEstudiante(6L).persona(persona).build();
    }

    @Test
    @DisplayName("notificarAsistencia no propaga si la consulta de vinculos falla")
    void notificarAsistencia_no_propaga_fallo_de_repositorio() {
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(anyLong()))
                .thenThrow(new DataIntegrityViolationException("vinculo inconsistente"));

        assertThatCode(() -> notificacionService.notifyAttendance(estudianteValido(), "PRESENTE"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notificarLesion no propaga si la consulta de vinculos falla")
    void notificarLesion_no_propaga_fallo_de_repositorio() {
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(anyLong()))
                .thenThrow(new DataIntegrityViolationException("vinculo inconsistente"));

        assertThatCode(() -> notificacionService.notifyInjury(estudianteValido(), "esguince"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notificarAsistencia no propaga si el estudiante no tiene persona asociada")
    void notificarAsistencia_no_propaga_dato_incompleto() {
        Estudiante sinPersona = Estudiante.builder().idEstudiante(7L).build();

        assertThatCode(() -> notificacionService.notifyAttendance(sinPersona, "TARDE"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sin representantes vinculados no es error: no se crea ninguna notificacion")
    void sin_representantes_no_es_error() {
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(anyLong()))
                .thenReturn(List.of());

        assertThatCode(() -> notificacionService.notifyAttendance(estudianteValido(), "PRESENTE"))
                .doesNotThrowAnyException();
    }
    @Test
    @DisplayName("sin consentimiento vigente no se crea ninguna notificacion")
    void sin_consentimiento_no_se_notifica() {
        Estudiante estudiante = estudianteCon(7L);
        Guardian representante = representanteCon(3L);
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(7L))
                .thenReturn(List.of(vinculoDe(representante, estudiante)));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        eq(3L), eq(7L), anyString()))
                .thenReturn(Optional.empty());

        notificacionService.notifyAttendance(estudiante, "PRESENTE");

        verify(notificacionRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("con consentimiento vigente si se crea la notificacion")
    void con_consentimiento_se_notifica() {
        Estudiante estudiante = estudianteCon(7L);
        Guardian representante = representanteCon(3L);
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(7L))
                .thenReturn(List.of(vinculoDe(representante, estudiante)));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        3L, 7L, Consent.ALCANCE_NOTIFICACIONES_ASISTENCIA))
                .thenReturn(Optional.of(new Consent()));

        notificacionService.notifyAttendance(estudiante, "PRESENTE");

        verify(notificacionRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("el consentimiento de asistencia no habilita el de lesion")
    void el_alcance_no_se_mezcla() {
        Estudiante estudiante = estudianteCon(7L);
        Guardian representante = representanteCon(3L);
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(7L))
                .thenReturn(List.of(vinculoDe(representante, estudiante)));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        eq(3L), eq(7L), anyString()))
                .thenReturn(Optional.empty());

        notificacionService.notifyInjury(estudiante, "esguince");

        verify(notificacionRepository, never()).save(any(Notification.class));
    }

    private Estudiante estudianteCon(Long id) {
        Person persona = Person.builder().nombre("Juan").apellido("Perez").build();
        return Estudiante.builder().idEstudiante(id).persona(persona).build();
    }

    private Guardian representanteCon(Long id) {
        return Guardian.builder().idRepresentante(id).build();
    }

    private GuardianStudent vinculoDe(Guardian r, Estudiante e) {
        return GuardianStudent.builder().representante(r).estudiante(e).build();
    }
}
