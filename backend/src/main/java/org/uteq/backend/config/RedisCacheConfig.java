package org.uteq.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Cache Redis del endpoint de listado (Bloque A.1).
 * El TTL se declara en configuración externa (application.yml / variable
 * de entorno CACHE_TTL_SECONDS), nunca en código, como exige la guía.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    public static final String CACHE_ESTUDIANTES = "estudiantes";

    @Value("${cache.estudiantes.ttl-seconds:60}")
    private long ttlSeconds;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
                .withCacheConfiguration(CACHE_ESTUDIANTES, config)
                .build();
    }
}
