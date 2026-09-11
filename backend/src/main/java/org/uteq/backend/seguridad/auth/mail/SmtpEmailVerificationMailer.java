package org.uteq.backend.seguridad.auth.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envía el enlace de confirmación de correo por SMTP (RNF-26 / RNF-15). Activa
 * solo con {@code mail.enabled=true}; la configuración del proveedor está en
 * {@code spring.mail.*}.
 *
 * <p>Un fallo del proveedor se registra pero <em>no se propaga</em>: el alta de
 * la persona no debe abortar porque el correo de confirmación no salga. La
 * persona queda sin verificar y puede pedir un enlace nuevo.
 */
@Component
@ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SmtpEmailVerificationMailer implements EmailVerificationMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailVerificationMailer.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String remitente;

    @Value("${mail.verify-token-ttl-hours:48}")
    private int horasVigencia;

    /**
     * @param correo correo destinatario del enlace de confirmación
     * @param url enlace de confirmación de un solo uso
     */
    @Override
    public void sendConfirmation(String correo, String url) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(correo);
            helper.setSubject("Confirma tu correo en SGED");
            helper.setText(htmlBody(url), true);
            mailSender.send(mensaje);
            log.info("EMAILVERIFY correo de confirmación enviado a {}", correo);
        } catch (Exception e) {
            log.error("EMAILVERIFY no se pudo enviar el correo de confirmación a {}: {}",
                    correo, e.getMessage());
        }
    }

    private String htmlBody(String url) {
        return """
                <p>Se registró este correo en una cuenta de SGED.</p>
                <p><a href="%s">Confirmar que este correo es mío</a></p>
                <p>El enlace vence en %d horas. Mientras el correo no se confirme,
                no se podrá enviar a esta dirección el enlace de restablecimiento
                de contraseña.</p>
                """.formatted(url, horasVigencia);
    }
}
