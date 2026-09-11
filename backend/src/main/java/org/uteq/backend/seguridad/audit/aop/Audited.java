package org.uteq.backend.seguridad.audit.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de servicio cuyo resultado hay que registrar en la
 * bitácora de auditoría. Lo intercepta {@link AuditAspect}, que resuelve las
 * expresiones SpEL declaradas aquí una vez que el método ya se ejecutó.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    /** Verbo de la acción registrada (ej. {@code "CREAR"}, {@code "EDITAR"}, {@code "ELIMINAR"}). */
    String action();

    /** Nombre de la entity de dominio afectada (ej. {@code "Estudiante"}). */
    String entity();

    /** Expresión SpEL que resuelve el identificador de la entity afectada; vacío si no aplica. */
    String idSpel() default "";

    /** Expresión SpEL que arma la descripción legible del acto; vacío si no aplica. */
    String descriptionSpel() default "";
}
