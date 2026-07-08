package org.uteq.backend.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.auth.dto.LoginRequest;
import org.uteq.backend.auth.dto.LoginResponse;
import org.uteq.backend.auth.dto.RegistroRequest;
import org.uteq.backend.auth.dto.SesionResponse;
import org.uteq.backend.auth.security.JwtAuthenticationFilter;
import org.uteq.backend.auth.security.LoginAttemptService;
import org.uteq.backend.auth.service.AuthService;
import org.uteq.backend.common.exception.CredencialesInvalidasException;
import org.uteq.backend.common.exception.TooManyRequestsException;

import java.util.Arrays;

/**
 * Autenticación stateless. Desde la Entrega 3 (Bloque A.1) el JWT de acceso
 * y el de refresco viajan en cookies HttpOnly + Secure + SameSite=Strict,
 * nunca en el body. OWASP A07: rate limiting por IP con respuesta 429.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String REFRESH_COOKIE = "sged_refresh";

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

    @Value("${security.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @GetMapping("/ping")
    public String ping() {
        return "AUTH OK";
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequest request) {
        return ResponseEntity.status(201).body(authService.registro(request).getUsername());
    }

    @PostMapping("/login")
    public ResponseEntity<SesionResponse> login(@RequestBody LoginRequest request,
                                                HttpServletRequest http) {
        String ip = obtenerIp(http);

        if (loginAttemptService.estaBloqueada(ip)) {
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos. Intente nuevamente más tarde.");
        }

        LoginResponse sesion;
        try {
            sesion = authService.login(request, ip);
        } catch (CredencialesInvalidasException e) {
            loginAttemptService.registrarFallo(ip);
            throw e;
        }
        loginAttemptService.registrarExito(ip);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookie(JwtAuthenticationFilter.ACCESS_COOKIE,
                                sesion.token(), expirationMs / 1000).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookie(REFRESH_COOKIE,
                                sesion.refreshToken(), refreshExpirationMs / 1000).toString())
                .body(SesionResponse.builder()
                        .username(sesion.username())
                        .nombre(sesion.nombre())
                        .rol(sesion.rol())
                        .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest http) {
        authService.logout(leerCookie(http, JwtAuthenticationFilter.ACCESS_COOKIE),
                obtenerIp(http));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookie(JwtAuthenticationFilter.ACCESS_COOKIE, "", 0).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookie(REFRESH_COOKIE, "", 0).toString())
                .body("Sesión cerrada correctamente");
    }

    @PostMapping("/refresh")
    public ResponseEntity<SesionResponse> refresh(HttpServletRequest http) {
        String refreshToken = leerCookie(http, REFRESH_COOKIE);
        LoginResponse sesion = authService.refresh(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookie(JwtAuthenticationFilter.ACCESS_COOKIE,
                                sesion.token(), expirationMs / 1000).toString())
                .body(SesionResponse.builder()
                        .username(sesion.username())
                        .nombre(sesion.nombre())
                        .rol(sesion.rol())
                        .build());
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        return ResponseEntity.ok(authentication.getName());
    }

    /** Cookie HttpOnly + Secure + SameSite=Strict (Bloque A.1). */
    private ResponseCookie cookie(String nombre, String valor, long maxAgeSegundos) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSegundos)
                .build();
    }

    private String leerCookie(HttpServletRequest request, String nombre) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> nombre.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String obtenerIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank())
                ? xff.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
