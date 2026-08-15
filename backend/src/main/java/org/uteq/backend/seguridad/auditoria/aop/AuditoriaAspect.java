package org.uteq.backend.seguridad.auditoria.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.uteq.backend.seguridad.auditoria.service.AuditoriaService;

/**
 * Envuelve cada metodo anotado con @Auditado: lo ejecuta primero, y solo
 * si termina sin excepcion registra la fila de auditoria (una operacion
 * que fallo no se audita como si hubiera ocurrido). Cualquier error al
 * armar o guardar el registro se loguea sin afectar el valor de retorno
 * del metodo envuelto -- ver AuditoriaService.registrar.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final AuditoriaService auditoriaService;

    @Around("@annotation(auditado)")
    public Object auditar(ProceedingJoinPoint pjp, Auditado auditado) throws Throwable {
        Object resultado = pjp.proceed();
        try {
            StandardEvaluationContext contexto = new StandardEvaluationContext();
            contexto.setVariable("result", resultado);
            Object[] args = pjp.getArgs();
            for (int i = 0; i < args.length; i++) {
                contexto.setVariable("p" + i, args[i]);
            }

            Long entidadId = evaluarId(auditado.idSpel(), contexto);
            String descripcion = auditado.descripcionSpel().isBlank()
                    ? descripcionGenerica(auditado, entidadId)
                    : String.valueOf(evaluar(auditado.descripcionSpel(), contexto));

            auditoriaService.registrar(auditado.accion(), auditado.entidad(), entidadId, descripcion);
        } catch (Exception e) {
            log.error("No se pudo auditar la llamada a {}", pjp.getSignature(), e);
        }
        return resultado;
    }

    private Long evaluarId(String spel, StandardEvaluationContext contexto) {
        if (spel == null || spel.isBlank()) {
            return null;
        }
        Object valor = evaluar(spel, contexto);
        if (valor == null) {
            return null;
        }
        return valor instanceof Number n ? n.longValue() : Long.valueOf(valor.toString());
    }

    private Object evaluar(String spel, StandardEvaluationContext contexto) {
        Expression expresion = PARSER.parseExpression(spel);
        return expresion.getValue(contexto);
    }

    private String descripcionGenerica(Auditado auditado, Long entidadId) {
        String verbo = switch (auditado.accion()) {
            case "CREAR" -> "creó";
            case "EDITAR" -> "editó";
            case "ELIMINAR" -> "eliminó";
            default -> auditado.accion().toLowerCase();
        };
        return entidadId != null
                ? verbo + " " + auditado.entidad() + " #" + entidadId
                : verbo + " " + auditado.entidad();
    }
}
