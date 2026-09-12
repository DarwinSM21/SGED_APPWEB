package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uteq.backend.academico.payment.entity.Payment;
import org.uteq.backend.academico.guardian.entity.Consent;
import org.uteq.backend.deportivo.attendance.entity.Attendance;
import org.uteq.backend.deportivo.evaluation.entity.DailyEvaluation;
import org.uteq.backend.deportivo.injury.entity.Injury;
import org.uteq.backend.deportivo.match.entity.Match;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba directa de la lógica escrita a mano en las entidades (los métodos
 * {@code @Transient} que no son getters/setters de Lombok). Cubre las dos
 * ramas de cada predicado, que es lo que JaCoCo mide una vez que
 * {@code lombok.config} excluye el código generado.
 */
class EntidadLogicaTest {

    @Test
    @DisplayName("Consent.isActive(): vigente mientras no se revoque")
    void consentimiento_estaVigente() {
        Consent c = Consent.builder().build();
        assertThat(c.isActive()).isTrue();

        c.setRevokedAt(OffsetDateTime.now());
        assertThat(c.isActive()).isFalse();
    }

    @Test
    @DisplayName("Payment.isActive(): deja de estarlo al anularse")
    void pago_estaVigente() {
        Payment p = Payment.builder().build();
        assertThat(p.isActive()).isTrue();

        p.setCanceledAt(OffsetDateTime.now());
        assertThat(p.isActive()).isFalse();
    }

    @Test
    @DisplayName("Injury.isActive(): activa hasta que hay fecha de alta")
    void lesion_estaActiva() {
        Injury l = new Injury();
        assertThat(l.isActive()).isTrue();

        l.setFechaAlta(java.time.LocalDate.now());
        assertThat(l.isActive()).isFalse();
    }

    @Test
    @DisplayName("Attendance.enablesEvaluation(): solo PRESENTE o TARDE habilitan calificar")
    void asistencia_habilitaEvaluacion() {
        Attendance a = new Attendance();

        a.setEstado(Attendance.ESTADO_PRESENTE);
        assertThat(a.enablesEvaluation()).isTrue();

        a.setEstado(Attendance.ESTADO_TARDE);
        assertThat(a.enablesEvaluation()).isTrue();

        a.setEstado(Attendance.ESTADO_AUSENTE);
        assertThat(a.enablesEvaluation()).isFalse();

        a.setEstado(Attendance.ESTADO_JUSTIFICADO);
        assertThat(a.enablesEvaluation()).isFalse();
    }

    @Test
    @DisplayName("DailyEvaluation.isFinished(): true solo en estado FINALIZADA")
    void evaluacion_estaFinalizada() {
        DailyEvaluation e = new DailyEvaluation();
        assertThat(e.isFinished()).isFalse();

        e.setEstado(DailyEvaluation.FINALIZADA);
        assertThat(e.isFinished()).isTrue();
    }

    @Test
    @DisplayName("Match.hasResult() / isClosed(): ambas ramas")
    void partido_predicados() {
        Match p = Match.builder().build();
        assertThat(p.hasResult()).isFalse();
        assertThat(p.isClosed()).isFalse();

        p.setGolesFavor((short) 2);
        assertThat(p.hasResult()).isFalse();
        p.setGolesContra((short) 1);
        assertThat(p.hasResult()).isTrue();

        p.setCerrado(true);
        assertThat(p.isClosed()).isTrue();
        p.setCerrado(false);
        assertThat(p.isClosed()).isFalse();
    }
}
