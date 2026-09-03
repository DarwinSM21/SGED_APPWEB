package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.representante.entity.Consentimiento;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.evaluacion.entity.EvaluacionDiaria;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.partido.entity.Partido;

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
    @DisplayName("Consentimiento.estaVigente(): vigente mientras no se revoque")
    void consentimiento_estaVigente() {
        Consentimiento c = Consentimiento.builder().build();
        assertThat(c.estaVigente()).isTrue();

        c.setRevocadoEn(OffsetDateTime.now());
        assertThat(c.estaVigente()).isFalse();
    }

    @Test
    @DisplayName("Pago.estaVigente(): deja de estarlo al anularse")
    void pago_estaVigente() {
        Pago p = Pago.builder().build();
        assertThat(p.estaVigente()).isTrue();

        p.setAnuladoEn(OffsetDateTime.now());
        assertThat(p.estaVigente()).isFalse();
    }

    @Test
    @DisplayName("Lesion.estaActiva(): activa hasta que hay fecha de alta")
    void lesion_estaActiva() {
        Lesion l = new Lesion();
        assertThat(l.estaActiva()).isTrue();

        l.setFechaAlta(java.time.LocalDate.now());
        assertThat(l.estaActiva()).isFalse();
    }

    @Test
    @DisplayName("Asistencia.habilitaEvaluacion(): solo PRESENTE o TARDE habilitan calificar")
    void asistencia_habilitaEvaluacion() {
        Asistencia a = new Asistencia();

        a.setEstado(Asistencia.ESTADO_PRESENTE);
        assertThat(a.habilitaEvaluacion()).isTrue();

        a.setEstado(Asistencia.ESTADO_TARDE);
        assertThat(a.habilitaEvaluacion()).isTrue();

        a.setEstado(Asistencia.ESTADO_AUSENTE);
        assertThat(a.habilitaEvaluacion()).isFalse();

        a.setEstado(Asistencia.ESTADO_JUSTIFICADO);
        assertThat(a.habilitaEvaluacion()).isFalse();
    }

    @Test
    @DisplayName("EvaluacionDiaria.estaFinalizada(): true solo en estado FINALIZADA")
    void evaluacion_estaFinalizada() {
        EvaluacionDiaria e = new EvaluacionDiaria();
        assertThat(e.estaFinalizada()).isFalse();

        e.setEstado(EvaluacionDiaria.FINALIZADA);
        assertThat(e.estaFinalizada()).isTrue();
    }

    @Test
    @DisplayName("Partido.tieneResultado() / estaCerrado(): ambas ramas")
    void partido_predicados() {
        Partido p = Partido.builder().build();
        assertThat(p.tieneResultado()).isFalse();
        assertThat(p.estaCerrado()).isFalse();

        p.setGolesFavor((short) 2);
        assertThat(p.tieneResultado()).isFalse();
        p.setGolesContra((short) 1);
        assertThat(p.tieneResultado()).isTrue();

        p.setCerrado(true);
        assertThat(p.estaCerrado()).isTrue();
        p.setCerrado(false);
        assertThat(p.estaCerrado()).isFalse();
    }
}
