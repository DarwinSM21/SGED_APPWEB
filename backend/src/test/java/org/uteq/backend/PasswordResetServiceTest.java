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

        Person persona = Person.builder().id(1L).email("ana@x.com").emailVerified(true).build();
        ana = UserAccount.builder()
                .id(7L).username("ana.torres").person(persona).active(true)
                .passwordHash("hash-viejo").build();
    }

    @Test
    @DisplayName("solicitar por username: guarda token con TTL y envia el enlace al correo")
    void solicitar_por_username() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana.torres")).thenReturn(Optional.of(ana));

        service.request("ana.torres");

        verify(tokenStore).save(eq("ana.torres"), anyString(), eq(Duration.ofMinutes(30)));
        verify(mailer).sendLink(eq("ana@x.com"), contains("token="));
    }

    @Test
    @DisplayName("solicitar por correo cuando no es un username")
    void solicitar_por_correo() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana@x.com")).thenReturn(Optional.empty());
        when(personaRepository.findByCorreo("ana@x.com")).thenReturn(Optional.of(ana.getPerson()));
        when(usuarioRepository.findByPersona_IdPersonaAndActivoTrue(1L)).thenReturn(Optional.of(ana));

        service.request("ana@x.com");

        verify(tokenStore).save(eq("ana.torres"), anyString(), eq(Duration.ofMinutes(30)));
        verify(mailer).sendLink(eq("ana@x.com"), contains("token="));
    }

    @Test
    @DisplayName("solicitar con identificador desconocido: no toca token ni mailer, no lanza")
    void solicitar_desconocido_es_no_op() {
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("nadie")).thenReturn(Optional.empty());
        when(personaRepository.findByCorreo("nadie")).thenReturn(Optional.empty());

        assertThatCode(() -> service.request("nadie")).doesNotThrowAnyException();

        verify(tokenStore, never()).save(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
        verify(mailer, never()).sendLink(anyString(), anyString());
    }

    @Test
    @DisplayName("RNF-26: solicitar cuando el correo no esta verificado: no guarda token ni envia enlace")
    void solicitar_correo_no_verificado_no_envia() {
        ana.getPerson().setEmailVerified(false);
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana.torres")).thenReturn(Optional.of(ana));

        service.request("ana.torres");

        verify(tokenStore, never()).save(anyString(), anyString(), org.mockito.ArgumentMatchers.any());
        verify(mailer, never()).sendLink(anyString(), anyString());
    }

    @Test
    @DisplayName("solicitar con identificador en blanco o nulo: no op")
    void solicitar_en_blanco_es_no_op() {
        service.request("   ");
        service.request(null);

        verify(mailer, never()).sendLink(anyString(), anyString());
    }

    @Test
    @DisplayName("restablecer con token valido: cambia el hash, consume el token y marca la epoca")
    void restablecer_ok() {
        when(tokenStore.resolve("tok")).thenReturn(Optional.of("ana.torres"));
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana.torres")).thenReturn(Optional.of(ana));
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-nuevo");

        service.reset("tok", "clave1234");

        assertThat(ana.getPasswordHash()).isEqualTo("hash-nuevo");
        verify(usuarioRepository).save(ana);
        verify(tokenStore).consume("tok");
        verify(sessionEpochService).mark("ana.torres");
        verify(auditService).recordEvent(eq("PWRESET_COMPLETADO"), eq("Usuario"), eq(7L), anyString());
    }

    @Test
    @DisplayName("restablecer con token inexistente o ya consumido: 400 y no cambia nada")
    void restablecer_token_invalido() {
        when(tokenStore.resolve("tok")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset("tok", "clave1234"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(400));

        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(sessionEpochService, never()).mark(anyString());
    }

    @Test
    @DisplayName("restablecer con contrasena debil: 422 y no consume el token")
    void restablecer_contrasena_debil() {
        when(tokenStore.resolve("tok")).thenReturn(Optional.of("ana.torres"));

        assertThatThrownBy(() -> service.reset("tok", "corta1"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(422));

        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(tokenStore, never()).consume(anyString());
        assertThat(ana.getPasswordHash()).isEqualTo("hash-viejo");
    }

    @Test
    @DisplayName("restablecer cuando el usuario ya no esta activo: 400")
    void restablecer_usuario_inactivo() {
        when(tokenStore.resolve("tok")).thenReturn(Optional.of("ana.torres"));
        when(usuarioRepository.findByUsernameIgnoreCaseAndActivoTrue("ana.torres")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset("tok", "clave1234"))
                .isInstanceOf(ApiException.class);

        verify(tokenStore, never()).consume(anyString());
    }
}
