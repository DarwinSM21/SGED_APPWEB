package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.common.exception.ApiException;
import org.uteq.backend.seguridad.audit.service.AuditService;
import org.uteq.backend.seguridad.auth.EmailVerificationTokenStore;
import org.uteq.backend.seguridad.auth.mail.EmailVerificationMailer;
import org.uteq.backend.seguridad.auth.service.EmailVerificationService;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private PersonRepository personaRepository;
    @Mock private EmailVerificationTokenStore tokenStore;
    @Mock private EmailVerificationMailer mailer;
    @Mock private AuditService auditService;

    @InjectMocks private EmailVerificationService service;

    private Person maria;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "urlBase", "https://sged.test/#/confirmar-correo");
        ReflectionTestUtils.setField(service, "ttlHoras", 48L);
        maria = Person.builder().id(3L).email("maria@x.com").emailVerified(false).build();
    }

    @Test
    @DisplayName("enviarConfirmacion guarda el token con el TTL y manda el enlace al correo")
    void enviarConfirmacion_ok() {
        service.sendConfirmation(maria);

        verify(tokenStore).save(eq(3L), anyString(), eq(Duration.ofHours(48)));
        verify(mailer).sendConfirmation(eq("maria@x.com"), contains("token="));
        verify(auditService).recordEvent(eq("EMAILVERIFY_SOLICITADO"), eq("Persona"), eq(3L), anyString());
    }

    @Test
    @DisplayName("enviarConfirmacion con persona sin id o sin correo: no hace nada")
    void enviarConfirmacion_persona_incompleta() {
        service.sendConfirmation(Person.builder().email("x@x.com").build());
        service.sendConfirmation(null);

        verify(tokenStore, never()).save(any(), anyString(), any());
        verify(mailer, never()).sendConfirmation(anyString(), anyString());
    }

    @Test
    @DisplayName("confirmar con token valido: marca el correo como verificado y consume el token")
    void confirmar_ok() {
        when(tokenStore.resolve("tok")).thenReturn(Optional.of(3L));
        when(personaRepository.findById(3L)).thenReturn(Optional.of(maria));

        service.confirm("tok");

        assertThat(maria.getEmailVerified()).isTrue();
        verify(personaRepository).save(maria);
        verify(tokenStore).consume("tok");
        verify(auditService).recordEvent(eq("EMAILVERIFY_CONFIRMADO"), eq("Persona"), eq(3L), anyString());
    }

    @Test
    @DisplayName("confirmar con token inexistente o consumido: 400 y no cambia nada")
    void confirmar_token_invalido() {
        when(tokenStore.resolve("tok")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm("tok"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(400));

        verify(personaRepository, never()).save(any());
        verify(tokenStore, never()).consume(anyString());
    }

    @Test
    @DisplayName("confirmar cuando la persona ya no existe: 400")
    void confirmar_persona_inexistente() {
        when(tokenStore.resolve("tok")).thenReturn(Optional.of(99L));
        when(personaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm("tok")).isInstanceOf(ApiException.class);

        verify(tokenStore, never()).consume(anyString());
    }
}
