package org.uteq.backend.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio de emisión y validación de JWT.
 * Cumple RFC 7519 con los siete claims estándar:
 * iss, sub, aud, exp, nbf, iat, jti. (OBS: claim aud / Entrega 3 Bloque A.1)
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

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, String rol) {
        return buildToken(username, rol, expirationMs, "access");
    }

    public String generateRefreshToken(String username, String rol) {
        return buildToken(username, rol, refreshExpirationMs, "refresh");
    }

    private String buildToken(String username, String rol, long expMs, String type) {
        Date ahora = new Date();
        return Jwts.builder()
                .issuer(issuer)                                    // iss
                .subject(username)                                 // sub
                .audience().add(audience).and()                    // aud
                .expiration(new Date(ahora.getTime() + expMs))     // exp
                .notBefore(ahora)                                  // nbf
                .issuedAt(ahora)                                   // iat
                .id(UUID.randomUUID().toString())                  // jti
                .claim("rol", rol)
                .claim("type", type)
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    public String extractRol(String token) {
        return extractClaims(token).get("rol", String.class);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Un token es válido si: firma correcta, no expirado (exp),
     * ya vigente (nbf), emisor y audiencia esperados.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims c = extractClaims(token);
            Date ahora = new Date();
            boolean vigente = c.getExpiration().after(ahora)
                    && (c.getNotBefore() == null || !c.getNotBefore().after(ahora));
            boolean emisorOk = issuer.equals(c.getIssuer());
            boolean audOk = c.getAudience() != null && c.getAudience().contains(audience);
            return vigente && emisorOk && audOk;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
