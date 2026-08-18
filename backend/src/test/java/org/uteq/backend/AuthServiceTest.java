package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.uteq.backend.common.exception.TooManyRequestsException;
import org.uteq.backend.seguridad.auditoria.service.AuditoriaService;
import org.uteq.backend.seguridad.auth.dto.LoginRequest;
import org.uteq.backend.seguridad.auth.dto.RegisterRequest;
import org.uteq.backend.seguridad.auth.dto.SesionResponse;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.security.LoginAttemptService;
import org.uteq.backend.seguridad.auth.security.RedisBlacklistService;
import org.uteq.backend.seguridad.auth.service.AuthService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de AuthService, sin contexto HTTP (D-03 / R-03 del
 * informe de evaluacion de calidad: antes esta logica vivia en
 * AuthController y solo podia probarse levantando MockMvc). Los
 * escenarios que solo tienen sentido a nivel HTTP (cookies, codigos de
 * estado sin autenticacion) viven en AuthControllerTest.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    @InjectMocks private AuthService authService;

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

    // --- login ---

    @Test
    void loginConCredencialesCorrectasDevuelveTokensYSesion() {
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

        AuthService.LoginResult resultado = authService.login(
                new LoginRequest("admin@test.com", "Admin2026!"), "127.0.0.1");

        assertThat(resultado.accessToken()).isEqualTo("mock-jwt-token");
        assertThat(resultado.refreshToken()).isEqualTo("mock-refresh-token");
        assertThat(resultado.sesion().getUsername()).isEqualTo("admin@test.com");
        assertThat(resultado.sesion().getNombre()).isEqualTo("Admin SGED");
        assertThat(resultado.sesion().getRol()).isEqualTo("ADMINISTRADOR");
    }

    /** Cuenta autenticada cuya Persona no se pudo resolver: el nombre cae al username, no rompe el login. */
    @Test
    void loginSinFichaDePersonaUsaUsernameComoNombre() {
        UserDetails userDetails = mockUser("sinficha@test.com", "ROLE_ENTRENADOR");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(loginAttemptService.estaBloqueada(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("mock-jwt-token");
        when(jwtService.generateRefreshToken(anyString(), anyString())).thenReturn("mock-refresh-token");
        when(usuarioRepository.findByUsernameAndActivoTrue("sinficha@test.com")).thenReturn(Optional.empty());

        AuthService.LoginResult resultado = authService.login(
                new LoginRequest("sinficha@test.com", "Admin2026!"), "127.0.0.1");

        assertThat(resultado.sesion().getNombre()).isEqualTo("sinficha@test.com");
    }

    @Test
    void loginConContrasenaIncorrectaLanzaBadCredentialsYRegistraFallo() {
        when(loginAttemptService.estaBloqueada(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        LoginRequest loginRequest = new LoginRequest("admin@test.com", "WrongPass");

        assertThatThrownBy(() -> authService.login(loginRequest, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).registrarFallo("127.0.0.1");
    }

    @Test
    void loginConIpBloqueadaLanzaTooManyRequestsSinAutenticar() {
        when(loginAttemptService.estaBloqueada("10.0.0.1")).thenReturn(true);

        LoginRequest loginRequest = new LoginRequest("admin@test.com", "Admin2026!");

        assertThatThrownBy(() -> authService.login(loginRequest, "10.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class);

        verify(authenticationManager, never()).authenticate(any());
    }

    // --- registrar ---

    @Test
    void registrarConUsernameDuplicadoDevuelveVacio() {
        when(usuarioRepository.existsByUsernameIgnoreCase("test@test.com")).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345678", "test@test.com",
                LocalDate.of(2000, 1, 1), "test@test.com", "test123", "ENTRENADOR");

        assertThat(authService.registrar(registerRequest)).isEmpty();
        verify(personaRepository, never()).save(any());
    }

    @Test
    void registrarConCedulaDuplicadaDevuelveVacio() {
        when(usuarioRepository.existsByUsernameIgnoreCase("cedula.dup@test.com")).thenReturn(false);
        when(personaRepository.existsByCedulaAndActivoTrue("0912345678")).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345678", "cedula.dup.correo@test.com",
                LocalDate.of(2000, 1, 1), "cedula.dup@test.com", "test123", "ENTRENADOR");

        assertThat(authService.registrar(registerRequest)).isEmpty();
        verify(personaRepository, never()).save(any());
    }

    @Test
    void registrarConCorreoDuplicadoDevuelveVacio() {
        when(usuarioRepository.existsByUsernameIgnoreCase("correo.dup@test.com")).thenReturn(false);
        when(personaRepository.existsByCedulaAndActivoTrue("0912345681")).thenReturn(false);
        when(personaRepository.existsByCorreo("correo.dup.persona@test.com")).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345681", "correo.dup.persona@test.com",
                LocalDate.of(2000, 1, 1), "correo.dup@test.com", "test123", "ENTRENADOR");

        assertThat(authService.registrar(registerRequest)).isEmpty();
        verify(personaRepository, never()).save(any());
    }

    @Test
    void registrarExitosoCreaPersonaYUsuario() {
        when(usuarioRepository.existsByUsernameIgnoreCase("new@test.com")).thenReturn(false);
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

        Optional<SesionResponse> resultado = authService.registrar(registerRequest);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getUsername()).isEqualTo("new@test.com");
        assertThat(resultado.get().getNombre()).isEqualTo("Test User");
        assertThat(resultado.get().getRol()).isEqualTo("ENTRENADOR");
    }

    /** Un rol que no existe en seguridad.roles no crea nada a medias. */
    @Test
    void registrarConRolInexistenteLanzaIllegalArgumentException() {
        when(usuarioRepository.existsByUsernameIgnoreCase("otro@test.com")).thenReturn(false);
        when(rolRepository.findByNombre("SUPERADMIN")).thenReturn(Optional.empty());

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345680", "otro.correo@test.com",
                LocalDate.of(2000, 1, 1), "otro@test.com", "password123", "SUPERADMIN");

        assertThatThrownBy(() -> authService.registrar(registerRequest))
                .isInstanceOf(IllegalArgumentException.class);

        verify(personaRepository, never()).save(any());
    }

    /** id_estado_general=1 falta en el catalogo (seed no aplicado): la excepcion sale sin capturar, no un guardado a medias. */
    @Test
    void registrarSinCatalogoEstadoGeneralLanzaIllegalStateException() {
        when(usuarioRepository.existsByUsernameIgnoreCase("sinestado@test.com")).thenReturn(false);
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

        assertThatThrownBy(() -> authService.registrar(registerRequest))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- logout ---

    @Test
    void logoutConTokenValidoRevocaYAudita() {
        when(jwtService.extractJti("token-valido")).thenReturn("jti-123");
        when(jwtService.getExpirationMs()).thenReturn(900_000L);

        authService.logout("token-valido");

        verify(blacklistService).revocar("jti-123", 900_000L);
        verify(auditoriaService).registrar(eq("LOGOUT"), eq("Usuario"), isNull(), anyString());
    }

    @Test
    void logoutSinTokenNoIntentaRevocar() {
        authService.logout(null);

        verify(jwtService, never()).extractJti(any());
        verify(blacklistService, never()).revocar(any(), anyLong());
    }

    /** Token presente pero corrupto: extractJti lanza, logout igual audita y no propaga la excepcion. */
    @Test
    void logoutConTokenCorruptoIgnoraLaExcepcion() {
        when(jwtService.extractJti("token-corrupto")).thenThrow(new RuntimeException("token malformado"));

        authService.logout("token-corrupto");

        verify(blacklistService, never()).revocar(any(), anyLong());
        verify(auditoriaService).registrar(eq("LOGOUT"), eq("Usuario"), isNull(), anyString());
    }

    // --- refrescar ---

    @Test
    void refrescarConTokenInvalidoDevuelveVacio() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        assertThat(authService.refrescar("bad-token")).isEmpty();
    }

    @Test
    void refrescarConTokenValidoDevuelveNuevoAccessToken() {
        when(jwtService.isTokenValid("good-token")).thenReturn(true);
        when(jwtService.extractUsername("good-token")).thenReturn("admin@test.com");
        when(jwtService.extractRol("good-token")).thenReturn("ADMINISTRADOR");
        when(jwtService.generateToken("admin@test.com", "ADMINISTRADOR")).thenReturn("nuevo-access-token");

        assertThat(authService.refrescar("good-token")).contains("nuevo-access-token");
    }

    // --- obtenerSesionActual ---

    @Test
    void obtenerSesionActualDevuelveLaSesion() {
        UserDetails userDetails = mockUser("admin@test.com", "ROLE_ADMINISTRADOR");
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Persona persona = Persona.builder().nombre("Admin").apellido("SGED").activo(true).build();
        Usuario usuario = Usuario.builder().username("admin@test.com").persona(persona)
                .roles(Set.of(Rol.builder().nombre("ADMINISTRADOR").build())).build();
        when(usuarioRepository.findByUsername("admin@test.com")).thenReturn(Optional.of(usuario));

        Optional<SesionResponse> resultado = authService.obtenerSesionActual();

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getUsername()).isEqualTo("admin@test.com");
        assertThat(resultado.get().getNombre()).isEqualTo("Admin SGED");
        assertThat(resultado.get().getRol()).isEqualTo("ADMINISTRADOR");
    }

    /** Sesion autenticada pero cuya Persona ya no existe: el nombre cae al username, no rompe. */
    @Test
    void obtenerSesionActualSinFichaUsaUsernameComoNombre() {
        UserDetails userDetails = mockUser("huerfano@test.com", "ROLE_ENTRENADOR");
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByUsername("huerfano@test.com")).thenReturn(Optional.empty());

        Optional<SesionResponse> resultado = authService.obtenerSesionActual();

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("huerfano@test.com");
    }

    @Test
    void obtenerSesionActualSinAutenticarDevuelveVacio() {
        assertThat(authService.obtenerSesionActual()).isEmpty();
    }
}
