package org.uteq.backend.seguridad.audit.aop;

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
import org.uteq.backend.seguridad.audit.service.AuditService;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final AuditService auditoriaService;

    /**
     * Envuelve cualquier método anotado con {@link Audited}: lo deja ejecutar
     * primero y, si termina sin lanzar, resuelve las expresiones SpEL de
     * {@code idSpel}/{@code descriptionSpel} contra el resultado y los
     * argumentos ({@code #result}, {@code #p0}, {@code #p1}, ...) y registra
     * el acto en la bitácora de auditoría.
     *
     * @param pjp punto de unión del método interceptado
     * @param auditado datos de la anotación {@link Audited} (acción, entity, expresiones)
     * @return el resultado del método interceptado, sin modificar
     * @throws Throwable la excepción que haya lanzado el método interceptado, propagada tal cual
     */
    @Around("@annotation(auditado)")
    public Object audit(ProceedingJoinPoint pjp, Audited auditado) throws Throwable {
        Object resultado = pjp.proceed();
        try {
            StandardEvaluationContext contexto = new StandardEvaluationContext();
            contexto.setVariable("result", resultado);
            Object[] args = pjp.getArgs();
            for (int i = 0; i < args.length; i++) {
                contexto.setVariable("p" + i, args[i]);
            }

            Long entityId = evaluateId(auditado.idSpel(), contexto);
            String descripcion = auditado.descriptionSpel().isBlank()
                    ? genericDescription(auditado, entityId)
                    : String.valueOf(evaluate(auditado.descriptionSpel(), contexto));

            auditoriaService.recordEvent(auditado.action(), auditado.entity(), entityId, descripcion);
        } catch (Exception e) {
            log.error("No se pudo auditar la llamada a {}", pjp.getSignature(), e);
        }
        return resultado;
    }

    private Long evaluateId(String spel, StandardEvaluationContext contexto) {
        if (spel == null || spel.isBlank()) {
            return null;
        }
        Object valor = evaluate(spel, contexto);
        if (valor == null) {
            return null;
        }
        return valor instanceof Number n ? n.longValue() : Long.valueOf(valor.toString());
    }

    private Object evaluate(String spel, StandardEvaluationContext contexto) {
        Expression expresion = PARSER.parseExpression(spel);
        return expresion.getValue(contexto);
    }

    private String genericDescription(Audited auditado, Long entityId) {
        String verbo = switch (auditado.action()) {
            case "CREAR" -> "creó";
            case "EDITAR" -> "editó";
            case "ELIMINAR" -> "eliminó";
            default -> auditado.action().toLowerCase();
        };
        return entityId != null
                ? verbo + " " + auditado.entity() + " #" + entityId
                : verbo + " " + auditado.entity();
    }
}
