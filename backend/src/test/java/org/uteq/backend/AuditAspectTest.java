package org.uteq.backend;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.seguridad.audit.aop.Audited;
import org.uteq.backend.seguridad.audit.aop.AuditAspect;
import org.uteq.backend.seguridad.audit.service.AuditService;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {
    @Mock private AuditService auditoriaService;
    @Mock private ProceedingJoinPoint pjp;

    private AuditAspect aspecto;

    @BeforeEach
    void setUp() {
        aspecto = new AuditAspect(auditoriaService);
    }

    static class MetodosDeEjemplo {
        @Audited(accion = "CREAR", entidad = "Lesion", idSpel = "#result.idLesion")
        void conResultado() {
        }

        @Audited(accion = "ELIMINAR", entidad = "Estudiante", idSpel = "#p0")
        void conArgumentoPosicional(Long id) {
        }

        @Audited(accion = "CREAR", entidad = "Pago",
                descripcionSpel = "'creó ' + #result.size() + ' pago(s)'")
        void conDescripcionPersonalizada() {
        }
    }

    private Audited anotacionDe(String nombreMetodo) throws NoSuchMethodException {
        for (Method m : MetodosDeEjemplo.class.getDeclaredMethods()) {
            if (m.getName().equals(nombreMetodo)) {
                return m.getAnnotation(Audited.class);
            }
        }
        throw new NoSuchMethodException(nombreMetodo);
    }

    @Test
    @DisplayName("evalua el idSpel sobre el resultado y registra la auditoria con la descripcion generica")
    void evaluaIdSobreElResultado() throws Throwable {
        Lesion resultado = Lesion.builder().idLesion(45L).build();
        when(pjp.proceed()).thenReturn(resultado);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        Audited auditado = anotacionDe("conResultado");

        Object devuelto = aspecto.audit(pjp, auditado);

        assertSame(resultado, devuelto);
        verify(auditoriaService).recordEvent(eq("CREAR"), eq("Lesion"), eq(45L), eq("creó Lesion #45"));
    }

    @Test
    @DisplayName("evalua el idSpel sobre un argumento posicional (#p0)")
    void evaluaIdSobreArgumento() throws Throwable {
        when(pjp.proceed()).thenReturn(null);
        when(pjp.getArgs()).thenReturn(new Object[]{99L});
        Audited auditado = anotacionDe("conArgumentoPosicional");

        aspecto.audit(pjp, auditado);

        verify(auditoriaService).recordEvent(eq("ELIMINAR"), eq("Estudiante"), eq(99L), eq("eliminó Estudiante #99"));
    }

    @Test
    @DisplayName("usa descripcionSpel personalizada cuando esta definida, sin id")
    void usaDescripcionPersonalizada() throws Throwable {
        when(pjp.proceed()).thenReturn(List.of("a", "b"));
        when(pjp.getArgs()).thenReturn(new Object[0]);
        Audited auditado = anotacionDe("conDescripcionPersonalizada");

        aspecto.audit(pjp, auditado);

        verify(auditoriaService).recordEvent(eq("CREAR"), eq("Pago"), isNull(), eq("creó 2 pago(s)"));
    }

    @Test
    @DisplayName("un error al auditar no impide devolver el resultado del metodo envuelto")
    void erroresDeAuditoriaNoRompenElResultado() throws Throwable {
        Lesion resultado = Lesion.builder().idLesion(45L).build();
        when(pjp.proceed()).thenReturn(resultado);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(pjp.getSignature()).thenReturn(mock(Signature.class));
        doThrow(new RuntimeException("fallo")).when(auditoriaService).recordEvent(any(), any(), any(), any());
        Audited auditado = anotacionDe("conResultado");

        Object devuelto = aspecto.audit(pjp, auditado);

        assertSame(resultado, devuelto);
    }
}
