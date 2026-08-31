package org.uteq.backend.deportivo.asistencia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QrAsistenciaService {
    private static final String PREFIJO = "qr:asistencia:";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final StringRedisTemplate redis;

    @Value("${asistencia.qr.ttl-segundos:60}")
    private int ttlSegundos;

    public TokenQr emitir(Long idSesion) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redis.opsForValue().set(PREFIJO + token,
                String.valueOf(idSesion),
                Duration.ofSeconds(ttlSegundos));

        return new TokenQr(token, ttlSegundos);
    }

    public Optional<Long> canjear(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String idSesion = redis.opsForValue().getAndDelete(PREFIJO + token);
        if (idSesion == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(idSesion));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public record TokenQr(String token, int expiraEnSegundos) {}
}
