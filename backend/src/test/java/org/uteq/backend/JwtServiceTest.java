package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.uteq.backend.seguridad.auth.security.JwtService;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private final String SECRET_TEST = "CLAVE_DE_PRUEBA_SGED_MINIMO_32_CARACTERES_2026";
    private final String ISSUER_TEST = "sged-backend";
    private final String AUDIENCE_TEST = "sged-frontend";
    private final long EXPIRATION_MS = 3600000L; // 1 hora
    private final long REFRESH_EXPIRATION_MS = 604800000L; // 7 días

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET_TEST);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", REFRESH_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtService, "issuer", ISSUER_TEST);
        ReflectionTestUtils.setField(jwtService, "audience", AUDIENCE_TEST);
    }

    @Test
    @DisplayName("generateToken - Crea un token válido con issuer, audience, rol y JTI correctos")
    void token_valido_con_iss_y_aud_correctos() {
        String token = jwtService.generateToken("admin", "ADMINISTRADOR");

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("admin", jwtService.extractUsername(token));
        assertEquals("ADMINISTRADOR", jwtService.extractRol(token));
        assertNotNull(jwtService.extractJti(token));
    }

    @Test
    @DisplayName("isTokenValid - Retorna false si la audiencia esperada no coincide con la del token")
    void token_con_audiencia_distinta_es_invalido() {
        // Generamos un token con audiencia "otro-publico"
        ReflectionTestUtils.setField(jwtService, "audience", "otro-publico");
        String token = jwtService.generateToken("admin", "USER");

        // Restauramos la audiencia esperada ("sged-frontend")
        ReflectionTestUtils.setField(jwtService, "audience", AUDIENCE_TEST);

        // El token emitido para "otro-publico" debe ser rechazado
        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid - Retorna false cuando el token ha sido alterado o manipulado")
    void token_manipulado_es_invalido() {
        String token = jwtService.generateToken("admin", "USER");

        assertFalse(jwtService.isTokenValid(token + "x"));
    }

    @Test
    @DisplayName("generateRefreshToken - Crea un token de refresco válido")
    void refresh_token_valido() {
        String refresh = jwtService.generateRefreshToken("admin", "USER");

        assertTrue(jwtService.isTokenValid(refresh));
        assertEquals("admin", jwtService.extractUsername(refresh));
    }

    @Test
    @DisplayName("isTokenValid - Retorna false si el token está expirado")
    void token_expirado_es_invalido() {
        // Configuramos expiración negativa (expiró en el pasado)
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String tokenExpirado = jwtService.generateToken("admin", "USER");

        assertFalse(jwtService.isTokenValid(tokenExpirado));
    }

    @Test
    @DisplayName("Getters - Devuelven los tiempos de expiración configurados")
    void getters_expiracion_correctos() {
        assertEquals(EXPIRATION_MS, jwtService.getExpirationMs());
        assertEquals(REFRESH_EXPIRATION_MS, jwtService.getRefreshExpirationMs());
    }
}