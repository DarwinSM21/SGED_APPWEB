package org.uteq.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {
    public static final String CACHE_ESTUDIANTES = "estudiantes";
    public static final String CACHE_ENTRENADORES = "entrenadores";
    public static final String CACHE_USUARIOS = "usuarios";

    @Value("${cache.estudiantes.ttl-seconds:60}")
    private long ttlEstudiantesSeconds;

    @Value("${cache.entrenadores.ttl-seconds:60}")
    private long ttlEntrenadoresSeconds;

    @Value("${cache.usuarios.ttl-seconds:60}")
    private long ttlUsuariosSeconds;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration configEstudiantes = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlEstudiantesSeconds))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        RedisCacheConfiguration configEntrenadores = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlEntrenadoresSeconds))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        RedisCacheConfiguration configUsuarios = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlUsuariosSeconds))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(factory)
                .withCacheConfiguration(CACHE_ESTUDIANTES, configEstudiantes)
                .withCacheConfiguration(CACHE_ENTRENADORES, configEntrenadores)
                .withCacheConfiguration(CACHE_USUARIOS, configUsuarios)
                .build();
    }
}
