package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.representante.repository.NotificacionRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.academico.representante.service.NotificacionService;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.uteq.backend.academico.representante.entity.Consentimiento;
import org.uteq.backend.academico.representante.entity.Notificacion;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.entity.RepresentanteEstudiante;
import org.uteq.backend.academico.representante.repository.ConsentimientoRepository;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private RepresentanteEstudianteRepository vinculoRepository;
    @Mock private RepresentanteRepository representanteRepository;
    @Mock private ConsentimientoRepository consentimientoRepository;

    @InjectMocks
    private NotificacionService notificacionService;

    private Estudiante estudianteValido() {
        Persona persona = Persona.builder().nombre("Andres").apellido("Rivas").build();
        return Estudiante.builder().idEstudiante(6L).persona(persona).build();
    }

    @Test
    @DisplayName("notificarAsistencia no propaga si la consulta de vinculos falla")
    void notificarAsistencia_no_propaga_fallo_de_repositorio() {
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(anyLong()))
                .thenThrow(new DataIntegrityViolationException("vinculo inconsistente"));

        assertThatCode(() -> notificacionService.notificarAsistencia(estudianteValido(), "PRESENTE"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notificarLesion no propaga si la consulta de vinculos falla")
    void notificarLesion_no_propaga_fallo_de_repositorio() {
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(anyLong()))
                .thenThrow(new DataIntegrityViolationException("vinculo inconsistente"));

        assertThatCode(() -> notificacionService.notificarLesion(estudianteValido(), "esguince"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notificarAsistencia no propaga si el estudiante no tiene persona asociada")
    void notificarAsistencia_no_propaga_dato_incompleto() {
        Estudiante sinPersona = Estudiante.builder().idEstudiante(7L).build();

        assertThatCode(() -> notificacionService.notificarAsistencia(sinPersona, "TARDE"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sin representantes vinculados no es error: no se crea ninguna notificacion")
    void sin_representantes_no_es_error() {
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(anyLong()))
                .thenReturn(List.of());

        assertThatCode(() -> notificacionService.notificarAsistencia(estudianteValido(), "PRESENTE"))
                .doesNotThrowAnyException();
    }
    @Test
    @DisplayName("sin consentimiento vigente no se crea ninguna notificacion")
    void sin_consentimiento_no_se_notifica() {
        Estudiante estudiante = estudianteCon(7L);
        Representante representante = representanteCon(3L);
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(7L))
                .thenReturn(List.of(vinculoDe(representante, estudiante)));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        eq(3L), eq(7L), anyString()))
                .thenReturn(Optional.empty());

        notificacionService.notificarAsistencia(estudiante, "PRESENTE");

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("con consentimiento vigente si se crea la notificacion")
    void con_consentimiento_se_notifica() {
        Estudiante estudiante = estudianteCon(7L);
        Representante representante = representanteCon(3L);
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(7L))
                .thenReturn(List.of(vinculoDe(representante, estudiante)));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        3L, 7L, Consentimiento.ALCANCE_NOTIFICACIONES_ASISTENCIA))
                .thenReturn(Optional.of(new Consentimiento()));

        notificacionService.notificarAsistencia(estudiante, "PRESENTE");

        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("el consentimiento de asistencia no habilita el de lesion")
    void el_alcance_no_se_mezcla() {
        Estudiante estudiante = estudianteCon(7L);
        Representante representante = representanteCon(3L);
        when(vinculoRepository.findByEstudiante_IdEstudianteAndActivoTrue(7L))
                .thenReturn(List.of(vinculoDe(representante, estudiante)));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        eq(3L), eq(7L), anyString()))
                .thenReturn(Optional.empty());

        notificacionService.notificarLesion(estudiante, "esguince");

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    private Estudiante estudianteCon(Long id) {
        Persona persona = Persona.builder().nombre("Juan").apellido("Perez").build();
        return Estudiante.builder().idEstudiante(id).persona(persona).build();
    }

    private Representante representanteCon(Long id) {
        return Representante.builder().idRepresentante(id).build();
    }

    private RepresentanteEstudiante vinculoDe(Representante r, Estudiante e) {
        return RepresentanteEstudiante.builder().representante(r).estudiante(e).build();
    }
}
