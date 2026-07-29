package org.uteq.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.uteq.backend.auth.security.RedisBlacklistService;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisBlacklistServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private RedisBlacklistService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new RedisBlacklistService(redis);
    }

    @Test
    void revocar_guarda_el_jti_con_el_ttl_restante() {
        when(redis.opsForValue()).thenReturn(valueOps);

        service.revocar("jti-123", 60_000L);

        verify(valueOps).set("jwt:blacklist:jti-123", "revoked", Duration.ofMillis(60_000L));
    }

    @Test
    void revocar_ignora_ttl_no_positivo() {
        service.revocar("jti-123", 0L);
        verify(redis, never()).opsForValue();
    }

    @Test
    void estaRevocado_true_si_existe_la_clave() {
        when(redis.hasKey("jwt:blacklist:jti-123")).thenReturn(true);
        assertTrue(service.estaRevocado("jti-123"));
    }

    @Test
    void estaRevocado_false_si_no_existe() {
        when(redis.hasKey("jwt:blacklist:jti-999")).thenReturn(false);
        assertFalse(service.estaRevocado("jti-999"));
    }
}
