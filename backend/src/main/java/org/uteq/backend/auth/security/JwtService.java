package org.uteq.backend.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio JWT: genera, valida y extrae claims de tokens JWT (RFC 7519).
 * Usa jjwt 0.12 con HMAC-SHA256 (HS256).
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

    public String generateToken(String username, String rol) {
        return buildToken(username, rol, expirationMs, "access");
    }

    public String generateRefreshToken(String username, String rol) {
        return buildToken(username, rol, refreshExpirationMs, "refresh");
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token).getPayload();
            java.util.Set<String> aud = claims.getAudience();
            return aud != null && aud.contains(audience) && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    public String extractRol(String token) {
        return parseToken(token).getPayload().get("rol", String.class);
    }

    public String extractJti(String token) {
        return parseToken(token).getPayload().getId();
    }

    public long getExpirationMs() {
        return expirationMs;
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
