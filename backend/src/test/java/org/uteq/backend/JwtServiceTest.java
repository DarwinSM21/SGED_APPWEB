package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.auth.security.JwtService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la emisión y validación del JWT con los siete claims
 * estándar del RFC 7519 (Bloque A.1).
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "CLAVE_DE_PRUEBA_SGED_MINIMO_32_CARACTERES_2026");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604800000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "sged-backend");
        ReflectionTestUtils.setField(jwtService, "audience", "sged-frontend");
    }

    @Test
    void token_valido_con_iss_y_aud_correctos() {
        String token = jwtService.generateToken("admin", "ADMINISTRADOR");
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("admin", jwtService.extractUsername(token));
        assertEquals("ADMINISTRADOR", jwtService.extractRol(token));
        assertNotNull(jwtService.extractJti(token));
    }

    @Test
    void token_con_audiencia_distinta_es_invalido() {
        ReflectionTestUtils.setField(jwtService, "audience", "otro-publico");
        String token = jwtService.generateToken("admin", "USER");
        // Restaurar audiencia esperada: el token emitido para otro público no valida
        ReflectionTestUtils.setField(jwtService, "audience", "sged-frontend");
        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void token_manipulado_es_invalido() {
        String token = jwtService.generateToken("admin", "USER");
        assertFalse(jwtService.isTokenValid(token + "x"));
    }

    @Test
    void refresh_token_valido() {
        String refresh = jwtService.generateRefreshToken("admin", "USER");
        assertTrue(jwtService.isTokenValid(refresh));
    }
}
