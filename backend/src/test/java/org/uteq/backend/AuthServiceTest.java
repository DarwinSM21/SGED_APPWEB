package org.uteq.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.uteq.backend.auth.dto.LoginRequest;
import org.uteq.backend.auth.entity.Rol;
import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.entity.UsuarioRol;
import org.uteq.backend.auth.repository.PersonaRepository;
import org.uteq.backend.auth.repository.RolRepository;
import org.uteq.backend.auth.repository.UsuarioRepository;
import org.uteq.backend.auth.repository.UsuarioRolRepository;
import org.uteq.backend.auth.security.JwtService;
import org.uteq.backend.auth.security.RedisBlacklistService;
import org.uteq.backend.auth.service.AuthService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PersonaRepository personaRepository;
    @Mock RolRepository rolRepository;
    @Mock UsuarioRolRepository usuarioRolRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RedisBlacklistService blacklistService;

    @InjectMocks AuthService authService;

    @Test
    void loginExitoso() {
        Rol rol = new Rol(1L, "USER", "Usuario");
        UsuarioRol usuarioRol = new UsuarioRol(1L, null, rol);
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .username("test@uteq.edu.ec")
                .passwordHash("hashedpass")
                .activo(true)
                .usuarioRoles(List.of(usuarioRol))
                .persona(org.uteq.backend.auth.entity.Persona.builder()
                        .nombre("Juan").apellido("Perez").build())
                .build();

        when(usuarioRepository.findByUsername("test@uteq.edu.ec"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hashedpass")).thenReturn(true);
        when(jwtService.generateToken("test@uteq.edu.ec", "USER")).thenReturn("jwt-token");

        var response = authService.login(new LoginRequest("test@uteq.edu.ec", "123456"));

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("USER", response.rol());
    }

    @Test
    void loginClaveIncorrecta() {
        Usuario usuario = Usuario.builder()
                .username("test@uteq.edu.ec")
                .passwordHash("hashedpass")
                .activo(true)
                .build();

        when(usuarioRepository.findByUsername("test@uteq.edu.ec"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrongpass", "hashedpass")).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                authService.login(new LoginRequest("test@uteq.edu.ec", "wrongpass"))
        );
    }

    @Test
    void loginUsuarioNoEncontrado() {
        when(usuarioRepository.findByUsername("noexiste@uteq.edu.ec"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                authService.login(new LoginRequest("noexiste@uteq.edu.ec", "123456"))
        );
    }

    @Test
    void registroUsernameDuplicado() {
        when(usuarioRepository.findByUsername("duplicado"))
                .thenReturn(Optional.of(new Usuario()));

        assertThrows(RuntimeException.class, () ->
                authService.registro(new org.uteq.backend.auth.dto.RegistroRequest(
                        "Juan", "Perez", "duplicado", "123456"))
        );
    }

    @Test
    void loginUsuarioInactivo() {
        Usuario usuario = Usuario.builder()
                .username("inactivo@uteq.edu.ec")
                .passwordHash("hashedpass")
                .activo(false)
                .build();

        when(usuarioRepository.findByUsername("inactivo@uteq.edu.ec"))
                .thenReturn(Optional.of(usuario));

        assertThrows(RuntimeException.class, () ->
                authService.login(new LoginRequest("inactivo@uteq.edu.ec", "123456"))
        );
    }
}