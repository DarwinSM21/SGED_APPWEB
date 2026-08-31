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
import org.uteq.backend.seguridad.auditoria.aop.Auditado;
import org.uteq.backend.seguridad.auditoria.aop.AuditoriaAspect;
import org.uteq.backend.seguridad.auditoria.service.AuditoriaService;

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
class AuditoriaAspectTest {
    @Mock private AuditoriaService auditoriaService;
    @Mock private ProceedingJoinPoint pjp;

    private AuditoriaAspect aspecto;

    @BeforeEach
    void setUp() {
        aspecto = new AuditoriaAspect(auditoriaService);
    }

    static class MetodosDeEjemplo {
        @Auditado(accion = "CREAR", entidad = "Lesion", idSpel = "#result.idLesion")
        void conResultado() {
        }

        @Auditado(accion = "ELIMINAR", entidad = "Estudiante", idSpel = "#p0")
        void conArgumentoPosicional(Long id) {
        }

        @Auditado(accion = "CREAR", entidad = "Pago",
                descripcionSpel = "'creó ' + #result.size() + ' pago(s)'")
        void conDescripcionPersonalizada() {
        }
    }

    private Auditado anotacionDe(String nombreMetodo) throws NoSuchMethodException {
        for (Method m : MetodosDeEjemplo.class.getDeclaredMethods()) {
            if (m.getName().equals(nombreMetodo)) {
                return m.getAnnotation(Auditado.class);
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
        Auditado auditado = anotacionDe("conResultado");

        Object devuelto = aspecto.auditar(pjp, auditado);

        assertSame(resultado, devuelto);
        verify(auditoriaService).registrar(eq("CREAR"), eq("Lesion"), eq(45L), eq("creó Lesion #45"));
    }

    @Test
    @DisplayName("evalua el idSpel sobre un argumento posicional (#p0)")
    void evaluaIdSobreArgumento() throws Throwable {
        when(pjp.proceed()).thenReturn(null);
        when(pjp.getArgs()).thenReturn(new Object[]{99L});
        Auditado auditado = anotacionDe("conArgumentoPosicional");

        aspecto.auditar(pjp, auditado);

        verify(auditoriaService).registrar(eq("ELIMINAR"), eq("Estudiante"), eq(99L), eq("eliminó Estudiante #99"));
    }

    @Test
    @DisplayName("usa descripcionSpel personalizada cuando esta definida, sin id")
    void usaDescripcionPersonalizada() throws Throwable {
        when(pjp.proceed()).thenReturn(List.of("a", "b"));
        when(pjp.getArgs()).thenReturn(new Object[0]);
        Auditado auditado = anotacionDe("conDescripcionPersonalizada");

        aspecto.auditar(pjp, auditado);

        verify(auditoriaService).registrar(eq("CREAR"), eq("Pago"), isNull(), eq("creó 2 pago(s)"));
    }

    @Test
    @DisplayName("un error al auditar no impide devolver el resultado del metodo envuelto")
    void erroresDeAuditoriaNoRompenElResultado() throws Throwable {
        Lesion resultado = Lesion.builder().idLesion(45L).build();
        when(pjp.proceed()).thenReturn(resultado);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(pjp.getSignature()).thenReturn(mock(Signature.class));
        doThrow(new RuntimeException("fallo")).when(auditoriaService).registrar(any(), any(), any(), any());
        Auditado auditado = anotacionDe("conResultado");

        Object devuelto = aspecto.auditar(pjp, auditado);

        assertSame(resultado, devuelto);
    }
}
