package org.uteq.backend.seguridad.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.uteq.backend.common.exception.TooManyRequestsException;

import java.time.Duration;
import java.util.Locale;

/**
 * Límite de solicitudes de restablecimiento de contraseña (RF-37), sobre
 * Redis y con el mismo patrón que {@link LoginAttemptService}: un count
 * por clave con expiración fijada en el primer incremento.
 *
 * <p>Dos cuentas independientes:
 * <ul>
 *   <li>por identificador solicitado — frena el acoso a una cuenta concreta;</li>
 *   <li>por IP de origen — frena el uso del endpoint como emisor masivo de
 *       correo.</li>
 * </ul>
 * Superar cualquiera de las dos corta la solicitud con {@code 429}, sin que
 * la respuesta dependa de si la cuenta existe.
 */
@Service
@RequiredArgsConstructor
public class ResetRequestLimitService {

    private static final String ID_PREFIX = "pwreset_lim:id:";
    private static final String IP_PREFIX = "pwreset_lim:ip:";

    private final StringRedisTemplate redis;

    @Value("${security.reset.max-por-identificador:3}")
    private int maxPorIdentificador;

    @Value("${security.reset.ventana-identificador-minutos:15}")
    private long ventanaIdentificadorMinutos;

    @Value("${security.reset.max-por-ip:10}")
    private int maxPorIp;

    @Value("${security.reset.ventana-ip-minutos:60}")
    private long ventanaIpMinutos;

    /**
     * Comprueba los dos límites antes de atender una solicitud.
     *
     * @param identificador username o correo tal como llegó en la petición
     * @param ip            dirección remota del cliente
     * @throws TooManyRequestsException si alguna de las dos cuentas ya alcanzó
     *                                  su máximo dentro de la ventana
     */
    public void check(String identificador, String ip) {
        if (count(ID_PREFIX + normalize(identificador)) >= maxPorIdentificador
                || count(IP_PREFIX + ip) >= maxPorIp) {
            throw new TooManyRequestsException(
                    "Demasiadas solicitudes de restablecimiento. Intenta de nuevo más tarde.");
        }
    }

    /**
     * Cuenta una solicitud atendida contra los dos contadores.
     *
     * @param identificador username o correo solicitado
     * @param ip            dirección remota del cliente
     */
    public void record(String identificador, String ip) {
        increment(ID_PREFIX + normalize(identificador), ventanaIdentificadorMinutos);
        increment(IP_PREFIX + ip, ventanaIpMinutos);
    }

    private long count(String clave) {
        String valor = redis.opsForValue().get(clave);
        return valor == null ? 0L : Long.parseLong(valor);
    }

    private void increment(String clave, long ventanaMinutos) {
        Long total = redis.opsForValue().increment(clave);
        if (total != null && total == 1L) {
            redis.expire(clave, Duration.ofMinutes(ventanaMinutos));
        }
    }

    private static String normalize(String identificador) {
        return identificador == null ? "" : identificador.trim().toLowerCase(Locale.ROOT);
    }
}
