package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.uteq.backend.seguridad.auth.EmailVerificationTokenStore;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenStoreTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private EmailVerificationTokenStore store;

    @BeforeEach
    void setUp() {
        store = new EmailVerificationTokenStore(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("guardar escribe la clave del token y el indice de persona con el TTL")
    void guardar_escribe_ambas_claves() {
        when(valueOps.get("emailverify:persona:1")).thenReturn(null);

        store.save(1L, "token-crudo", Duration.ofHours(48));

        verify(valueOps).set(startsWith("emailverify:"), eq("1"), eq(Duration.ofHours(48)));
        verify(valueOps).set(eq("emailverify:persona:1"), anyString(), eq(Duration.ofHours(48)));
        verify(redis, never()).delete(anyString());
    }

    @Test
    @DisplayName("guardar invalida el token anterior de la misma persona")
    void guardar_invalida_el_anterior() {
        when(valueOps.get("emailverify:persona:1")).thenReturn("hash-viejo");

        store.save(1L, "token-nuevo", Duration.ofHours(48));

        verify(redis).delete("emailverify:hash-viejo");
    }

    @Test
    @DisplayName("resolver devuelve vacio para token nulo, en blanco o inexistente")
    void resolver_token_invalido() {
        assertThat(store.resolve(null)).isEmpty();
        assertThat(store.resolve("  ")).isEmpty();

        when(valueOps.get(startsWith("emailverify:"))).thenReturn(null);
        assertThat(store.resolve("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("guardar y resolver son coherentes para el mismo token crudo")
    void guardar_y_resolver_coherentes() {
        when(valueOps.get("emailverify:persona:1")).thenReturn(null);
        store.save(1L, "T0k3n-Cru2o", Duration.ofHours(48));

        ArgumentCaptor<String> claveToken = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(claveToken.capture(), eq("1"), eq(Duration.ofHours(48)));

        when(valueOps.get(claveToken.getValue())).thenReturn("1");
        assertThat(store.resolve("T0k3n-Cru2o")).contains(1L);
    }

    @Test
    @DisplayName("consumir borra la clave del token y el indice de persona")
    void consumir_borra_ambas() {
        when(valueOps.get(startsWith("emailverify:"))).thenReturn("1");

        store.consume("token-crudo");

        verify(redis, times(2)).delete(startsWith("emailverify:"));
        verify(redis).delete("emailverify:persona:1");
    }

    @Test
    @DisplayName("consumir es idempotente si el token ya no existe")
    void consumir_idempotente() {
        when(valueOps.get(startsWith("emailverify:"))).thenReturn(null);

        store.consume("token-crudo");

        verify(redis).delete(startsWith("emailverify:"));
        verify(redis, never()).delete(startsWith("emailverify:persona:"));
    }
}
