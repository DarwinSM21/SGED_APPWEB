package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.seguridad.auth.security.SessionEpochService;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionEpochServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private SessionEpochService service;

    @BeforeEach
    void setUp() {
        service = new SessionEpochService(redis);
        ReflectionTestUtils.setField(service, "refreshExpirationMs", Duration.ofDays(7).toMillis());
    }

    @Test
    @DisplayName("marcar escribe la epoca actual con el TTL del refresh token")
    void marcar_escribe_epoca() {
        when(redis.opsForValue()).thenReturn(valueOps);

        service.marcar("Ana.Torres");

        verify(valueOps).set(eq("sec:pwepoch:ana.torres"), anyString(),
                eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("sin marca, ningun token queda invalidado")
    void sin_marca_no_invalida() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("sec:pwepoch:ana.torres")).thenReturn(null);

        assertThat(service.invalidadoPorReseteo("ana.torres", Instant.now())).isFalse();
    }

    @Test
    @DisplayName("token emitido antes de la epoca: invalidado")
    void token_anterior_a_la_epoca_invalidado() {
        when(redis.opsForValue()).thenReturn(valueOps);
        long ahora = Instant.now().getEpochSecond();
        when(valueOps.get("sec:pwepoch:ana.torres")).thenReturn(Long.toString(ahora));

        assertThat(service.invalidadoPorReseteo("ana.torres", Instant.ofEpochSecond(ahora - 60))).isTrue();
    }

    @Test
    @DisplayName("token emitido despues de la epoca: valido")
    void token_posterior_a_la_epoca_valido() {
        when(redis.opsForValue()).thenReturn(valueOps);
        long ahora = Instant.now().getEpochSecond();
        when(valueOps.get("sec:pwepoch:ana.torres")).thenReturn(Long.toString(ahora - 120));

        assertThat(service.invalidadoPorReseteo("ana.torres", Instant.ofEpochSecond(ahora))).isFalse();
    }

    @Test
    @DisplayName("iat nulo nunca invalida")
    void iat_nulo_no_invalida() {
        assertThat(service.invalidadoPorReseteo("ana.torres", null)).isFalse();
    }
}
