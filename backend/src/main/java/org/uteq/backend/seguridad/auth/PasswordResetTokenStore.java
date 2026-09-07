package org.uteq.backend.seguridad.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Almacén en Redis de los tokens de restablecimiento de contraseña (RF-37).
 *
 * <p>El token que viaja en el enlace nunca se guarda tal cual: en Redis vive
 * su SHA-256, de modo que un volcado de la base no entrega tokens usables.
 * Se mantienen dos claves por token:
 * <ul>
 *   <li>{@code pwreset:{sha256}} → username, para resolver el enlace;</li>
 *   <li>{@code pwreset:user:{username}} → {sha256}, para invalidar el token
 *       anterior de ese usuario cuando pide uno nuevo.</li>
 * </ul>
 * Ambas comparten el mismo TTL. El token es de un solo uso: {@link #consumir}
 * borra las dos claves.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetTokenStore {

    private static final String TOKEN_PREFIX = "pwreset:";
    private static final String USER_PREFIX = "pwreset:user:";

    private final StringRedisTemplate redis;

    /**
     * Guarda un token para un usuario, invalidando el que tuviera antes.
     *
     * @param username   usuario dueño del token (se guarda tal cual y se
     *                   normaliza a minúsculas solo para la clave de índice)
     * @param tokenCrudo el token que viajará en el enlace
     * @param ttl        vigencia de ambas claves
     */
    public void guardar(String username, String tokenCrudo, Duration ttl) {
        String hash = sha256(tokenCrudo);
        String userKey = USER_PREFIX + normalizar(username);

        String hashAnterior = redis.opsForValue().get(userKey);
        if (hashAnterior != null) {
            redis.delete(TOKEN_PREFIX + hashAnterior);
        }

        redis.opsForValue().set(TOKEN_PREFIX + hash, username, ttl);
        redis.opsForValue().set(userKey, hash, ttl);
    }

    /**
     * Resuelve el usuario asociado a un token vigente.
     *
     * @param tokenCrudo token recibido en la petición de restablecimiento
     * @return el username, o {@link Optional#empty()} si el token no existe,
     *         expiró o ya se consumió
     */
    public Optional<String> resolver(String tokenCrudo) {
        if (tokenCrudo == null || tokenCrudo.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(redis.opsForValue().get(TOKEN_PREFIX + sha256(tokenCrudo)));
    }

    /**
     * Invalida un token y su índice de usuario. Idempotente.
     *
     * @param tokenCrudo token a consumir
     */
    public void consumir(String tokenCrudo) {
        String hash = sha256(tokenCrudo);
        String username = redis.opsForValue().get(TOKEN_PREFIX + hash);
        redis.delete(TOKEN_PREFIX + hash);
        if (username != null) {
            redis.delete(USER_PREFIX + normalizar(username));
        }
    }

    private static String normalizar(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
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
