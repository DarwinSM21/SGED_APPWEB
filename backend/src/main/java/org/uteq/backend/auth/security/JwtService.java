package org.uteq.backend.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

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
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .claim("jti", UUID.randomUUID().toString())
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractClaims(token).get("jti", String.class);
    }

    public String extractRol(String token) {
        return extractClaims(token).get("rol", String.class);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public boolean isTokenValid(String token) {
        return extractClaims(token)
                .getExpiration()
                .after(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}