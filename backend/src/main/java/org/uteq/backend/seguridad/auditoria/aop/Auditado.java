package org.uteq.backend.seguridad.auditoria.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditado {
    String accion();

    String entidad();

    String idSpel() default "";

    String descripcionSpel() default "";
}
