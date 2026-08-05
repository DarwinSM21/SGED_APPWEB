package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.dto.ConsentimientoDtos.ConsentimientoResponse;
import org.uteq.backend.academico.representante.dto.ConsentimientoDtos.OtorgarConsentimientoRequest;
import org.uteq.backend.academico.representante.entity.Consentimiento;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.repository.ConsentimientoRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.academico.representante.service.ConsentimientoService;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentimientoServiceTest {

    @Mock private ConsentimientoRepository consentimientoRepository;
    @Mock private RepresentanteRepository representanteRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ConsentimientoService consentimientoService;

    private Representante representante() {
        return Representante.builder().idRepresentante(1L).build();
    }

    private Estudiante estudiante() {
        return Estudiante.builder().idEstudiante(10L).build();
    }

    @Test
    @DisplayName("otorgar lanza RecursoNoEncontradoException si el representante no existe")
    void otorgar_representante_inexistente_lanza_excepcion() {
        var request = new OtorgarConsentimientoRequest(1L, 10L, Consentimiento.ALCANCE_INFORMES);
        when(representanteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consentimientoService.otorgar(request, "admin"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("otorgar rechaza si ya existe un consentimiento vigente con ese alcance")
    void otorgar_rechaza_consentimiento_duplicado() {
        var request = new OtorgarConsentimientoRequest(1L, 10L, Consentimiento.ALCANCE_INFORMES);
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representante()));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante()));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        1L, 10L, Consentimiento.ALCANCE_INFORMES))
                .thenReturn(Optional.of(Consentimiento.builder().idConsentimiento(5L).build()));

        assertThatThrownBy(() -> consentimientoService.otorgar(request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    @DisplayName("otorgar registra quien lo otorgo y queda vigente")
    void otorgar_persiste_consentimiento_vigente() {
        var request = new OtorgarConsentimientoRequest(1L, 10L, Consentimiento.ALCANCE_INFORMES);
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representante()));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante()));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        1L, 10L, Consentimiento.ALCANCE_INFORMES))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByUsername("admin")).thenReturn(
                Optional.of(Usuario.builder().idUsuario(99L).username("admin").build()));
        when(consentimientoRepository.save(any(Consentimiento.class))).thenAnswer(inv -> {
            Consentimiento c = inv.getArgument(0);
            c.setIdConsentimiento(7L);
            return c;
        });

        ConsentimientoResponse resultado = consentimientoService.otorgar(request, "admin");

        assertThat(resultado.idConsentimiento()).isEqualTo(7L);
        assertThat(resultado.vigente()).isTrue();
        assertThat(resultado.registradoPorUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("revocar marca revocadoEn y quien lo revoco")
    void revocar_marca_revocado() {
        Consentimiento existente = Consentimiento.builder()
                .idConsentimiento(7L)
                .representante(representante())
                .estudiante(estudiante())
                .alcance(Consentimiento.ALCANCE_INFORMES)
                .otorgadoEn(OffsetDateTime.now().minusDays(1))
                .build();
        when(consentimientoRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByUsername("admin")).thenReturn(
                Optional.of(Usuario.builder().idUsuario(99L).username("admin").build()));
        when(consentimientoRepository.save(any(Consentimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentimientoResponse resultado = consentimientoService.revocar(7L, "admin");

        assertThat(resultado.vigente()).isFalse();
        assertThat(existente.getRevocadoEn()).isNotNull();
    }

    @Test
    @DisplayName("revocar rechaza un consentimiento que ya estaba revocado")
    void revocar_rechaza_doble_revocacion() {
        Consentimiento yaRevocado = Consentimiento.builder()
                .idConsentimiento(7L)
                .representante(representante())
                .estudiante(estudiante())
                .revocadoEn(OffsetDateTime.now())
                .build();
        when(consentimientoRepository.findById(7L)).thenReturn(Optional.of(yaRevocado));

        assertThatThrownBy(() -> consentimientoService.revocar(7L, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("listarPorEstudiante delega en el repositorio y mapea vigencia")
    void listarPorEstudiante_devuelve_historial() {
        Consentimiento c = Consentimiento.builder()
                .idConsentimiento(1L).representante(representante()).estudiante(estudiante())
                .alcance(Consentimiento.ALCANCE_INFORMES).otorgadoEn(OffsetDateTime.now())
                .build();
        when(consentimientoRepository.findByEstudiante_IdEstudianteOrderByOtorgadoEnDesc(10L))
                .thenReturn(List.of(c));

        List<ConsentimientoResponse> resultado = consentimientoService.listarPorEstudiante(10L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).vigente()).isTrue();
    }
}
