package org.uteq.backend.seguridad.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Almacén en Redis de los tokens de confirmación de correo (RNF-26 / H-09).
 *
 * <p>Mismo patrón que {@link PasswordResetTokenStore}: el token que viaja en el
 * enlace nunca se guarda tal cual, en Redis vive su SHA-256. Se mantienen dos
 * claves por token:
 * <ul>
 *   <li>{@code emailverify:{sha256}} → id de la persona, para resolve el enlace;</li>
 *   <li>{@code emailverify:persona:{id}} → {sha256}, para invalidar el token
 *       anterior de esa persona cuando se emite uno nuevo (p. ej. al volver a
 *       cambiar el correo).</li>
 * </ul>
 * El token es de un solo uso: {@link #consume} borra ambas claves.
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationTokenStore {

    private static final String TOKEN_PREFIX = "emailverify:";
    private static final String PERSONA_PREFIX = "emailverify:persona:";

    private final StringRedisTemplate redis;

    /**
     * Guarda un token para una persona, invalidando el que tuviera antes.
     *
     * @param idPersona  identificador de la persona dueña del token
     * @param tokenCrudo el token que viajará en el enlace de confirmación
     * @param ttl        vigencia de ambas claves
     */
    public void save(Long idPersona, String tokenCrudo, Duration ttl) {
        String hash = sha256(tokenCrudo);
        String personaKey = PERSONA_PREFIX + idPersona;

        String hashAnterior = redis.opsForValue().get(personaKey);
        if (hashAnterior != null) {
            redis.delete(TOKEN_PREFIX + hashAnterior);
        }

        redis.opsForValue().set(TOKEN_PREFIX + hash, String.valueOf(idPersona), ttl);
        redis.opsForValue().set(personaKey, hash, ttl);
    }

    /**
     * Resuelve la persona asociada a un token vigente.
     *
     * @param tokenCrudo token recibido en la petición de confirmación
     * @return el id de la persona, o {@link Optional#empty()} si el token no
     *         existe, expiró o ya se consumió
     */
    public Optional<Long> resolve(String tokenCrudo) {
        if (tokenCrudo == null || tokenCrudo.isBlank()) {
            return Optional.empty();
        }
        String valor = redis.opsForValue().get(TOKEN_PREFIX + sha256(tokenCrudo));
        return valor == null ? Optional.empty() : Optional.of(Long.valueOf(valor));
    }

    /**
     * Invalida un token y su índice de persona. Idempotente.
     *
     * @param tokenCrudo token a consume
     */
    public void consume(String tokenCrudo) {
        String hash = sha256(tokenCrudo);
        String idPersona = redis.opsForValue().get(TOKEN_PREFIX + hash);
        redis.delete(TOKEN_PREFIX + hash);
        if (idPersona != null) {
            redis.delete(PERSONA_PREFIX + idPersona);
        }
    }

    private static String sha256(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
