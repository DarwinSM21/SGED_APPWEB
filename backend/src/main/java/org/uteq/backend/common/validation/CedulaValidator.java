package org.uteq.backend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida el dígito verificador de una cédula ecuatoriana (algoritmo módulo 10
 * para personas naturales). Un valor ausente o en blanco es válido: la cédula
 * es opcional (RF-49 / H-01).
 */
public class CedulaValidator implements ConstraintValidator<Cedula, String> {

    private static final int[] COEFICIENTES = {2, 1, 2, 1, 2, 1, 2, 1, 2};

    /**
     * @param valor cédula a validar; {@code null} o en blanco se considera válido (es opcional)
     * @param context contexto de validación de Bean Validation, sin uso en esta implementación
     * @return {@code true} si el valor está ausente o es una cédula ecuatoriana válida
     */
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        if (valor == null || valor.isBlank()) {
            return true; // opcional
        }
        String cedula = valor.trim();
        if (cedula.length() != 10 || !cedula.chars().allMatch(Character::isDigit)) {
            return false;
        }

        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if ((provincia < 1 || provincia > 24) && provincia != 30) {
            return false;
        }
        // Tercer dígito: 0–5 para personas naturales.
        if (Character.getNumericValue(cedula.charAt(2)) >= 6) {
            return false;
        }

        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int producto = Character.getNumericValue(cedula.charAt(i)) * COEFICIENTES[i];
            if (producto >= 10) {
                producto -= 9;
            }
            suma += producto;
        }
        int digitoVerificador = (10 - (suma % 10)) % 10;
        return digitoVerificador == Character.getNumericValue(cedula.charAt(9));
    }
}
