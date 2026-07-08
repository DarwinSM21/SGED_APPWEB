package org.uteq.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import org.uteq.backend.common.exception.ConflictoException;
import org.uteq.backend.common.exception.CredencialesInvalidasException;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import java.util.List;

@Slf4j
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

    @Transactional
    public Usuario registro(RegistroRequest request) {
        if (usuarioRepository.findByUsername(request.username()).isPresent()) {
            throw new ConflictoException("El username ya existe");
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

        usuarioRolRepository.save(new UsuarioRol(null, usuario, rol));
        return usuario;
    }

    /**
     * Autentica al usuario. El parámetro ip se usa exclusivamente para el
     * registro de auditoría exigido por OWASP A09 (Bloque C.2): cada login
     * exitoso o fallido queda en el log con ip, timestamp y sub.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String ip) {
        Usuario usuario = usuarioRepository
                .findByUsername(request.username())
                .orElseThrow(() -> loginFallido(ip, request.username()));

        if (usuario.getActivo() == null || !usuario.getActivo()) {
            throw loginFallido(ip, request.username());
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw loginFallido(ip, request.username());
        }

        List<UsuarioRol> roles = usuarioRolRepository.findByUsuario(usuario);
        String rol = roles.isEmpty() ? "USER" : roles.get(0).getRol().getNombre();

        String token = jwtService.generateToken(usuario.getUsername(), rol);
        String refreshToken = jwtService.generateRefreshToken(usuario.getUsername(), rol);

        // A09: registro de acceso exitoso con ip, timestamp (lo añade el appender) y sub
        log.info("AUTH_LOGIN_OK ip={} sub={}", ip, usuario.getUsername());

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(usuario.getUsername())
                .nombre(usuario.getPersona().getNombre()
                        + " " + usuario.getPersona().getApellido())
                .rol(rol)
                .build();
    }

    private CredencialesInvalidasException loginFallido(String ip, String username) {
        // A09: registro de acceso fallido con ip, timestamp y sub intentado
        log.warn("AUTH_LOGIN_FAIL ip={} sub={}", ip, username);
        return new CredencialesInvalidasException("Usuario o contraseña incorrectos");
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)
                || blacklistService.estaRevocado(jwtService.extractJti(refreshToken))) {
            throw new CredencialesInvalidasException("Token de refresco inválido o expirado");
        }

        String username = jwtService.extractUsername(refreshToken);
        String rol = jwtService.extractRol(refreshToken);

        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

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

    /** Revoca el token (por su jti) recibido como valor crudo del JWT. */
    public void logout(String token, String ip) {
        if (token != null && !token.isBlank()) {
            try {
                String jti = jwtService.extractJti(token);
                blacklistService.revocarToken(jti, jwtService.getExpirationMs());
                log.info("AUTH_LOGOUT ip={} sub={}", ip, jwtService.extractUsername(token));
            } catch (Exception e) {
                log.warn("AUTH_LOGOUT_TOKEN_INVALIDO ip={}", ip);
            }
        }
    }
}
