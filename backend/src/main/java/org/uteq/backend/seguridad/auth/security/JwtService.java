package org.uteq.backend.seguridad.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Emisión y validación de tokens JWT (acceso y refresco). Cada token lleva un
 * {@code jti} único, firma HMAC derivada de {@code security.jwt.secret}, y
 * reclamos de rol, tipo, emisor y audiencia configurados por propiedades.
 */
@Service
public class JwtService {
    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${security.jwt.issuer:sged-backend}")
    private String issuer;

    @Value("${security.jwt.audience:sged-frontend}")
    private String audience;

    /**
     * Genera un token de acceso (tipo {@code access}) con la vigencia de
     * {@code security.jwt.expiration-ms}.
     *
     * @param username nombre de usuario (sujeto del token)
     * @param rol      rol del usuario, guardado como reclamo
     * @return token JWT firmado
     */
    public String generateToken(String username, String rol) {
        return buildToken(username, rol, expirationMs, "access");
    }

    /**
     * Genera un token de refresco (tipo {@code refresh}) con la vigencia de
     * {@code security.jwt.refresh-expiration-ms}.
     *
     * @param username nombre de usuario (sujeto del token)
     * @param rol      rol del usuario, guardado como reclamo
     * @return token JWT firmado
     */
    public String generateRefreshToken(String username, String rol) {
        return buildToken(username, rol, refreshExpirationMs, "refresh");
    }

    /**
     * Comprueba si un token es válido: firma y emisor correctos, audiencia
     * esperada y expiración no vencida.
     *
     * @param token token JWT a examinar
     * @return {@code true} si es válido; {@code false} si es inválido, mal
     *         formado o está expirado
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token).getPayload();
            java.util.Set<String> aud = claims.getAudience();
            return aud != null && aud.contains(audience) && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extrae el nombre de usuario (sujeto) de un token.
     *
     * @param token token JWT ya firmado
     * @return el sujeto del token
     */
    public String extractUsername(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    /**
     * Extrae el rol del usuario guardado como reclamo del token.
     *
     * @param token token JWT ya firmado
     * @return el valor del reclamo {@code rol}
     */
    public String extractRol(String token) {
        return parseToken(token).getPayload().get("rol", String.class);
    }

    /**
     * Extrae el identificador único del token.
     *
     * @param token token JWT ya firmado
     * @return el valor del reclamo {@code jti}
     */
    public String extractJti(String token) {
        return parseToken(token).getPayload().getId();
    }

    /**
     * Vigencia de los tokens de acceso, en milisegundos.
     *
     * @return {@code security.jwt.expiration-ms}
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Vigencia de los tokens de refresco, en milisegundos.
     *
     * @return {@code security.jwt.refresh-expiration-ms}
     */
    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private String buildToken(String username, String rol, long ttl, String tokenType) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("rol", rol)
                .claim("type", tokenType)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(now)
                .notBefore(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(getSigningKey())
                .compact();
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(secret.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
