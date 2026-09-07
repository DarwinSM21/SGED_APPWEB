package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.common.exception.TooManyRequestsException;
import org.uteq.backend.seguridad.auth.security.ResetRequestLimitService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetRequestLimitServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private ResetRequestLimitService service;

    @BeforeEach
    void setUp() {
        service = new ResetRequestLimitService(redis);
        ReflectionTestUtils.setField(service, "maxPorIdentificador", 3);
        ReflectionTestUtils.setField(service, "ventanaIdentificadorMinutos", 15L);
        ReflectionTestUtils.setField(service, "maxPorIp", 10);
        ReflectionTestUtils.setField(service, "ventanaIpMinutos", 60L);
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("por debajo de los dos limites no lanza")
    void por_debajo_del_limite() {
        when(valueOps.get("pwreset_lim:id:ana@x.com")).thenReturn("2");
        when(valueOps.get("pwreset_lim:ip:1.2.3.4")).thenReturn("5");

        assertThatCode(() -> service.check("Ana@x.com", "1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("alcanzado el limite por identificador: 429")
    void limite_por_identificador() {
        when(valueOps.get("pwreset_lim:id:ana@x.com")).thenReturn("3");

        assertThatThrownBy(() -> service.check("ana@x.com", "1.2.3.4"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("alcanzado el limite por IP: 429")
    void limite_por_ip() {
        when(valueOps.get("pwreset_lim:id:ana@x.com")).thenReturn(null);
        when(valueOps.get("pwreset_lim:ip:1.2.3.4")).thenReturn("10");

        assertThatThrownBy(() -> service.check("ana@x.com", "1.2.3.4"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("registrar incrementa ambos contadores y fija la ventana en el primer incremento")
    void registrar_incrementa_y_expira() {
        when(valueOps.increment("pwreset_lim:id:ana@x.com")).thenReturn(1L);
        when(valueOps.increment("pwreset_lim:ip:1.2.3.4")).thenReturn(4L);

        service.registrar("Ana@x.com", "1.2.3.4");

        verify(redis).expire(eq("pwreset_lim:id:ana@x.com"), eq(Duration.ofMinutes(15)));
        verify(redis, org.mockito.Mockito.never())
                .expire(eq("pwreset_lim:ip:1.2.3.4"), org.mockito.ArgumentMatchers.any());
    }
}
