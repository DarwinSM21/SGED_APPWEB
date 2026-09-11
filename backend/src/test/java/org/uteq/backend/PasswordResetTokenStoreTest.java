package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.uteq.backend.seguridad.auth.PasswordResetTokenStore;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenStoreTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private PasswordResetTokenStore store;

    @BeforeEach
    void setUp() {
        store = new PasswordResetTokenStore(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("guardar escribe la clave del token y el indice de usuario con el TTL")
    void guardar_escribe_ambas_claves() {
        when(valueOps.get("pwreset:user:ana.torres")).thenReturn(null);

        store.save("ana.torres", "token-crudo", Duration.ofMinutes(30));

        verify(valueOps).set(startsWith("pwreset:"), eq("ana.torres"), eq(Duration.ofMinutes(30)));
        verify(valueOps).set(eq("pwreset:user:ana.torres"), anyString(), eq(Duration.ofMinutes(30)));
        verify(redis, never()).delete(anyString());
    }

    @Test
    @DisplayName("guardar invalida el token anterior del mismo usuario")
    void guardar_invalida_el_anterior() {
        when(valueOps.get("pwreset:user:ana.torres")).thenReturn("hash-viejo");

        store.save("ana.torres", "token-nuevo", Duration.ofMinutes(30));

        verify(redis).delete("pwreset:hash-viejo");
    }

    @Test
    @DisplayName("resolver devuelve el usuario de un token vigente")
    void resolver_token_vigente() {
        when(valueOps.get(startsWith("pwreset:"))).thenReturn("ana.torres");

        assertThat(store.resolve("token-crudo")).contains("ana.torres");
    }

    @Test
    @DisplayName("resolver devuelve vacio para token nulo, en blanco o inexistente")
    void resolver_token_invalido() {
        assertThat(store.resolve(null)).isEmpty();
        assertThat(store.resolve("  ")).isEmpty();

        when(valueOps.get(startsWith("pwreset:"))).thenReturn(null);
        assertThat(store.resolve("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("consumir borra la clave del token y el indice de usuario")
    void consumir_borra_ambas() {
        when(valueOps.get(startsWith("pwreset:"))).thenReturn("ana.torres");

        store.consume("token-crudo");

        verify(redis, times(2)).delete(startsWith("pwreset:"));
        verify(redis).delete("pwreset:user:ana.torres");
    }

    @Test
    @DisplayName("consumir es idempotente si el token ya no existe")
    void consumir_idempotente() {
        when(valueOps.get(startsWith("pwreset:"))).thenReturn(null);

        store.consume("token-crudo");

        verify(redis).delete(startsWith("pwreset:"));
        verify(redis, never()).delete(startsWith("pwreset:user:"));
    }

    @Test
    @DisplayName("resolver devuelve el mismo usuario que se guardo (ida y vuelta del hash)")
    void guardar_y_resolver_coherentes() {
        when(valueOps.get("pwreset:user:ana.torres")).thenReturn(null);
        store.save("ana.torres", "T0k3n-Cru2o", Duration.ofMinutes(30));

        // El set del token usa una clave pwreset:{sha256}; resolver debe pedir
        // esa misma clave para el mismo token crudo.
        var claveToken = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(claveToken.capture(), eq("ana.torres"), eq(Duration.ofMinutes(30)));

        when(valueOps.get(claveToken.getValue())).thenReturn("ana.torres");
        assertThat(store.resolve("T0k3n-Cru2o")).contains("ana.torres");
    }
}
