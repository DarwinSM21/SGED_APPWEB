package org.uteq.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
import org.uteq.backend.common.exception.ApiException;
import org.uteq.backend.common.exception.TooManyRequestsException;
import org.uteq.backend.seguridad.audit.service.AuditService;
import org.uteq.backend.seguridad.auth.dto.LoginRequest;
import org.uteq.backend.seguridad.auth.dto.RegisterRequest;
import org.uteq.backend.seguridad.auth.PasswordPolicy;
import org.uteq.backend.seguridad.auth.dto.SessionResponse;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.security.LoginAttemptService;
import org.uteq.backend.seguridad.auth.security.RedisBlacklistService;
import org.uteq.backend.seguridad.auth.service.AuthService;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.role.entity.Role;
import org.uteq.backend.seguridad.role.repository.RoleRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RedisBlacklistService blacklistService;
    @Mock private UserAccountRepository usuarioRepository;
    @Mock private PersonRepository personaRepository;
    @Mock private RoleRepository rolRepository;
    @Mock private GeneralStatusRepository estadoGeneralRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Spy private PasswordPolicy passwordPolicy = new PasswordPolicy();
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private AuditService auditoriaService;
    @Mock private org.uteq.backend.seguridad.auth.service.EmailVerificationService emailVerificationService;

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

    @Test
    void loginConCredencialesCorrectasDevuelveTokensYSesion() {
        UserDetails userDetails = mockUser("admin@test.com", "ROLE_ADMINISTRADOR");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("mock-jwt-token");
        when(jwtService.generateRefreshToken(anyString(), anyString())).thenReturn("mock-refresh-token");

        Person persona = Person.builder().name("Admin").lastName("SGED").active(true).build();
        UserAccount usuario = UserAccount.builder().username("admin@test.com").person(persona)
                .roles(Set.of(Role.builder().name("ADMINISTRADOR").build())).build();

        when(usuarioRepository.findByUsernameAndActivoTrue("admin@test.com")).thenReturn(Optional.of(usuario));

        AuthService.LoginResult resultado = authService.login(
                new LoginRequest("admin@test.com", "Admin2026!"), "127.0.0.1");

        assertThat(resultado.accessToken()).isEqualTo("mock-jwt-token");
        assertThat(resultado.refreshToken()).isEqualTo("mock-refresh-token");
        assertThat(resultado.session().getUsername()).isEqualTo("admin@test.com");
        assertThat(resultado.session().getNombre()).isEqualTo("Admin SGED");
        assertThat(resultado.session().getRol()).isEqualTo("ADMINISTRADOR");
    }

    @Test
    void loginSinFichaDePersonaUsaUsernameComoNombre() {
        UserDetails userDetails = mockUser("sinficha@test.com", "ROLE_ENTRENADOR");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("mock-jwt-token");
        when(jwtService.generateRefreshToken(anyString(), anyString())).thenReturn("mock-refresh-token");
        when(usuarioRepository.findByUsernameAndActivoTrue("sinficha@test.com")).thenReturn(Optional.empty());

        AuthService.LoginResult resultado = authService.login(
                new LoginRequest("sinficha@test.com", "Admin2026!"), "127.0.0.1");

        assertThat(resultado.session().getNombre()).isEqualTo("sinficha@test.com");
    }

    @Test
    void loginConContrasenaIncorrectaLanzaBadCredentialsYRegistraFallo() {
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        LoginRequest loginRequest = new LoginRequest("admin@test.com", "WrongPass");

        assertThatThrownBy(() -> authService.login(loginRequest, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).recordFailure("127.0.0.1");
    }

    @Test
    void loginConIpBloqueadaLanzaTooManyRequestsSinAutenticar() {
        when(loginAttemptService.isBlocked("10.0.0.1")).thenReturn(true);

        LoginRequest loginRequest = new LoginRequest("admin@test.com", "Admin2026!");

        assertThatThrownBy(() -> authService.login(loginRequest, "10.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class);

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void registrarConUsernameDuplicadoDevuelveVacio() {
        when(usuarioRepository.existsByUsernameIgnoreCase("test@test.com")).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345678", "test@test.com",
                LocalDate.of(2000, 1, 1), "test@test.com", "clave1234", "ENTRENADOR");

        assertThat(authService.register(registerRequest)).isEmpty();
        verify(personaRepository, never()).save(any());
    }

    @Test
    void registrarConCedulaDuplicadaDevuelveVacio() {
        when(usuarioRepository.existsByUsernameIgnoreCase("cedula.dup@test.com")).thenReturn(false);
        when(personaRepository.existsByCedulaAndActivoTrue("0912345678")).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345678", "cedula.dup.correo@test.com",
                LocalDate.of(2000, 1, 1), "cedula.dup@test.com", "clave1234", "ENTRENADOR");

        assertThat(authService.register(registerRequest)).isEmpty();
        verify(personaRepository, never()).save(any());
    }

    @Test
    void registrarConCorreoDuplicadoDevuelveVacio() {
        when(usuarioRepository.existsByUsernameIgnoreCase("correo.dup@test.com")).thenReturn(false);
        when(personaRepository.existsByCedulaAndActivoTrue("0912345681")).thenReturn(false);
        when(personaRepository.existsByCorreo("correo.dup.persona@test.com")).thenReturn(true);

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345681", "correo.dup.persona@test.com",
                LocalDate.of(2000, 1, 1), "correo.dup@test.com", "clave1234", "ENTRENADOR");

        assertThat(authService.register(registerRequest)).isEmpty();
        verify(personaRepository, never()).save(any());
    }

    @Test
    void registrarExitosoCreaPersonaYUsuario() {
        when(usuarioRepository.existsByUsernameIgnoreCase("new@test.com")).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(i -> {
            Person p = i.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(rolRepository.findByNombre("ENTRENADOR")).thenReturn(
                Optional.of(Role.builder().id(2L).name("ENTRENADOR").build()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(
                Optional.of(GeneralStatus.builder().id(1L).build()));
        when(usuarioRepository.save(any(UserAccount.class))).thenAnswer(i -> {
            UserAccount u = i.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345678", "nuevo.correo@test.com",
                LocalDate.of(2000, 1, 1), "new@test.com", "password123", "ENTRENADOR");

        Optional<SessionResponse> resultado = authService.register(registerRequest);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getUsername()).isEqualTo("new@test.com");
        assertThat(resultado.get().getNombre()).isEqualTo("Test User");
        assertThat(resultado.get().getRol()).isEqualTo("ENTRENADOR");
    }

    @Test
    void registrarConRolInexistenteLanzaIllegalArgumentException() {
        when(usuarioRepository.existsByUsernameIgnoreCase("otro@test.com")).thenReturn(false);
        when(rolRepository.findByNombre("SUPERADMIN")).thenReturn(Optional.empty());

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345680", "otro.correo@test.com",
                LocalDate.of(2000, 1, 1), "otro@test.com", "password123", "SUPERADMIN");

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class);

        verify(personaRepository, never()).save(any());
    }

    @Test
    void registrarConContrasenaDebilLanza422() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345679", "debil.persona@test.com",
                LocalDate.of(2000, 1, 1), "debil@test.com", "corta1", "ENTRENADOR");

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ApiException.class);
        verify(personaRepository, never()).save(any());
    }

    @Test
    void registrarSinCatalogoEstadoGeneralLanzaIllegalStateException() {
        when(usuarioRepository.existsByUsernameIgnoreCase("sinestado@test.com")).thenReturn(false);
        when(personaRepository.existsByCedulaAndActivoTrue("0912345682")).thenReturn(false);
        when(personaRepository.existsByCorreo("sinestado.persona@test.com")).thenReturn(false);
        when(personaRepository.save(any(Person.class))).thenAnswer(i -> {
            Person p = i.getArgument(0);
            p.setId(9L);
            return p;
        });
        when(rolRepository.findByNombre("ENTRENADOR")).thenReturn(
                Optional.of(Role.builder().id(2L).name("ENTRENADOR").build()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.empty());

        RegisterRequest registerRequest = new RegisterRequest(
                "Test", "User", "0912345682", "sinestado.persona@test.com",
                LocalDate.of(2000, 1, 1), "sinestado@test.com", "clave1234", "ENTRENADOR");

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void logoutConTokenValidoRevocaYAudita() {
        when(jwtService.extractJti("token-valido")).thenReturn("jti-123");
        when(jwtService.getExpirationMs()).thenReturn(900_000L);

        authService.logout("token-valido");

        verify(blacklistService).revoke("jti-123", 900_000L);
        verify(auditoriaService).recordEvent(eq("LOGOUT"), eq("Usuario"), isNull(), anyString());
    }

    @Test
    void logoutSinTokenNoIntentaRevocar() {
        authService.logout(null);

        verify(jwtService, never()).extractJti(any());
        verify(blacklistService, never()).revoke(any(), anyLong());
    }

    @Test
    void logoutConTokenCorruptoIgnoraLaExcepcion() {
        when(jwtService.extractJti("token-corrupto")).thenThrow(new RuntimeException("token malformado"));

        authService.logout("token-corrupto");

        verify(blacklistService, never()).revoke(any(), anyLong());
        verify(auditoriaService).recordEvent(eq("LOGOUT"), eq("Usuario"), isNull(), anyString());
    }

    @Test
    void refrescarConTokenInvalidoDevuelveVacio() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        assertThat(authService.refresh("bad-token")).isEmpty();
    }

    @Test
    void refrescarConTokenValidoDevuelveNuevoAccessToken() {
        when(jwtService.isTokenValid("good-token")).thenReturn(true);
        when(jwtService.extractUsername("good-token")).thenReturn("admin@test.com");
        when(jwtService.extractRole("good-token")).thenReturn("ADMINISTRADOR");
        when(jwtService.generateToken("admin@test.com", "ADMINISTRADOR")).thenReturn("nuevo-access-token");

        assertThat(authService.refresh("good-token")).contains("nuevo-access-token");
    }

    @Test
    void obtenerSesionActualDevuelveLaSesion() {
        UserDetails userDetails = mockUser("admin@test.com", "ROLE_ADMINISTRADOR");
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Person persona = Person.builder().name("Admin").lastName("SGED").active(true).build();
        UserAccount usuario = UserAccount.builder().username("admin@test.com").person(persona)
                .roles(Set.of(Role.builder().name("ADMINISTRADOR").build())).build();
        when(usuarioRepository.findByUsername("admin@test.com")).thenReturn(Optional.of(usuario));

        Optional<SessionResponse> resultado = authService.getCurrentSession();

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getUsername()).isEqualTo("admin@test.com");
        assertThat(resultado.get().getNombre()).isEqualTo("Admin SGED");
        assertThat(resultado.get().getRol()).isEqualTo("ADMINISTRADOR");
    }

    @Test
    void obtenerSesionActualSinFichaUsaUsernameComoNombre() {
        UserDetails userDetails = mockUser("huerfano@test.com", "ROLE_ENTRENADOR");
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByUsername("huerfano@test.com")).thenReturn(Optional.empty());

        Optional<SessionResponse> resultado = authService.getCurrentSession();

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("huerfano@test.com");
    }

    @Test
    void obtenerSesionActualSinAutenticarDevuelveVacio() {
        assertThat(authService.getCurrentSession()).isEmpty();
    }
}
