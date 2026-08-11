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

/**
 * RF-22: el efecto de notificacion no debe poder tumbar el flujo principal.
 *
 * <p>Estas pruebas existen por un defecto real: {@code notificarAsistencia} y
 * {@code notificarLesion} se invocan dentro de la transaccion que guarda la
 * asistencia o la lesion. Si la notificacion propagaba una excepcion, Spring
 * marcaba la transaccion como {@code rollback-only} y se perdia el registro
 * principal --- un estudiante marcaba asistencia, fallaba el aviso al
 * representante, y la asistencia desaparecia---. El javadoc del servicio ya
 * prometia que esto no debia ocurrir, pero nada lo verificaba.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private RepresentanteEstudianteRepository vinculoRepository;
    @Mock private RepresentanteRepository representanteRepository;

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
        // Estado invalido pero alcanzable: al construir el mensaje se lee
        // persona.getNombre() y revienta con NullPointerException.
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
}
