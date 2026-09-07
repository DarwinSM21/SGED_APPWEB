package org.uteq.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
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
    public static final String CACHE_STUDENTS = "estudiantes";
    public static final String CACHE_COACHES = "entrenadores";
    public static final String CACHE_USERS = "usuarios";

    @Value("${cache.estudiantes.ttl-seconds:60}")
    private long ttlEstudiantesSeconds;

    @Value("${cache.entrenadores.ttl-seconds:60}")
    private long ttlEntrenadoresSeconds;

    @Value("${cache.usuarios.ttl-seconds:60}")
    private long ttlUsuariosSeconds;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        /*
         * GenericJackson2JsonRedisSerializer requiere polymorphic typing para
         * reconstruir el tipo concreto al leer. El constructor que recibe un
         * ObjectMapper NO lo activa por defecto: al deserializar devolvió
         * LinkedHashMap y el cache hit reventaba con ClassCastException en
         * StudentService.listar(). Se habilita explicitamente con
         * activateDefaultTyping(EVERYTHING): el tipo raiz del record de
         * respuesta no esta anotado con @JsonTypeInfo, y con NON_FINAL la
         * serializacion deja de registrar la clase concreta del record
         * generico, por lo que el desempate al leer otra vez falla igual.
         */
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.EVERYTHING,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);

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
                .withCacheConfiguration(CACHE_STUDENTS, configEstudiantes)
                .withCacheConfiguration(CACHE_COACHES, configEntrenadores)
                .withCacheConfiguration(CACHE_USERS, configUsuarios)
                .build();
    }
}
