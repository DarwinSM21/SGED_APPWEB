package org.uteq.backend.seguridad.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Época de invalidación de sesiones por usuario (RF-37). Al restablecer su
 * contraseña se marca el instante actual; a partir de ahí, cualquier token
 * —de acceso o de refresco— emitido antes de esa marca deja de ser aceptado,
 * lo que mata todas las sesiones abiertas del usuario, incluidas las de
 * refresh token, que de otro modo no se pueden revocar.
 *
 * <p>La marca vive en Redis con un TTL igual a la vida del refresh token: más
 * allá de eso ya no queda ningún token que pudiera ser anterior a ella.
 */
@Service
@RequiredArgsConstructor
public class SessionEpochService {

    private static final String PREFIX = "sec:pwepoch:";

    private final StringRedisTemplate redis;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Marca "ahora" como época de invalidación del usuario.
     *
     * @param username usuario cuyas sesiones previas quedan invalidadas
     */
    public void mark(String username) {
        redis.opsForValue().set(
                PREFIX + normalize(username),
                Long.toString(Instant.now().getEpochSecond()),
                Duration.ofMillis(refreshExpirationMs));
    }

    /**
     * Época de invalidación del usuario, si tiene una vigente.
     *
     * @param username usuario a consultar
     * @return el instante (epoch en segundos) del último restablecimiento
     */
    public Optional<Long> epochOf(String username) {
        String valor = redis.opsForValue().get(PREFIX + normalize(username));
        return valor == null ? Optional.empty() : Optional.of(Long.parseLong(valor));
    }

    /**
     * Indica si un token quedó invalidado por un restablecimiento posterior
     * a su emisión.
     *
     * @param username    sujeto del token
     * @param emitidoEn   instante de emisión ({@code iat}) del token
     * @return {@code true} si hubo un restablecimiento después de esa emisión
     */
    public boolean invalidatedByReset(String username, Instant emitidoEn) {
        if (emitidoEn == null) {
            return false;
        }
        return epochOf(username)
                .map(epoca -> emitidoEn.getEpochSecond() < epoca)
                .orElse(false);
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
