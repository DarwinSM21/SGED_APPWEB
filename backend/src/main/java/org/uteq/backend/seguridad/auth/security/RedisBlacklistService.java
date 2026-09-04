package org.uteq.backend.seguridad.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Lista negra de tokens JWT revocados usando Redis, indexada por el
 * {@code jti} de cada token. Permite invalidar un token antes de que expire
 * (p. ej. al cerrar sesión o rotar la contraseña).
 */
@Service
@RequiredArgsConstructor
public class RedisBlacklistService {
    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;

    /**
     * Añade el {@code jti} de un token a la lista negra, con una vigencia
     * limitada al tiempo de vida restante del token.
     *
     * @param jti    identificador único del token a revocar
     * @param ttlMs  vigencia de la revocación, en milisegundos
     */
    public void agregar(String jti, long ttlMs) {
        redis.opsForValue().set(PREFIX + jti, "revoked", Duration.ofMillis(ttlMs));
    }

    /**
     * Comprueba si el {@code jti} de un token está revocado.
     *
     * @param jti identificador único del token a comprobar
     * @return {@code true} si el token está en la lista negra
     */
    public boolean estaRevocado(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }

    /**
     * Revoca un token si aún le queda tiempo de vida; si ya expiró, no hace
     * nada.
     *
     * @param jti                identificador único del token a revocar
     * @param tiempoRestanteMs   milisegundos de vida que le quedan al token
     */
    public void revocar(String jti, long tiempoRestanteMs) {
        if (tiempoRestanteMs > 0) {
            redis.opsForValue().set(PREFIX + jti, "revoked",
                    Duration.ofMillis(tiempoRestanteMs));
        }
    }
}
