package org.uteq.backend.seguridad.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.seguridad.auth.dto.*;
import org.uteq.backend.seguridad.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String ACCESS_COOKIE = "sged_access";
    private static final String REFRESH_COOKIE = "sged_refresh";

    @org.springframework.beans.factory.annotation.Value("${security.cookie.secure:true}")
    private boolean cookieSecure;

    private final AuthService authService;
    private final org.uteq.backend.seguridad.auth.security.JwtService jwtService;

    @PostMapping("/registro")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SesionResponse> registro(@Valid @RequestBody RegisterRequest request) {
        return authService.registrar(request)
                .map(sesion -> ResponseEntity.status(HttpStatus.CREATED).body(sesion))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    @PostMapping("/login")
    public ResponseEntity<SesionResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthService.LoginResult resultado = authService.login(request, httpRequest.getRemoteAddr());

        setAuthCookies(httpResponse, resultado.accessToken(), resultado.refreshToken());
        return ResponseEntity.ok(resultado.sesion());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = ACCESS_COOKIE, required = false) String accessToken,
            HttpServletResponse httpResponse) {
        authService.logout(accessToken);
        clearAuthCookies(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        return authService.refrescar(refreshToken)
                .map(nuevoAccessToken -> {
                    setAccessCookie(httpResponse, nuevoAccessToken);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/me")
    public ResponseEntity<SesionResponse> me() {
        return authService.obtenerSesionActual()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
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
