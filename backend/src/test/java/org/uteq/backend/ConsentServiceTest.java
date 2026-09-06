package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.guardian.dto.ConsentDtos.ConsentResponse;
import org.uteq.backend.academico.guardian.dto.ConsentDtos.GrantConsentRequest;
import org.uteq.backend.academico.guardian.entity.Consent;
import org.uteq.backend.academico.guardian.entity.Guardian;
import org.uteq.backend.academico.guardian.repository.ConsentRepository;
import org.uteq.backend.academico.guardian.repository.GuardianRepository;
import org.uteq.backend.academico.guardian.service.ConsentService;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock private ConsentRepository consentimientoRepository;
    @Mock private GuardianRepository representanteRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UserAccountRepository usuarioRepository;

    @InjectMocks
    private ConsentService consentimientoService;

    private Guardian representante() {
        return Guardian.builder().idRepresentante(1L).build();
    }

    private Estudiante estudiante() {
        return Estudiante.builder().idEstudiante(10L).build();
    }

    @Test
    @DisplayName("otorgar lanza ResourceNotFoundException si el representante no existe")
    void otorgar_representante_inexistente_lanza_excepcion() {
        var request = new GrantConsentRequest(1L, 10L, Consent.ALCANCE_INFORMES);
        when(representanteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consentimientoService.grant(request, "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("otorgar rechaza si ya existe un consentimiento vigente con ese alcance")
    void otorgar_rechaza_consentimiento_duplicado() {
        var request = new GrantConsentRequest(1L, 10L, Consent.ALCANCE_INFORMES);
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representante()));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante()));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        1L, 10L, Consent.ALCANCE_INFORMES))
                .thenReturn(Optional.of(Consent.builder().idConsentimiento(5L).build()));

        assertThatThrownBy(() -> consentimientoService.grant(request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    @DisplayName("otorgar registra quien lo otorgo y queda vigente")
    void otorgar_persiste_consentimiento_vigente() {
        var request = new GrantConsentRequest(1L, 10L, Consent.ALCANCE_INFORMES);
        when(representanteRepository.findById(1L)).thenReturn(Optional.of(representante()));
        when(estudianteRepository.findById(10L)).thenReturn(Optional.of(estudiante()));
        when(consentimientoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndAlcanceAndRevocadoEnIsNull(
                        1L, 10L, Consent.ALCANCE_INFORMES))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByUsername("admin")).thenReturn(
                Optional.of(UserAccount.builder().idUsuario(99L).username("admin").build()));
        when(consentimientoRepository.save(any(Consent.class))).thenAnswer(inv -> {
            Consent c = inv.getArgument(0);
            c.setIdConsentimiento(7L);
            return c;
        });

        ConsentResponse resultado = consentimientoService.grant(request, "admin");

        assertThat(resultado.idConsentimiento()).isEqualTo(7L);
        assertThat(resultado.vigente()).isTrue();
        assertThat(resultado.registradoPorUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("revocar marca revocadoEn y quien lo revoco")
    void revocar_marca_revocado() {
        Consent existente = Consent.builder()
                .idConsentimiento(7L)
                .representante(representante())
                .estudiante(estudiante())
                .alcance(Consent.ALCANCE_INFORMES)
                .otorgadoEn(OffsetDateTime.now().minusDays(1))
                .build();
        when(consentimientoRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByUsername("admin")).thenReturn(
                Optional.of(UserAccount.builder().idUsuario(99L).username("admin").build()));
        when(consentimientoRepository.save(any(Consent.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentResponse resultado = consentimientoService.revoke(7L, "admin");

        assertThat(resultado.vigente()).isFalse();
        assertThat(existente.getRevocadoEn()).isNotNull();
    }

    @Test
    @DisplayName("revocar rechaza un consentimiento que ya estaba revocado")
    void revocar_rechaza_doble_revocacion() {
        Consent yaRevocado = Consent.builder()
                .idConsentimiento(7L)
                .representante(representante())
                .estudiante(estudiante())
                .revocadoEn(OffsetDateTime.now())
                .build();
        when(consentimientoRepository.findById(7L)).thenReturn(Optional.of(yaRevocado));

        assertThatThrownBy(() -> consentimientoService.revoke(7L, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("listarPorEstudiante delega en el repositorio y mapea vigencia")
    void listarPorEstudiante_devuelve_historial() {
        Consent c = Consent.builder()
                .idConsentimiento(1L).representante(representante()).estudiante(estudiante())
                .alcance(Consent.ALCANCE_INFORMES).otorgadoEn(OffsetDateTime.now())
                .build();
        when(consentimientoRepository.findByEstudiante_IdEstudianteOrderByOtorgadoEnDesc(10L))
                .thenReturn(List.of(c));

        List<ConsentResponse> resultado = consentimientoService.listByStudent(10L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).vigente()).isTrue();
    }
}
