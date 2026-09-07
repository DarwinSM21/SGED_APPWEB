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
import org.uteq.backend.seguridad.auth.dto.SessionResponse;
import org.uteq.backend.common.exception.ApiException;
import org.uteq.backend.common.exception.TooManyRequestsException;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.security.ResetRequestLimitService;
import org.uteq.backend.seguridad.auth.service.AuthService;
import org.uteq.backend.seguridad.auth.service.PasswordResetService;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock private AuthService authService;
    @Mock private PasswordResetService passwordResetService;
    @Mock private ResetRequestLimitService resetRequestLimitService;
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
        SessionResponse sesion = SessionResponse.builder()
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
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().exists("sged_access"))
                .andExpect(cookie().exists("sged_refresh"))
                .andExpect(cookie().httpOnly("sged_access", true));
    }

    @Test
    void registroDelegaEnAuthServiceYDevuelveConflictSiVacio() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(Optional.empty());

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345678", "test@test.com",
                LocalDate.of(2000, 1, 1), "test@test.com", "test123", "ENTRENADOR");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

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

        verify(authService, never()).register(any());
    }

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

        verify(authService, never()).register(any());
    }

    @Test
    void pingRespondePong() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

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

    @Test
    void refreshSinCookieDa401() throws Exception {
        when(authService.refresh(isNull())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshConTokenInvalidoDa401() throws Exception {
        when(authService.refresh("refresh-invalido")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("sged_refresh", "refresh-invalido")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshConTokenValidoPoneLaCookieYDevuelve204() throws Exception {
        when(authService.refresh("refresh-valido")).thenReturn(Optional.of("nuevo-access"));
        when(jwtService.getExpirationMs()).thenReturn(900_000L);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("sged_refresh", "refresh-valido")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value("sged_access", "nuevo-access"));
    }

    @Test
    void meSinAutenticarDa401() throws Exception {
        when(authService.getCurrentSession()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meAutenticadoDevuelveLaSesion() throws Exception {
        SessionResponse sesion = SessionResponse.builder()
                .username("admin@test.com").nombre("Admin SGED").rol("ADMINISTRADOR").build();
        when(authService.getCurrentSession()).thenReturn(Optional.of(sesion));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin@test.com"))
                .andExpect(jsonPath("$.nombre").value("Admin SGED"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    void forgotDevuelve202YMensajeGenerico() throws Exception {
        mockMvc.perform(post("/api/auth/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"ana@test.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").exists());

        verify(passwordResetService).solicitar("ana@test.com");
        verify(resetRequestLimitService).registrar("ana@test.com", "127.0.0.1");
    }

    @Test
    void forgotConCuentaInexistenteTambienDa202() throws Exception {
        // el servicio no lanza para identificador desconocido; el controlador responde igual
        mockMvc.perform(post("/api/auth/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"nadie@test.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void forgotConLimiteExcedidoDa429() throws Exception {
        doThrow(new TooManyRequestsException("demasiadas"))
                .when(resetRequestLimitService).check(anyString(), anyString());

        mockMvc.perform(post("/api/auth/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"ana@test.com\"}"))
                .andExpect(status().isTooManyRequests());

        verify(passwordResetService, never()).solicitar(anyString());
    }

    @Test
    void resetConTokenValidoDa204() throws Exception {
        mockMvc.perform(post("/api/auth/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"nuevaPassword\":\"clave1234\"}"))
                .andExpect(status().isNoContent());

        verify(passwordResetService).restablecer("tok", "clave1234");
    }

    @Test
    void resetConTokenInvalidoDa400() throws Exception {
        doThrow(new ApiException(HttpStatus.BAD_REQUEST, "enlace invalido"))
                .when(passwordResetService).restablecer(anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"malo\",\"nuevaPassword\":\"clave1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetConContrasenaDebilDa422() throws Exception {
        doThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "contrasena debil"))
                .when(passwordResetService).restablecer(anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"nuevaPassword\":\"corta1\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
