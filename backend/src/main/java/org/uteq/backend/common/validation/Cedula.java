package org.uteq.backend.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * RF-49 / hallazgo H-01 de {@code docs/etica/ETHICS.md}: la cédula es un dato
 * <b>opcional</b>; cuando se proporciona debe ser una cédula ecuatoriana
 * válida (10 dígitos, provincia 01–24 ó 30, y dígito verificador correcto).
 *
 * <p>Un valor {@code null} o en blanco se considera válido —el requisito es la
 * opcionalidad—. La unicidad, cuando hay valor, la garantiza el índice único
 * parcial de la migración {@code V26}.
 */
@Documented
@Constraint(validatedBy = CedulaValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface Cedula {
    String message() default "La cédula ecuatoriana no es válida (dígito verificador incorrecto o provincia fuera de rango)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
