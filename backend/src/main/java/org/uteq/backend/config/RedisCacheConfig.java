package org.uteq.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.uteq.backend.estudiante.dto.PageResponse;

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
        // GenericJackson2JsonRedisSerializer con default typing NON_FINAL no
        // agrega @class al objeto raiz cuando este es un record final
        // (PageResponse lo es): al leer, Jackson no sabe que tipo reconstruir
        // y falla con "missing type id property '@class'" - error que solo
        // aparece en el segundo request en adelante (el primero escribe el
        // cache, nunca lo lee). Como este cache solo guarda un tipo conocido
        // (PageResponse<EstudianteResponse>), es mas simple y robusto atar el
        // serializador directamente a esa clase en vez de depender de tipado
        // polimorfico generico.
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<PageResponse> serializer =
                new Jackson2JsonRedisSerializer<>(PageResponse.class);
        serializer.setObjectMapper(mapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        return RedisCacheManager.builder(factory)
                .withCacheConfiguration(CACHE_ESTUDIANTES, config)
                .build();
    }
}
