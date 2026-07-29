package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.uteq.backend.seguridad.auth.security.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisBlacklistServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private RedisBlacklistService service;

    private final String JTI_TEST = "jti-123";
    private final String CLAVE_REDIS = "jwt:blacklist:jti-123";

    @BeforeEach
    void setUp() {
        service = new RedisBlacklistService(redis);
    }

    @Test
    @DisplayName("agregar - Guarda el JTI en Redis con la clave de blacklist y el TTL indicado")
    void agregar_guarda_el_jti_con_el_ttl() {
        when(redis.opsForValue()).thenReturn(valueOps);

        service.agregar(JTI_TEST, 60_000L);

        verify(valueOps).set(CLAVE_REDIS, "revoked", Duration.ofMillis(60_000L));
    }

    @Test
    @DisplayName("revocar - Guarda el JTI con el TTL restante cuando es mayor a cero (> 0)")
    void revocar_guarda_el_jti_con_el_ttl_restante() {
        when(redis.opsForValue()).thenReturn(valueOps);

        service.revocar(JTI_TEST, 60_000L);

        verify(valueOps).set(CLAVE_REDIS, "revoked", Duration.ofMillis(60_000L));
    }

    @Test
    @DisplayName("revocar - Ignora la revocación cuando el tiempo restante es cero o negativo")
    void revocar_ignora_ttl_no_positivo() {
        service.revocar(JTI_TEST, 0L);
        service.revocar(JTI_TEST, -100L);

        verify(redis, never()).opsForValue();
    }

    @Test
    @DisplayName("estaRevocado - Retorna true si la clave existe en la blacklist de Redis")
    void estaRevocado_true_si_existe_la_clave() {
        when(redis.hasKey(CLAVE_REDIS)).thenReturn(true);

        assertTrue(service.estaRevocado(JTI_TEST));
    }

    @Test
    @DisplayName("estaRevocado - Retorna false si la clave no existe en la blacklist")
    void estaRevocado_false_si_no_existe() {
        when(redis.hasKey("jwt:blacklist:jti-999")).thenReturn(false);

        assertFalse(service.estaRevocado("jti-999"));
    }

    @Test
    @DisplayName("estaRevocado - Retorna false si Redis responde null al verificar la clave")
    void estaRevocado_false_si_redis_responde_null() {
        when(redis.hasKey(CLAVE_REDIS)).thenReturn(null);

        assertFalse(service.estaRevocado(JTI_TEST));
    }
}