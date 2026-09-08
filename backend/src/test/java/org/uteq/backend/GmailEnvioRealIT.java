package org.uteq.backend;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Properties;

/**
 * Prueba de humo del envío real por SMTP (RF-37 / RNF-15). Comprueba que las
 * credenciales de {@code MAIL_*} del entorno sirven para autenticarse contra
 * Gmail y entregar un mensaje.
 *
 * <p><b>Desactivada por defecto:</b> solo corre si {@code MAIL_LIVE_TEST=true}
 * (así la CI y {@code mvn verify} normales la saltan). No usa el contexto de
 * Spring ni base de datos; toma las credenciales del entorno y nunca las
 * imprime.
 *
 * <p>Uso (desde {@code backend/}, con las variables ya en el entorno):
 * <pre>
 *   MAIL_LIVE_TEST=true ./mvnw test -Dtest=GmailEnvioRealIT
 * </pre>
 */
class GmailEnvioRealIT {

    @Test
    @EnabledIfEnvironmentVariable(named = "MAIL_LIVE_TEST", matches = "true")
    void envia_un_correo_de_prueba_por_gmail() throws Exception {
        String host = env("MAIL_HOST", "smtp.gmail.com");
        String port = env("MAIL_PORT", "587");
        String usuario = env("MAIL_USERNAME", null);
        String clave = env("MAIL_PASSWORD", null);
        String remitente = env("MAIL_FROM", usuario);
        String destino = env("MAIL_TEST_TO", usuario);

        if (usuario == null || clave == null) {
            throw new IllegalStateException(
                    "Faltan MAIL_USERNAME / MAIL_PASSWORD en el entorno.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, clave);
            }
        });

        MimeMessage mensaje = new MimeMessage(session);
        mensaje.setFrom(new InternetAddress(remitente));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
        mensaje.setSubject("SGED — prueba de envío (Fase 9, RF-37)");
        mensaje.setText("""
                Si recibes esto, el App Password de Gmail configurado en MAIL_PASSWORD
                funciona y el correo de recuperación de contraseña (RF-37) puede
                entregarse. Este mensaje lo genera la prueba GmailEnvioRealIT.""");

        Transport.send(mensaje);
        System.out.println("GmailEnvioRealIT: mensaje entregado al SMTP de " + host
                + " para " + destino + " — auth OK.");
    }

    private static String env(String nombre, String porDefecto) {
        String v = System.getenv(nombre);
        return (v == null || v.isBlank()) ? porDefecto : v.trim();
    }
}
