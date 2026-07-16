package org.uteq.backend.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.auth.dto.*;
import org.uteq.backend.auth.entity.Persona;
import org.uteq.backend.auth.entity.Rol;
import org.uteq.backend.auth.entity.Usuario;
import org.uteq.backend.auth.repository.PersonaRepository;
import org.uteq.backend.auth.repository.RolRepository;
import org.uteq.backend.auth.repository.UsuarioRepository;
import org.uteq.backend.auth.security.JwtService;
import org.uteq.backend.auth.security.RedisBlacklistService;

import java.time.Instant;
import java.util.Set;

/**
 * Controlador de autenticacion JWT.
 * Endpoints: registro, login, logout, refresh, /me.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisBlacklistService blacklistService;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/registro")
    public ResponseEntity<SesionResponse> registro(@Valid @RequestBody RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Persona persona = Persona.builder()
                .nombre(request.nombre())
                .apellido(request.apellido())
                .activo(true)
                .build();
        persona = personaRepository.save(persona);

        Rol rolUser = rolRepository.findByNombre("USER")
                .orElseGet(() -> rolRepository.save(
                        Rol.builder().nombre("USER").descripcion("Usuario estandar").build()));

        Usuario usuario = Usuario.builder()
                .persona(persona)
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .activo(true)
                .roles(Set.of(rolUser))
                .build();
        usuario = usuarioRepository.save(usuario);

        String nombreCompleto = persona.getNombre() + " " + persona.getApellido();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SesionResponse.builder()
                        .username(usuario.getUsername())
                        .nombre(nombreCompleto)
                        .rol("USER")
                        .build());
    }

    @PostMapping("/login")
    public ResponseEntity<SesionResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String rol = userDetails.getAuthorities().iterator().next().getAuthority();

        String accessToken = jwtService.generateToken(userDetails.getUsername(), rol);
        String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername(), rol);

        // Buscar nombre de la persona
        String nombre = usuarioRepository.findByUsername(userDetails.getUsername())
                .map(u -> u.getPersona().getNombre() + " " + u.getPersona().getApellido())
                .orElse(userDetails.getUsername());

        return ResponseEntity.ok(SesionResponse.builder()
                .username(userDetails.getUsername())
                .nombre(nombre)
                .rol(rol)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String jti = jwtService.extractJti(token);
                if (jti != null) {
                    // Calcular tiempo restante de expiracion
                    long tiempoRestante = jwtService.getExpirationMs() - 1000;
                    blacklistService.revocar(jti, tiempoRestante);
                }
            } catch (Exception e) {
                // Token ya invalido, ignorar
            }
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtService.extractUsername(refreshToken);
        String rol = jwtService.extractRol(refreshToken);

        String newAccessToken = jwtService.generateToken(username, rol);

        return ResponseEntity.ok(java.util.Map.of(
                "accessToken", newAccessToken,
                "tokenType", "Bearer"
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<SesionResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String rol = userDetails.getAuthorities().iterator().next().getAuthority();

        String nombre = usuarioRepository.findByUsername(userDetails.getUsername())
                .map(u -> u.getPersona().getNombre() + " " + u.getPersona().getApellido())
                .orElse(userDetails.getUsername());

        return ResponseEntity.ok(SesionResponse.builder()
                .username(userDetails.getUsername())
                .nombre(nombre)
                .rol(rol)
                .build());
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
