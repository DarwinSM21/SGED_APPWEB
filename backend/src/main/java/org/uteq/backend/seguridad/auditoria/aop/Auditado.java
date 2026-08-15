package org.uteq.backend.seguridad.auditoria.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un metodo de servicio cuya ejecucion exitosa debe quedar
 * registrada en seguridad.auditoria. Se usa en vez de un pointcut por
 * nombre de metodo porque los servicios de negocio del proyecto usan
 * verbos inconsistentes (registrar/crear/editar/eliminar/darDeAlta/...)
 * y devuelven tipos con campos de id no uniformes (idEstudiante,
 * idPersona, idUsuario, ...): la anotacion deja explicito, por metodo,
 * como extraer ese id.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditado {

    /** Ej. "CREAR", "EDITAR", "ELIMINAR". */
    String accion();

    /** Nombre simple de la entidad afectada, ej. "Lesion", "Pago". */
    String entidad();

    /**
     * Expresion SpEL evaluada sobre el resultado del metodo (#result) o sus
     * argumentos posicionales (#p0, #p1, ...) para obtener el id de la fila
     * afectada. Vacio si el metodo no tiene un id unico que registrar.
     */
    String idSpel() default "";

    /**
     * Expresion SpEL para una descripcion legible personalizada. Si esta
     * vacia, se arma una plantilla generica ("{usuario} {accion} {entidad}
     * #{id}") en el aspecto.
     */
    String descripcionSpel() default "";
}
