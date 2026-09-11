package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uteq.backend.common.validation.NationalIdValidator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-49 / hallazgo H-01: la cédula es opcional; cuando viene, debe pasar el
 * dígito verificador ecuatoriano.
 */
class NationalIdValidatorTest {

    private final NationalIdValidator validator = new NationalIdValidator();

    @Test
    @DisplayName("ausente o en blanco es válido (la cédula es opcional)")
    void opcional() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("cédulas con dígito verificador correcto son válidas")
    @ValueSource(strings = {"0912345675", "0923456784", "1711223345", "1300556675", "1746553211"})
    void validas(String cedula) {
        assertThat(validator.isValid(cedula, null)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("dígito verificador incorrecto, formato o provincia inválidos se rechazan")
    @ValueSource(strings = {
            "0912345678",   // dígito verificador incorrecto
            "0000000000",   // provincia 00
            "4000000001",   // provincia 40 inexistente
            "1111111111",   // dígito verificador incorrecto
            "091234567",    // 9 dígitos
            "09123456750",  // 11 dígitos
            "09a2345675",   // no numérica
            "0962345675"    // tercer dígito 6 (>= 6)
    })
    void invalidas(String cedula) {
        assertThat(validator.isValid(cedula, null)).isFalse();
    }
}
