package org.uteq.backend;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.seguridad.auth.mail.SmtpEmailVerificationMailer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailVerificationMailerTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks private SmtpEmailVerificationMailer mailer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailer, "remitente", "no-reply@sged.test");
        ReflectionTestUtils.setField(mailer, "horasVigencia", 48);
    }

    @Test
    @DisplayName("envia al correo indicado con el asunto y el enlace de confirmacion")
    void envia_correo_con_destinatario_asunto_y_enlace() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        mailer.sendConfirmation("ana@x.com", "https://sged.test/#/confirmar-correo?token=abc123");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage enviado = captor.getValue();
        assertThat(enviado.getAllRecipients()[0].toString()).isEqualTo("ana@x.com");
        assertThat(enviado.getSubject()).isEqualTo("Confirma tu correo en SGED");
        assertThat(enviado.getContent().toString()).contains("token=abc123");
    }

    @Test
    @DisplayName("un fallo del proveedor SMTP se registra pero no se propaga")
    void fallo_del_proveedor_no_se_propaga() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("proveedor caido")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> mailer.sendConfirmation("ana@x.com", "https://sged.test/x"))
                .doesNotThrowAnyException();
    }
}
