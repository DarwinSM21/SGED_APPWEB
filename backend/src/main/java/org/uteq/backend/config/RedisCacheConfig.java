package org.uteq.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        // GenericJackson2JsonRedisSerializer sin configurar no soporta
        // java.time.Instant (falla al serializar EstudianteResponse.creadoEn).
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)));

        return RedisCacheManager.builder(factory)
                .withCacheConfiguration(CACHE_ESTUDIANTES, config)
                .build();
    }
}
