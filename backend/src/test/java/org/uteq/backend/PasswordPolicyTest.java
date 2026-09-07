package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uteq.backend.common.exception.ApiException;
import org.uteq.backend.seguridad.auth.PasswordPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    @DisplayName("contraseña con letra y dígito, de 8+ y distinta del username: válida")
    void contrasena_valida() {
        assertThat(policy.problemas("clave1234", "ana.torres")).isEmpty();
        assertThatCode(() -> policy.validar("clave1234", "ana.torres")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("menos de 8 caracteres falla")
    void muy_corta() {
        assertThat(policy.problemas("clav12", null)).anyMatch(m -> m.contains("8 caracteres"));
    }

    @Test
    @DisplayName("sin dígito falla")
    void sin_digito() {
        assertThat(policy.problemas("solotexto", null)).anyMatch(m -> m.contains("dígito"));
    }

    @Test
    @DisplayName("sin letra falla")
    void sin_letra() {
        assertThat(policy.problemas("12345678", null)).anyMatch(m -> m.contains("letra"));
    }

    @Test
    @DisplayName("igual al username (ignorando mayúsculas) falla")
    void igual_al_username() {
        assertThat(policy.problemas("Admin123", "admin123")).anyMatch(m -> m.contains("nombre de usuario"));
    }

    @Test
    @DisplayName("más de 72 bytes falla")
    void demasiado_larga() {
        String larga = "a1".repeat(40); // 80 bytes
        assertThat(policy.problemas(larga, null)).anyMatch(m -> m.contains("72 bytes"));
    }

    @Test
    @DisplayName("null se trata como vacía y falla")
    void nula() {
        assertThat(policy.problemas(null, null)).isNotEmpty();
    }

    @Test
    @DisplayName("validar() lanza ApiException 422 con el detalle")
    void validar_lanza_422() {
        assertThatThrownBy(() -> policy.validar("x", null))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(422));
    }
}
