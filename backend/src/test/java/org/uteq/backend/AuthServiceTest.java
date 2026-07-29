package org.uteq.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.seguridad.auth.controller.AuthController;
import org.uteq.backend.seguridad.auth.dto.LoginRequest;
import org.uteq.backend.seguridad.auth.dto.RegisterRequest;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.security.LoginAttemptService;
import org.uteq.backend.seguridad.auth.security.RedisBlacklistService;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.rol.repository.RolRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RedisBlacklistService blacklistService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginAttemptService loginAttemptService;

    @InjectMocks private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"));
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

        RegisterRequest registerRequest = new RegisterRequest("Test", "User", "test@test.com", "test123");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void registroExitoso() throws Exception {
        when(usuarioRepository.existsByUsername("new@test.com")).thenReturn(false);
        when(personaRepository.save(any(Persona.class))).thenAnswer(i -> {
            Persona p = i.getArgument(0);
            p.setIdPersona(1L);
            return p;
        });
        when(rolRepository.findByNombre("USER")).thenReturn(
                Optional.of(Rol.builder().idRol(1L).nombre("USER").build()));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setIdUsuario(1L);
            return u;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");

        RegisterRequest registerRequest = new RegisterRequest("Test", "User", "new@test.com", "password123");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new@test.com"))
                .andExpect(jsonPath("$.nombre").value("Test User"))
                .andExpect(jsonPath("$.rol").value("USER"));
    }

    @Test
    void pingRespondePong() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }
}