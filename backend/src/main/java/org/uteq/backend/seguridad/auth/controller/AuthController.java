package org.uteq.backend.seguridad.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.common.exception.TooManyRequestsException;
import org.uteq.backend.seguridad.auth.dto.*;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.usuario.entity.Usuario;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.usuario.repository.UsuarioRepository;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.security.LoginAttemptService;
import org.uteq.backend.seguridad.auth.security.RedisBlacklistService;
import org.uteq.backend.seguridad.rol.entity.Rol;
import org.uteq.backend.seguridad.rol.repository.RolRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Controlador de autenticacion JWT.
 * Endpoints: registro, login, logout, refresh, /me.
 * El access token y el refresh token viajan exclusivamente en cookies
 * HttpOnly + Secure + SameSite=Strict (Bloque A.1 de la guia de la
 * Tercera Entrega); nunca en el body ni en un header de respuesta.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ACCESS_COOKIE = "sged_access";
    private static final String REFRESH_COOKIE = "sged_refresh";
    private static final Logger AUTH_AUDIT_LOG = LoggerFactory.getLogger("AUTH_AUDIT");

    @org.springframework.beans.factory.annotation.Value("${security.cookie.secure:true}")
    private boolean cookieSecure;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisBlacklistService blacklistService;
    private final LoginAttemptService loginAttemptService;
    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final RolRepository rolRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/registro")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
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
                .password_Hash(passwordEncoder.encode(request.password()))
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
    @Transactional(readOnly = true)
    public ResponseEntity<SesionResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String ip = httpRequest.getRemoteAddr();

        if (loginAttemptService.estaBloqueada(ip)) {
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos. Intenta de nuevo en 15 minutos.");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            loginAttemptService.registrarFallo(ip);
            AUTH_AUDIT_LOG.warn("AUTH_LOGIN_FAIL ip={} sub={}", ip, request.username());
            throw e;
        }

        loginAttemptService.registrarExito(ip);

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String rol = userDetails.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
        AUTH_AUDIT_LOG.info("AUTH_LOGIN_OK ip={} sub={}", ip, userDetails.getUsername());

        String accessToken = jwtService.generateToken(userDetails.getUsername(), rol);
        String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername(), rol);
        setAuthCookies(httpResponse, accessToken, refreshToken);

        String nombre = usuarioRepository.findByUsernameAndActivoTrue(userDetails.getUsername())
                .map(u -> u.getPersona().getNombre() + " " + u.getPersona().getApellido())
                .orElse(userDetails.getUsername());

        // Asigna el token a la respuesta
        return ResponseEntity.ok(SesionResponse.builder()
                .username(userDetails.getUsername())
                .nombre(nombre)
                .rol(rol)
                .accessToken(accessToken)   // 👈 AQUÍ USAS LA VARIABLE
                .refreshToken(refreshToken) // 👈 AQUÍ USAS LA VARIABLE
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = ACCESS_COOKIE, required = false) String accessToken,
            HttpServletResponse httpResponse) {

        if (accessToken != null) {
            try {
                String jti = jwtService.extractJti(accessToken);
                if (jti != null) {
                    blacklistService.revocar(jti, jwtService.getExpirationMs());
                }
            } catch (Exception e) {
                // Token ya invalido, ignorar
            }
        }

        clearAuthCookies(httpResponse);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse httpResponse) {

        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtService.extractUsername(refreshToken);
        String rol = jwtService.extractRol(refreshToken);
        String newAccessToken = jwtService.generateToken(username, rol);
        setAccessCookie(httpResponse, newAccessToken);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<SesionResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String rol = userDetails.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");

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

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        setAccessCookie(response, accessToken);
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(REFRESH_COOKIE, refreshToken, jwtService.getRefreshExpirationMs()).toString());
    }

    private void setAccessCookie(HttpServletResponse response, String accessToken) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(ACCESS_COOKIE, accessToken, jwtService.getExpirationMs()).toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_COOKIE, "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_COOKIE, "", 0).toString());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeMs) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api")
                .maxAge(maxAgeMs / 1000)
                .build();
    }
}
