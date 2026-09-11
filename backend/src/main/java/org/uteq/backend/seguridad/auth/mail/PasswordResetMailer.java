package org.uteq.backend.seguridad.auth.mail;

/**
 * Entrega al usuario el enlace de restablecimiento de contraseña (RF-37).
 *
 * <p>Hay exactamente una implementación activa, elegida por
 * {@code mail.enabled}: {@link SmtpPasswordResetMailer} envía un correo real
 * cuando vale {@code true}; {@link LoggingPasswordResetMailer} —la de por
 * defecto— solo escribe el enlace en la bitácora, de modo que el sistema
 * funciona completo sin ningún proveedor de correo configurado.
 */
public interface PasswordResetMailer {

    /**
     * Hace llegar el enlace al titular de la cuenta.
     *
     * @param correo dirección de correo registrada de la persona
     * @param url    enlace absoluto de restablecimiento, con el token ya
     *               incluido como parámetro de consulta
     */
    void sendLink(String correo, String url);
}
