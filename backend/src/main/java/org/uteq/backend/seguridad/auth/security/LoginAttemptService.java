package org.uteq.backend.seguridad.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Control de intentos de inicio de sesión por IP usando Redis: cuenta los
 * fallos dentro de una ventana de tiempo y bloquea la IP cuando se supera
 * {@code security.login.max-intentos} (por defecto 5). Un login exitoso
 * reinicia el contador.
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

    /**
     * Indica si una IP está bloqueada por haber superado el máximo de intentos
     * fallidos dentro de la ventana.
     *
     * @param ip dirección IP a comprobar
     * @return {@code true} si el contador alcanza o supera el máximo
     */
    public boolean estaBloqueada(String ip) {
        String valor = redis.opsForValue().get(PREFIX + ip);
        return valor != null && Integer.parseInt(valor) >= maxIntentos;
    }

    /**
     * Registra un intento de login fallido para una IP, estableciendo la
     * expiración de la ventana en el primer fallo.
     *
     * @param ip dirección IP que falló el inicio de sesión
     */
    public void registrarFallo(String ip) {
        String clave = PREFIX + ip;
        Long intentos = redis.opsForValue().increment(clave);
        if (intentos != null && intentos == 1L) {
            redis.expire(clave, Duration.ofMinutes(ventanaMinutos));
        }
    }

    /**
     * Registra un login exitoso, eliminando el contador de intentos de la IP.
     *
     * @param ip dirección IP que inició sesión correctamente
     */
    public void registrarExito(String ip) {
        redis.delete(PREFIX + ip);
    }
}
