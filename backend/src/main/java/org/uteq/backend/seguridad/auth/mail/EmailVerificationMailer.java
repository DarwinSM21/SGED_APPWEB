package org.uteq.backend.seguridad.auth.mail;

/**
 * Entrega al titular el enlace de confirmación de su correo (RNF-26 / H-09).
 *
 * <p>Mismo esquema que {@link PasswordResetMailer}: hay exactamente una
 * implementación activa según {@code mail.enabled}.
 * {@link SmtpEmailVerificationMailer} envía un correo real cuando vale
 * {@code true}; {@link LoggingEmailVerificationMailer} —la de por defecto—
 * solo escribe el enlace en la bitácora, de modo que el sistema funciona
 * completo sin ningún proveedor de correo configurado.
 */
public interface EmailVerificationMailer {

    /**
     * Hace llegar el enlace de confirmación al correo indicado.
     *
     * @param correo dirección a confirmar
     * @param url    enlace absoluto de confirmación, con el token ya incluido
     *               como parámetro de consulta
     */
    void sendConfirmation(String correo, String url);
}
