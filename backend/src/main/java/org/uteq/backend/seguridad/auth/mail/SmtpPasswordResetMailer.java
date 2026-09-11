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
 * Envía el enlace de restablecimiento por SMTP (RF-37 / RNF-15). Activa solo
 * con {@code mail.enabled=true}; la configuración del proveedor está en
 * {@code spring.mail.*}.
 *
 * <p>Un fallo del proveedor —503, timeout, credenciales caducadas— se
 * registra pero <em>no se propaga</em>: la solicitud de restablecimiento
 * debe responder igual exista o no la cuenta y llegue o no el correo, para
 * no convertir el resultado del envío en un oráculo. El usuario siempre
 * puede volver a solicitarlo.
 */
@Component
@ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SmtpPasswordResetMailer implements PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetMailer.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String remitente;

    @Value("${mail.reset-token-ttl-minutes:30}")
    private int minutosVigencia;

    /**
     * @param correo correo destinatario del enlace de restablecimiento
     * @param url enlace de restablecimiento de un solo uso
     */
    @Override
    public void sendLink(String correo, String url) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(correo);
            helper.setSubject("Restablece tu contraseña de SGED");
            helper.setText(htmlBody(url), true);
            mailSender.send(mensaje);
            log.info("PWRESET correo de restablecimiento enviado a {}", correo);
        } catch (Exception e) {
            log.error("PWRESET no se pudo enviar el correo de restablecimiento a {}: {}",
                    correo, e.getMessage());
        }
    }

    private String htmlBody(String url) {
        return """
                <p>Recibimos una solicitud para restablecer tu contraseña de SGED.</p>
                <p><a href="%s">Elegir una contraseña nueva</a></p>
                <p>El enlace vence en %d minutos y solo puede usarse una vez.
                Si no lo pediste, puedes ignorar este mensaje: tu contraseña no cambia.</p>
                """.formatted(url, minutosVigencia);
    }
}
