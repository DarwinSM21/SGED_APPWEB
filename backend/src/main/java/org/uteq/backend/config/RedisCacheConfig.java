package org.uteq.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.Duration;

/**
 * Configuración de la caché Redis (RNF-02) y su comportamiento ante fallos
 * (RNF-23b).
 *
 * <p>Implementa {@link CachingConfigurer} para registrar un
 * {@link CacheErrorHandler} que <b>degrada, no rompe</b>: si Redis no está
 * disponible, un {@code GET} cacheado (por ejemplo {@code GET /api/estudiantes})
 * no debe devolver {@code 5xx} —debe caer a la consulta directa a la base—.
 * El handler traga la excepción de lectura/escritura de caché y la registra;
 * Spring entonces ejecuta el método anotado como si fuera un fallo de caché,
 * que es exactamente la consulta a la base. Las caídas de Redis para la
 * autenticación siguen fallando cerradas (RNF-23a), eso vive en
 * {@code JwtAuthenticationFilter} y no lo toca este handler.
 */
@Configuration
public class RedisCacheConfig implements CachingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);
    public static final String CACHE_STUDENTS = "estudiantes";
    public static final String CACHE_COACHES = "entrenadores";
    public static final String CACHE_USERS = "usuarios";

    @Value("${cache.estudiantes.ttl-seconds:60}")
    private long ttlEstudiantesSeconds;

    @Value("${cache.entrenadores.ttl-seconds:60}")
    private long ttlEntrenadoresSeconds;

    @Value("${cache.usuarios.ttl-seconds:60}")
    private long ttlUsuariosSeconds;

    /**
     * @param factory conexión a Redis inyectada por Spring
     * @return el gestor de caché con una configuración de TTL y serialización
     *         propia para cada caché ({@code estudiantes}, {@code entrenadores}, {@code usuarios})
     */
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

    /**
     * RNF-23b: ante un fallo de Redis, la caché se degrada a consulta directa
     * a la base en vez de propagar el error. Cada método registra el fallo una
     * vez (nivel WARN) y no relanza; Spring continúa como si fuera un fallo de
     * caché normal —invoca el método y consulta la base—.
     *
     * @return el manejador que degrada lectura, escritura, evicción y
     *         limpieza de caché a advertencia registrada, sin relanzar
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            /**
             * @param ex excepción lanzada por Redis al leer
             * @param cache caché afectada
             * @param key clave que se intentaba leer
             */
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Caché no disponible al leer '{}' (clave {}): se consulta la base. Causa: {}",
                        cache.getName(), key, ex.getClass().getSimpleName());
            }

            /**
             * @param ex excepción lanzada por Redis al escribir
             * @param cache caché afectada
             * @param key clave que se intentaba escribir
             * @param value valor que se intentaba cachear
             */
            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Caché no disponible al escribir '{}' (clave {}): el resultado no se cachea. Causa: {}",
                        cache.getName(), key, ex.getClass().getSimpleName());
            }

            /**
             * @param ex excepción lanzada por Redis al invalidar
             * @param cache caché afectada
             * @param key clave que se intentaba invalidar
             */
            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Caché no disponible al invalidar '{}' (clave {}). Causa: {}",
                        cache.getName(), key, ex.getClass().getSimpleName());
            }

            /**
             * @param ex excepción lanzada por Redis al limpiar
             * @param cache caché afectada
             */
            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Caché no disponible al limpiar '{}'. Causa: {}",
                        cache.getName(), ex.getClass().getSimpleName());
            }
        };
    }
}
