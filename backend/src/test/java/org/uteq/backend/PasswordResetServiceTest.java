package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.common.exception.ApiException;
import org.uteq.backend.seguridad.audit.service.AuditService;
import org.uteq.backend.seguridad.auth.PasswordPolicy;
import org.uteq.backend.seguridad.auth.PasswordResetTokenStore;
import org.uteq.backend.seguridad.auth.mail.PasswordResetMailer;
import org.uteq.backend.seguridad.auth.security.SessionEpochService;
import org.uteq.backend.seguridad.auth.service.PasswordResetService;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserAccountRepository usuarioRepository;
    @Mock private PersonRepository personaRepository;
    @Mock private PasswordResetTokenStore tokenStore;
    @Mock private PasswordResetMailer mailer;
    @Mock private SessionEpochService sessionEpochService;
    @Spy private PasswordPolicy passwordPolicy = new PasswordPolicy();
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    @InjectMocks private PasswordResetService service;

    private UserAccount ana;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "urlBase", "https://sged.test/#/restablecer");
        ReflectionTestUtils.setField(service, "ttlMinutos", 30L);

        Person persona = Person.builder().idPersona(1L).correo("ana@x.com").correoVerificado(true).build();
        ana = UserAccount.builder()
                .idUsuario(7L).username("ana.torres").persona(persona).activo(true)
                .password_Hash("hash-viejo").build();
    }

    @Test
    @DisplayName("solicitar por username: guarda token con TTL y envia el enlace al correo")
    void solicitar_por_username() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana.torres")).thenReturn(Optional.of(ana));

        service.solicitar("ana.torres");

        verify(tokenStore).guardar(eq("ana.torres"), anyString(), eq(Duration.ofMinutes(30)));
        verify(mailer).enviarEnlace(eq("ana@x.com"), contains("token="));
    }

    @Test
    @DisplayName("solicitar por correo cuando no es un username")
    void solicitar_por_correo() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana@x.com")).thenReturn(Optional.empty());
        when(personaRepository.findByCorreo("ana@x.com")).thenReturn(Optional.of(ana.getPersona()));
        when(usuarioRepository.findByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(Optional.of(ana));

        service.solicitar("ana@x.com");

        verify(tokenStore).guardar(eq("ana.torres"), anyString(), eq(Duration.ofMinutes(30)));
        verify(mailer).enviarEnlace(eq("ana@x.com"), contains("token="));
    }

    @Test
    @DisplayName("solicitar con identificador desconocido: no toca token ni mailer, no lanza")
    void solicitar_desconocido_es_no_op() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("nadie")).thenReturn(Optional.empty());
        when(personaRepository.findByCorreo("nadie")).thenReturn(Optional.empty());

        assertThatCode(() -> service.solicitar("nadie")).doesNotThrowAnyException();

        verify(tokenStore, never()).guardar(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
        verify(mailer, never()).enviarEnlace(anyString(), anyString());
    }

    @Test
    @DisplayName("RNF-26: solicitar cuando el correo no esta verificado: no guarda token ni envia enlace")
    void solicitar_correo_no_verificado_no_envia() {
        ana.getPersona().setCorreoVerificado(false);
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana.torres")).thenReturn(Optional.of(ana));

        service.solicitar("ana.torres");

        verify(tokenStore, never()).guardar(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
        verify(mailer, never()).enviarEnlace(anyString(), anyString());
    }

    @Test
    @DisplayName("solicitar con identificador en blanco o nulo: no op")
    void solicitar_en_blanco_es_no_op() {
        service.solicitar("   ");
        service.solicitar(null);

        verify(mailer, never()).enviarEnlace(anyString(), anyString());
    }

    @Test
    @DisplayName("restablecer con token valido: cambia el hash, consume el token y marca la epoca")
    void restablecer_ok() {
        when(tokenStore.resolver("tok")).thenReturn(Optional.of("ana.torres"));
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana.torres")).thenReturn(Optional.of(ana));
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-nuevo");

        service.restablecer("tok", "clave1234");

        assertThat(ana.getPassword_Hash()).isEqualTo("hash-nuevo");
        verify(usuarioRepository).save(ana);
        verify(tokenStore).consumir("tok");
        verify(sessionEpochService).marcar("ana.torres");
        verify(auditService).recordEvent(eq("PWRESET_COMPLETADO"), eq("Usuario"), eq(7L), anyString());
    }

    @Test
    @DisplayName("restablecer con token inexistente o ya consumido: 400 y no cambia nada")
    void restablecer_token_invalido() {
        when(tokenStore.resolver("tok")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restablecer("tok", "clave1234"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(400));

        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(sessionEpochService, never()).marcar(anyString());
    }

    @Test
    @DisplayName("restablecer con contrasena debil: 422 y no consume el token")
    void restablecer_contrasena_debil() {
        when(tokenStore.resolver("tok")).thenReturn(Optional.of("ana.torres"));

        assertThatThrownBy(() -> service.restablecer("tok", "corta1"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(422));

        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(tokenStore, never()).consumir(anyString());
        assertThat(ana.getPassword_Hash()).isEqualTo("hash-viejo");
    }

    @Test
    @DisplayName("restablecer cuando el usuario ya no esta activo: 400")
    void restablecer_usuario_inactivo() {
        when(tokenStore.resolver("tok")).thenReturn(Optional.of("ana.torres"));
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana.torres")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restablecer("tok", "clave1234"))
                .isInstanceOf(ApiException.class);

        verify(tokenStore, never()).consumir(anyString());
    }
}
