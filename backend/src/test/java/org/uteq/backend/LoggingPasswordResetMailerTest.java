package org.uteq.backend;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.uteq.backend.seguridad.auth.mail.LoggingPasswordResetMailer;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingPasswordResetMailerTest {

    @Test
    void registra_el_enlace_y_el_correo_en_la_bitacora() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingPasswordResetMailer.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            new LoggingPasswordResetMailer().sendLink(
                    "ana@x.com", "https://sged.test/#/restablecer?token=abc123");

            assertThat(appender.list).anyMatch(e ->
                    e.getFormattedMessage().contains("ana@x.com")
                            && e.getFormattedMessage().contains("token=abc123"));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
