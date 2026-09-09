package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.uteq.backend.config.RedisCacheConfig;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RNF-23b: ante un fallo de Redis, el {@link CacheErrorHandler} de
 * {@link RedisCacheConfig} degrada a consulta directa —traga la excepción y
 * la registra— en vez de propagarla. Si relanzara, un {@code GET} cacheado
 * devolvería {@code 5xx} con Redis caído.
 */
class RedisCacheErrorHandlerTest {

    private final CacheErrorHandler handler = new RedisCacheConfig().errorHandler();
    private final Cache cache = mockCache();

    private static Cache mockCache() {
        Cache c = mock(Cache.class);
        when(c.getName()).thenReturn("estudiantes");
        return c;
    }

    @Test
    @DisplayName("un fallo de lectura de caché no se propaga")
    void getError_no_se_propaga() {
        assertThatCode(() -> handler.handleCacheGetError(
                new RuntimeException("Redis caído"), cache, "0-10"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("un fallo de escritura, invalidación o limpieza de caché no se propaga")
    void put_evict_clear_no_se_propagan() {
        RuntimeException boom = new RuntimeException("Redis caído");
        assertThatCode(() -> {
            handler.handleCachePutError(boom, cache, "0-10", new Object());
            handler.handleCacheEvictError(boom, cache, "0-10");
            handler.handleCacheClearError(boom, cache);
        }).doesNotThrowAnyException();
    }
}
