package org.uteq.backend.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "blacklist:";

    public void revocarToken(String jti, long ttlMillis) {
        redisTemplate.opsForValue().set(
                PREFIX + jti,
                "revocado",
                ttlMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean estaRevocado(String jti) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX + jti)
        );
    }
}