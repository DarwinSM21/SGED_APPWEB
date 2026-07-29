package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.seguridad.auth.security.LoginAttemptService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(redis);
        ReflectionTestUtils.setField(service, "maxIntentos", 5);
        ReflectionTestUtils.setField(service, "ventanaMinutos", 15L);
    }

    @Test
    void no_bloqueada_por_debajo_del_limite() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("login_attempts:1.2.3.4")).thenReturn("4");

        assertFalse(service.estaBloqueada("1.2.3.4"));
    }

    @Test
    void bloqueada_al_alcanzar_el_limite() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("login_attempts:1.2.3.4")).thenReturn("5");

        assertTrue(service.estaBloqueada("1.2.3.4"));
    }

    @Test
    void sin_intentos_previos_no_bloqueada() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("login_attempts:1.2.3.4")).thenReturn(null);

        assertFalse(service.estaBloqueada("1.2.3.4"));
    }

    @Test
    void primer_fallo_pone_ttl_a_la_ventana() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("login_attempts:1.2.3.4")).thenReturn(1L);

        service.registrarFallo("1.2.3.4");

        verify(redis).expire("login_attempts:1.2.3.4", java.time.Duration.ofMinutes(15));
    }

    @Test
    void fallo_subsiguiente_no_reinicia_ttl() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("login_attempts:1.2.3.4")).thenReturn(2L);

        service.registrarFallo("1.2.3.4");

        verify(redis, org.mockito.Mockito.never()).expire(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void exito_borra_el_contador() {
        service.registrarExito("1.2.3.4");
        verify(redis).delete("login_attempts:1.2.3.4");
    }
}
