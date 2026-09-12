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
import org.uteq.backend.deportivo.attendance.service.QrAttendanceService;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrAttendanceServiceTest {
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> ops;

    private QrAttendanceService servicio;

    @BeforeEach
    void setUp() {
        servicio = new QrAttendanceService(redis);
        ReflectionTestUtils.setField(servicio, "ttlSegundos", 60);
    }

    @Test
    @DisplayName("El token se guarda con expiracion: un QR fotografiado deja de servir")
    void tokenSeGuardaConTtl() {
        when(redis.opsForValue()).thenReturn(ops);

        var resultado = servicio.issue(42L);

        assertNotNull(resultado.token());
        assertEquals(60, resultado.expiraEnSegundos());
        verify(ops).set(startsWith("qr:asistencia:"), eq("42"), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("Dos emisiones nunca devuelven el mismo token")
    void tokensSonUnicos() {
        when(redis.opsForValue()).thenReturn(ops);

        var a = servicio.issue(1L);
        var b = servicio.issue(1L);

        assertNotEquals(a.token(), b.token());
    }

    @Test
    @DisplayName("El token no contiene el id de sesion ni ningun dato del estudiante")
    void tokenNoFiltraInformacion() {
        when(redis.opsForValue()).thenReturn(ops);

        var resultado = servicio.issue(12345L);

        assertFalse(resultado.token().contains("12345"));
    }

    @Test
    @DisplayName("Canjear consume el token de forma atomica, para que no sirva dos veces")
    void canjearConsumeElToken() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.getAndDelete("qr:asistencia:abc")).thenReturn("7");

        assertEquals(Optional.of(7L), servicio.redeem("abc"));

        verify(ops).getAndDelete("qr:asistencia:abc");
        verify(ops, never()).get(anyString());
    }

    @Test
    @DisplayName("Un token expirado o ya usado no se canjea")
    void tokenExpiradoNoSeCanjea() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.getAndDelete(anyString())).thenReturn(null);

        assertTrue(servicio.redeem("token-viejo").isEmpty());
    }

    @Test
    @DisplayName("Un token vacio o nulo se rechaza sin tocar Redis")
    void tokenVacioNoConsultaRedis() {
        assertTrue(servicio.redeem(null).isEmpty());
        assertTrue(servicio.redeem("   ").isEmpty());

        verifyNoInteractions(redis);
    }
}
