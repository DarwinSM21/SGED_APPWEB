package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uteq.backend.academico.representante.entity.Consentimiento;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.evaluacion.entity.Alineacion;
import org.uteq.backend.deportivo.evaluacion.entity.AlineacionJugador;
import org.uteq.backend.seguridad.audit.entity.AuditLog;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de las devoluciones de ciclo de vida JPA ({@code @PrePersist} /
 * {@code @PreUpdate}) de las entidades. Los tests unitarios de servicio usan
 * mocks de repositorio, así que nunca disparan estas devoluciones; se
 * invocan aquí por reflexión (son {@code protected} / package-private) y se
 * verifica su efecto: sellado de marcas de tiempo, valores por defecto y
 * normalización de campos.
 */
class EntidadCicloVidaTest {

    private static void invocar(Object entidad, String metodo) throws Exception {
        Method m = entidad.getClass().getDeclaredMethod(metodo);
        m.setAccessible(true);
        m.invoke(entidad);
    }

    @Test
    @DisplayName("UserAccount.onCreate(): sella timestamps, activo por defecto y normaliza el username")
    void usuario_onCreate() throws Exception {
        UserAccount u = new UserAccount();
        u.setUsername("  DarwinSM21  ");
        invocar(u, "onCreate");

        assertThat(u.getCreatedAt()).isNotNull();
        assertThat(u.getUpdatedAt()).isNotNull();
        assertThat(u.getActivo()).isTrue();
        assertThat(u.getUsername()).isEqualTo("darwinsm21");
    }

    @Test
    @DisplayName("UserAccount.onCreate(): respeta activo si ya viene seteado")
    void usuario_onCreate_respeta_activo() throws Exception {
        UserAccount u = new UserAccount();
        u.setActivo(false);
        invocar(u, "onCreate");
        assertThat(u.getActivo()).isFalse();
    }

    @Test
    @DisplayName("UserAccount.onUpdate(): refresca updatedAt y renormaliza; username null no rompe")
    void usuario_onUpdate() throws Exception {
        UserAccount u = new UserAccount();
        invocar(u, "onUpdate");
        assertThat(u.getUpdatedAt()).isNotNull();
        assertThat(u.getUsername()).isNull();

        u.setUsername("PEPE");
        invocar(u, "onUpdate");
        assertThat(u.getUsername()).isEqualTo("pepe");
    }

    @Test
    @DisplayName("Person.onCreate()/onUpdate(): timestamps y activo por defecto")
    void persona_ciclo_vida() throws Exception {
        Person p = new Person();
        invocar(p, "onCreate");
        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getUpdatedAt()).isNotNull();
        assertThat(p.getActivo()).isTrue();

        p.setActivo(false);
        invocar(p, "onUpdate");
        assertThat(p.getActivo()).isFalse();
        assertThat(p.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Entrenador.onCreate()/onUpdate(): timestamps y activo por defecto")
    void entrenador_ciclo_vida() throws Exception {
        Entrenador e = new Entrenador();
        invocar(e, "onCreate");
        assertThat(e.getCreatedAt()).isNotNull();
        assertThat(e.getUpdatedAt()).isNotNull();
        assertThat(e.getActivo()).isTrue();

        invocar(e, "onUpdate");
        assertThat(e.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("AuditLog.onCreate(): fija la fecha solo si viene nula")
    void auditoria_onCreate() throws Exception {
        AuditLog a = new AuditLog();
        invocar(a, "onCreate");
        assertThat(a.getFecha()).isNotNull();

        OffsetDateTime fija = OffsetDateTime.now().minusDays(3);
        AuditLog b = new AuditLog();
        b.setFecha(fija);
        invocar(b, "onCreate");
        assertThat(b.getFecha()).isEqualTo(fija);
    }

    @Test
    @DisplayName("Consentimiento.onCreate(): fija otorgadoEn solo si viene nulo")
    void consentimiento_onCreate() throws Exception {
        Consentimiento c = new Consentimiento();
        invocar(c, "onCreate");
        assertThat(c.getOtorgadoEn()).isNotNull();

        OffsetDateTime fija = OffsetDateTime.now().minusHours(5);
        Consentimiento d = new Consentimiento();
        d.setOtorgadoEn(fija);
        invocar(d, "onCreate");
        assertThat(d.getOtorgadoEn()).isEqualTo(fija);
    }

    @Test
    @DisplayName("Alineacion.alCrear()/alActualizar(): sella creadoEn y actualizadoEn")
    void alineacion_ciclo_vida() throws Exception {
        Alineacion a = new Alineacion();
        invocar(a, "alCrear");
        assertThat(a.getCreadoEn()).isNotNull();
        assertThat(a.getActualizadoEn()).isEqualTo(a.getCreadoEn());

        invocar(a, "alActualizar");
        assertThat(a.getActualizadoEn()).isNotNull();
    }

    @Test
    @DisplayName("AlineacionJugador.alCrear(): sella creadoEn")
    void alineacionJugador_alCrear() throws Exception {
        AlineacionJugador j = new AlineacionJugador();
        invocar(j, "alCrear");
        assertThat(j.getCreadoEn()).isNotNull();
    }
}
