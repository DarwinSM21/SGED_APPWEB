package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.seguridad.auth.controller.AuthController;
import org.uteq.backend.seguridad.auth.dto.LoginRequest;
import org.uteq.backend.seguridad.auth.dto.RegisterRequest;
import org.uteq.backend.seguridad.auth.dto.SesionResponse;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.service.AuthService;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de AuthController a nivel HTTP: validacion de @Valid, codigos de
 * estado, cookies y delegacion en AuthService (ya mockeado, no la logica
 * de negocio real: eso lo cubre AuthServiceTest).
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    // JavaTimeModule: RegisterRequest incluye una LocalDate (fechaNacimiento).
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock private AuthService authService;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginDelegaEnAuthServiceYPonelasCookies() throws Exception {
        SesionResponse sesion = SesionResponse.builder()
                .username("admin@test.com").nombre("Admin SGED").rol("ADMINISTRADOR").build();
        when(authService.login(any(LoginRequest.class), anyString()))
                .thenReturn(new AuthService.LoginResult("mock-jwt-token", "mock-refresh-token", sesion));
        when(jwtService.getExpirationMs()).thenReturn(900_000L);
        when(jwtService.getRefreshExpirationMs()).thenReturn(604_800_000L);

        LoginRequest loginRequest = new LoginRequest("admin@test.com", "Admin2026!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin@test.com"))
                .andExpect(jsonPath("$.nombre").value("Admin SGED"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                // El JWT viaja solo en la cookie HttpOnly (ADR-002/ADR-008).
                // Si algun dia vuelve a aparecer aqui, esta prueba debe
                // fallar: un token en el cuerpo es legible por cualquier
                // fetch/axios del frontend y anula esa proteccion.
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().exists("sged_access"))
                .andExpect(cookie().exists("sged_refresh"))
                .andExpect(cookie().httpOnly("sged_access", true));
    }

    @Test
    void registroDelegaEnAuthServiceYDevuelveConflictSiVacio() throws Exception {
        when(authService.registrar(any(RegisterRequest.class))).thenReturn(Optional.empty());

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345678", "test@test.com",
                LocalDate.of(2000, 1, 1), "test@test.com", "test123", "ENTRENADOR");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    /**
     * Regresion: la reestructuracion volvio cedula, correo y fecha_nacimiento
     * columnas NOT NULL de seguridad.personas, pero RegisterRequest no las
     * pedia, asi que /api/auth/registro fallaba siempre con 500 contra la base
     * real. Esta prueba cubre el hueco por el lado que si es verificable sin
     * base de datos: que la peticion sin esos campos ni siquiera pase la
     * validacion (por lo tanto ni siquiera llega a AuthService).
     */
    @Test
    void registroSinCedulaNiFechaNacimientoNoLlegaAlServicio() throws Exception {
        String cuerpoIncompleto = """
                {"nombre":"Test","apellido":"User",
                 "username":"nuevo@test.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoIncompleto))
                .andExpect(status().isUnprocessableEntity());

        verify(authService, never()).registrar(any());
    }

    /** rol es obligatorio en RegisterRequest: en blanco o ausente responde 422, nunca llega al servicio. */
    @Test
    void registroSinRolDa422() throws Exception {
        String cuerpoSinRol = """
                {"nombre":"Test","apellido":"User","cedula":"0912345678",
                 "correo":"sinrol@test.com","fechaNacimiento":"2000-01-01",
                 "username":"sinrol@test.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoSinRol))
                .andExpect(status().isUnprocessableEntity());

        verify(authService, never()).registrar(any());
    }

    @Test
    void pingRespondePong() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // --- logout ---

    @Test
    void logoutConCookieDelegaElValorYLimpiaLasCookies() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("sged_access", "token-valido")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("sged_access", 0))
                .andExpect(cookie().maxAge("sged_refresh", 0));

        verify(authService).logout("token-valido");
    }

    @Test
    void logoutSinCookieDelegaNull() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService).logout(isNull());
    }

    // --- refresh ---

    @Test
    void refreshSinCookieDa401() throws Exception {
        when(authService.refrescar(isNull())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshConTokenInvalidoDa401() throws Exception {
        when(authService.refrescar("refresh-invalido")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("sged_refresh", "refresh-invalido")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshConTokenValidoPoneLaCookieYDevuelve204() throws Exception {
        when(authService.refrescar("refresh-valido")).thenReturn(Optional.of("nuevo-access"));
        when(jwtService.getExpirationMs()).thenReturn(900_000L);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("sged_refresh", "refresh-valido")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value("sged_access", "nuevo-access"));
    }

    // --- me ---

    @Test
    void meSinAutenticarDa401() throws Exception {
        when(authService.obtenerSesionActual()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meAutenticadoDevuelveLaSesion() throws Exception {
        SesionResponse sesion = SesionResponse.builder()
                .username("admin@test.com").nombre("Admin SGED").rol("ADMINISTRADOR").build();
        when(authService.obtenerSesionActual()).thenReturn(Optional.of(sesion));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin@test.com"))
                .andExpect(jsonPath("$.nombre").value("Admin SGED"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }
}
