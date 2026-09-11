package org.uteq.backend.seguridad.auth.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación por defecto de {@link EmailVerificationMailer}: no envía
 * correo, escribe el enlace de confirmación en la bitácora del backend. Activa
 * mientras {@code mail.enabled} sea {@code false} o no esté definida (misma
 * filosofía que {@link LoggingPasswordResetMailer}).
 */
@Component
@ConditionalOnProperty(name = "mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailVerificationMailer implements EmailVerificationMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailVerificationMailer.class);

    @Override
    public void enviarConfirmacion(String correo, String url) {
        log.warn("EMAILVERIFY (mail.enabled=false) enlace de confirmación para {}: {}", correo, url);
    }
}
