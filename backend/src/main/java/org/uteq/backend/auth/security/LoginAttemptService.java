package org.uteq.backend.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Control de intentos fallidos de login por IP (OWASP A07, Bloque C.2).
 * Tras security.login.max-intentos fallidos consecutivos, el siguiente
 * intento responde 429 Too Many Requests. El contador expira solo
 * (ventana deslizante) y se reinicia con un login exitoso.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String PREFIX = "login_attempts:";

    private final StringRedisTemplate redis;

    @Value("${security.login.max-intentos:5}")
    private int maxIntentos;

    @Value("${security.login.ventana-minutos:15}")
    private long ventanaMinutos;

    public boolean estaBloqueada(String ip) {
        String valor = redis.opsForValue().get(PREFIX + ip);
        return valor != null && Integer.parseInt(valor) >= maxIntentos;
    }

    public void registrarFallo(String ip) {
        String clave = PREFIX + ip;
        Long intentos = redis.opsForValue().increment(clave);
        if (intentos != null && intentos == 1L) {
            redis.expire(clave, Duration.ofMinutes(ventanaMinutos));
        }
    }

    public void registrarExito(String ip) {
        redis.delete(PREFIX + ip);
    }
}
