package org.uteq.backend.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Blacklist de tokens JWT en Redis.
 * Almacena el JTI de tokens revocados con TTL igual a la expiracion del token.
 * En cada solicitud, el filtro JWT consulta esta blacklist antes de autorizar.
 */
@Service
@RequiredArgsConstructor
public class RedisBlacklistService {

    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;

    /**
     * Agrega el JTI a la blacklist con el TTL indicado.
     */
    public void agregar(String jti, long ttlMs) {
        redis.opsForValue().set(PREFIX + jti, "revoked", Duration.ofMillis(ttlMs));
    }

    /**
     * Verifica si un JTI esta en la blacklist (token revocado).
     */
    public boolean estaRevocado(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }

    /**
     * Revoca un token usando su JTI y tiempo restante de expiracion.
     */
    public void revocar(String jti, long tiempoRestanteMs) {
        if (tiempoRestanteMs > 0) {
            redis.opsForValue().set(PREFIX + jti, "revoked",
                    Duration.ofMillis(tiempoRestanteMs));
        }
    }
}
