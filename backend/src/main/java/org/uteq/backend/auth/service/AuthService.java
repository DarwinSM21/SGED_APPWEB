package org.uteq.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.auth.dto.LoginRequest;
import org.uteq.backend.auth.dto.LoginResponse;
import org.uteq.backend.auth.dto.RegistroRequest;
import org.uteq.backend.auth.entity.Persona;
import org.uteq.backend.auth.entity.Rol;
import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.entity.UsuarioRol;
import org.uteq.backend.auth.repository.PersonaRepository;
import org.uteq.backend.auth.repository.RolRepository;
import org.uteq.backend.auth.repository.UsuarioRepository;
import org.uteq.backend.auth.repository.UsuarioRolRepository;
import org.uteq.backend.auth.security.JwtService;
import org.uteq.backend.auth.security.RedisBlacklistService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisBlacklistService blacklistService;

    public Usuario registro(RegistroRequest request) {
        if (usuarioRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("El username ya existe");
        }

        Persona persona = Persona.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .activo(true)
                .build();
        personaRepository.save(persona);

        Usuario usuario = Usuario.builder()
                .persona(persona)
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .activo(true)
                .build();
        usuarioRepository.save(usuario);

        Rol rol = rolRepository.findByNombre("USER")
                .orElseGet(() -> rolRepository.save(
                        new Rol(null, "USER", "Usuario estandar")
                ));

        UsuarioRol usuarioRol = new UsuarioRol(null, usuario, rol);
        usuarioRolRepository.save(usuarioRol);

        return usuario;
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository
                .findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!usuario.getActivo()) {
            throw new RuntimeException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        String rol = usuario.getUsuarioRoles()
                .stream()
                .findFirst()
                .orElseThrow()
                .getRol()
                .getNombre();

        String token = jwtService.generateToken(usuario.getUsername(), rol);
        String refreshToken = jwtService.generateRefreshToken(usuario.getUsername(), rol);

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(usuario.getUsername())
                .nombre(usuario.getPersona().getNombre()
                        + " " + usuario.getPersona().getApellido())
                .rol(rol)
                .build();
    }

    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Token de refresco invalido o expirado");
        }

        String username = jwtService.extractUsername(refreshToken);
        String rol = jwtService.extractRol(refreshToken);

        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String nuevoToken = jwtService.generateToken(usuario.getUsername(), rol);

        return LoginResponse.builder()
                .token(nuevoToken)
                .refreshToken(refreshToken)
                .username(usuario.getUsername())
                .nombre(usuario.getPersona().getNombre()
                        + " " + usuario.getPersona().getApellido())
                .rol(rol)
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String jti = jwtService.extractJti(token);
            long ttl = jwtService.getExpirationMs();
            blacklistService.revocarToken(jti, ttl);
        }
    }
}