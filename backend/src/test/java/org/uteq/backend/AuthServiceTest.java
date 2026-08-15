package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.seguridad.auditoria.service.AuditoriaService;
import org.uteq.backend.seguridad.auth.controller.AuthController;
import org.uteq.backend.seguridad.auth.dto.LoginRequest;
import org.uteq.backend.seguridad.auth.dto.RegisterRequest;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.security.LoginAttemptService;
import org.uteq.backend.seguridad.auth.security.RedisBlacklistService;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.rol.repository.RolRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private MockMvc mockMvc;
    // JavaTimeModule: RegisterRequest incluye una LocalDate (fechaNacimiento).
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RedisBlacklistService blacklistService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private RolRepository rolRepository;
    @Mock private EstadoGeneralRepository estadoGeneralRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails mockUser(String username, String rol) {
        return User.builder()
                .username(username)
                .password("$2a$12$hashedpassword")
                .authorities(List.of(new SimpleGrantedAuthority(rol)))
                .build();
    }

    @Test
    void loginConCredencialesCorrectas() throws Exception {
        UserDetails userDetails = mockUser("admin@test.com", "ROLE_ADMINISTRADOR");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(loginAttemptService.estaBloqueada(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("mock-jwt-token");
        when(jwtService.generateRefreshToken(anyString(), anyString())).thenReturn("mock-refresh-token");

        Persona persona = Persona.builder().nombre("Admin").apellido("SGED").activo(true).build();
        Usuario usuario = Usuario.builder().username("admin@test.com").persona(persona)
                .roles(Set.of(Rol.builder().nombre("ADMINISTRADOR").build())).build();

        when(usuarioRepository.findByUsernameAndActivoTrue("admin@test.com")).thenReturn(Optional.of(usuario));

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
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void loginConContrasenaIncorrecta() throws Exception {
        when(loginAttemptService.estaBloqueada(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        LoginRequest loginRequest = new LoginRequest("admin@test.com", "WrongPass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroEmailDuplicado() throws Exception {
        when(usuarioRepository.existsByUsername("test@test.com")).thenReturn(true);

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
     * real. Las pruebas con repositorio mockeado no podian detectarlo, porque
     * la restriccion la aplica PostgreSQL y no el codigo Java. Esta prueba
     * cubre el hueco por el lado que si es verificable sin base de datos:
     * que la peticion sin esos campos ni siquiera pase la validacion.
     */
    @Test
    void registroSinCedulaNiFechaNacimientoNoLlegaALaBaseDeDatos() throws Exception {
        String cuerpoIncompleto = """
                {"nombre":"Test","apellido":"User",
                 "username":"nuevo@test.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoIncompleto))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void registroExitoso() throws Exception {
        when(usuarioRepository.existsByUsername("new@test.com")).thenReturn(false);
        when(personaRepository.save(any(Persona.class))).thenAnswer(i -> {
            Persona p = i.getArgument(0);
            p.setIdPersona(1L);
            return p;
        });
        when(rolRepository.findByNombre("ENTRENADOR")).thenReturn(
                Optional.of(Rol.builder().idRol(2L).nombre("ENTRENADOR").build()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(
                Optional.of(EstadoGeneral.builder().idEstadoGeneral(1L).build()));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setIdUsuario(1L);
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345678", "nuevo.correo@test.com",
                LocalDate.of(2000, 1, 1), "new@test.com", "password123", "ENTRENADOR");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new@test.com"))
                .andExpect(jsonPath("$.nombre").value("Test User"))
                .andExpect(jsonPath("$.rol").value("ENTRENADOR"));
    }

    /** rol es obligatorio en RegisterRequest: en blanco o ausente responde 422, nunca cae a un rol por defecto. */
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

        verify(personaRepository, never()).save(any());
    }

    /**
     * rol permite crear cuentas de cualquiera de los roles reales del
     * sistema (REPRESENTANTE/RECEPCIONISTA/ENTRENADOR/ESTUDIANTE/
     * ADMINISTRADOR) con el mismo endpoint. Este caso prueba que se usa el
     * valor pedido tal cual, sin transformarlo.
     */
    @Test
    void registroConRolExplicitoUsaElRolPedido() throws Exception {
        when(usuarioRepository.existsByUsername("rep@test.com")).thenReturn(false);
        when(personaRepository.save(any(Persona.class))).thenAnswer(i -> {
            Persona p = i.getArgument(0);
            p.setIdPersona(2L);
            return p;
        });
        when(rolRepository.findByNombre("REPRESENTANTE")).thenReturn(
                Optional.of(Rol.builder().idRol(4L).nombre("REPRESENTANTE").build()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(
                Optional.of(EstadoGeneral.builder().idEstadoGeneral(1L).build()));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setIdUsuario(2L);
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");

        RegisterRequest registerRequest = new RegisterRequest(
                "Ana", "Vera", "0912345679", "ana@test.com",
                LocalDate.of(1985, 1, 1), "rep@test.com", "password123", "REPRESENTANTE");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("REPRESENTANTE"))
                .andExpect(jsonPath("$.idPersona").value(2))
                .andExpect(jsonPath("$.idUsuario").value(2));
    }

    /** Un rol que no existe en seguridad.roles responde 400, no crea nada a medias. */
    @Test
    void registroConRolInexistenteDa400() throws Exception {
        when(usuarioRepository.existsByUsername("otro@test.com")).thenReturn(false);
        when(rolRepository.findByNombre("SUPERADMIN")).thenReturn(Optional.empty());

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345680", "otro.correo@test.com",
                LocalDate.of(2000, 1, 1), "otro@test.com", "password123", "SUPERADMIN");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verify(personaRepository, never()).save(any());
    }

    @Test
    void pingRespondePong() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void loginBloqueadaPorIntentosFallidosDa429() throws Exception {
        when(loginAttemptService.estaBloqueada(anyString())).thenReturn(true);

        LoginRequest loginRequest = new LoginRequest("admin@test.com", "cualquiera");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests());

        verify(authenticationManager, never()).authenticate(any());
    }

    /** Cuenta autenticada cuya Persona no se pudo resolver: el nombre cae al username, no rompe el login. */
    @Test
    void loginSinFichaDePersonaUsaUsernameComoNombre() throws Exception {
        UserDetails userDetails = mockUser("sinficha@test.com", "ROLE_ENTRENADOR");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(loginAttemptService.estaBloqueada(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("mock-jwt-token");
        when(jwtService.generateRefreshToken(anyString(), anyString())).thenReturn("mock-refresh-token");
        when(usuarioRepository.findByUsernameAndActivoTrue("sinficha@test.com")).thenReturn(Optional.empty());

        LoginRequest loginRequest = new LoginRequest("sinficha@test.com", "Admin2026!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("sinficha@test.com"));
    }

    @Test
    void registroCedulaDuplicadaDa409() throws Exception {
        when(usuarioRepository.existsByUsername("cedula.dup@test.com")).thenReturn(false);
        when(personaRepository.existsByCedulaAndActivoTrue("0912345678")).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345678", "cedula.dup.correo@test.com",
                LocalDate.of(2000, 1, 1), "cedula.dup@test.com", "test123", "ENTRENADOR");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());

        verify(personaRepository, never()).save(any());
    }

    @Test
    void registroCorreoDuplicadoDa409() throws Exception {
        when(usuarioRepository.existsByUsername("correo.dup@test.com")).thenReturn(false);
        when(personaRepository.existsByCedulaAndActivoTrue("0912345681")).thenReturn(false);
        when(personaRepository.existsByCorreo("correo.dup.persona@test.com")).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345681", "correo.dup.persona@test.com",
                LocalDate.of(2000, 1, 1), "correo.dup@test.com", "test123", "ENTRENADOR");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());

        verify(personaRepository, never()).save(any());
    }

    /** id_estado_general=1 falta en el catalogo (seed no aplicado): 500, no una excepcion sin manejar. */
    @Test
    void registroSinCatalogoEstadoGeneralDa500() throws Exception {
        when(usuarioRepository.existsByUsername("sinestado@test.com")).thenReturn(false);
        when(personaRepository.existsByCedulaAndActivoTrue("0912345682")).thenReturn(false);
        when(personaRepository.existsByCorreo("sinestado.persona@test.com")).thenReturn(false);
        when(personaRepository.save(any(Persona.class))).thenAnswer(i -> {
            Persona p = i.getArgument(0);
            p.setIdPersona(9L);
            return p;
        });
        when(rolRepository.findByNombre("ENTRENADOR")).thenReturn(
                Optional.of(Rol.builder().idRol(2L).nombre("ENTRENADOR").build()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.empty());

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345682", "sinestado.persona@test.com",
                LocalDate.of(2000, 1, 1), "sinestado@test.com", "test123", "ENTRENADOR");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void logoutConCookieRevocaElToken() throws Exception {
        when(jwtService.extractJti("token-valido")).thenReturn("jti-123");
        when(jwtService.getExpirationMs()).thenReturn(900_000L);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("sged_access", "token-valido")))
                .andExpect(status().isNoContent());

        verify(blacklistService).revocar("jti-123", 900_000L);
        verify(auditoriaService).registrar(eq("LOGOUT"), eq("Usuario"), isNull(), anyString());
    }

    @Test
    void logoutSinCookieNoIntentaRevocar() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verify(jwtService, never()).extractJti(any());
        verify(blacklistService, never()).revocar(any(), anyLong());
    }

    /** Cookie presente pero con un JWT corrupto: extractJti lanza, logout igual responde 204. */
    @Test
    void logoutConTokenInvalidoIgnoraLaExcepcionYContinua() throws Exception {
        when(jwtService.extractJti("token-corrupto")).thenThrow(new RuntimeException("token malformado"));

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("sged_access", "token-corrupto")))
                .andExpect(status().isNoContent());

        verify(blacklistService, never()).revocar(any(), anyLong());
    }

    @Test
    void refreshSinCookieDa401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshConTokenInvalidoDa401() throws Exception {
        when(jwtService.isTokenValid("refresh-invalido")).thenReturn(false);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("sged_refresh", "refresh-invalido")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshConTokenValidoEmiteNuevoAccessToken() throws Exception {
        when(jwtService.isTokenValid("refresh-valido")).thenReturn(true);
        when(jwtService.extractUsername("refresh-valido")).thenReturn("admin@test.com");
        when(jwtService.extractRol("refresh-valido")).thenReturn("ADMINISTRADOR");
        when(jwtService.generateToken("admin@test.com", "ADMINISTRADOR")).thenReturn("nuevo-access");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("sged_refresh", "refresh-valido")))
                .andExpect(status().isNoContent());
    }

    @Test
    void meSinAutenticarDa401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meAutenticadoDevuelveLaSesion() throws Exception {
        UserDetails userDetails = mockUser("admin@test.com", "ROLE_ADMINISTRADOR");
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Persona persona = Persona.builder().nombre("Admin").apellido("SGED").activo(true).build();
        Usuario usuario = Usuario.builder().username("admin@test.com").persona(persona)
                .roles(Set.of(Rol.builder().nombre("ADMINISTRADOR").build())).build();
        when(usuarioRepository.findByUsername("admin@test.com")).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin@test.com"))
                .andExpect(jsonPath("$.nombre").value("Admin SGED"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    /** Sesion autenticada pero cuya Persona ya no existe: el nombre cae al username, /me no rompe. */
    @Test
    void meAutenticadoSinFichaUsaUsernameComoNombre() throws Exception {
        UserDetails userDetails = mockUser("huerfano@test.com", "ROLE_ENTRENADOR");
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByUsername("huerfano@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("huerfano@test.com"));
    }
}