package org.uteq.backend.seguridad.auth.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación por defecto de {@link PasswordResetMailer}: no envía correo,
 * escribe el enlace de restablecimiento en la bitácora del backend. Activa
 * mientras {@code mail.enabled} sea {@code false} o no esté definida, lo que
 * permite que {@code make up}, la CI y una demo sin conexión levanten el
 * sistema completo sin credenciales de correo (misma filosofía que el
 * módulo de IA).
 */
@Component
@ConditionalOnProperty(name = "mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPasswordResetMailer implements PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMailer.class);

    /**
     * @param correo correo destinatario, solo para el mensaje de log
     * @param url enlace de restablecimiento, escrito en el log en vez de enviarse
     */
    @Override
    public void sendLink(String correo, String url) {
        log.warn("PWRESET (mail.enabled=false) enlace de restablecimiento para {}: {}", correo, url);
    }
}
