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

/**
 * Controlador de autenticación JWT: {@code /registro}, {@code /login},
 * {@code /logout}, {@code /refresh}, {@code /me} y {@code /ping}.
 *
 * <p>El token de acceso y el de refresco viajan <em>exclusivamente</em> en
 * cookies {@code HttpOnly + Secure + SameSite=Strict} (ADR-002, ADR-008);
 * nunca en el cuerpo ni en un encabezado de respuesta, para que ningún
 * {@code fetch} del navegador pueda leerlos.
 *
 * <p>La lógica de negocio vive en {@link AuthService} (D-03 del informe de
 * evaluación de calidad): este controlador solo traduce HTTP a llamadas de
 * dominio y arma las cookies de sesión.
 */
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

    /**
     * Da de alta una persona y su cuenta de usuario. Reservado a
     * {@code ADMINISTRADOR}.
     *
     * @param request datos de la persona y de la cuenta; validado con
     *                {@code @Valid}
     * @return {@code 201 Created} con la sesión del usuario creado, o
     *         {@code 409 Conflict} si el {@code username}, la cédula o el
     *         correo ya existen
     */
    @PostMapping("/registro")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SesionResponse> registro(@Valid @RequestBody RegisterRequest request) {
        return authService.registrar(request)
                .map(sesion -> ResponseEntity.status(HttpStatus.CREATED).body(sesion))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    /**
     * Autentica al usuario y deja los tokens en cookies {@code HttpOnly}.
     *
     * @param request      credenciales; validado con {@code @Valid}
     * @param httpRequest  petición entrante, de la que se toma la IP para el
     *                     límite de intentos
     * @param httpResponse respuesta a la que se añaden las cookies de sesión
     * @return {@code 200 OK} con los datos de sesión (sin el token)
     * @throws org.uteq.backend.common.exception.TooManyRequestsException si la
     *         IP está bloqueada por intentos fallidos
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         si las credenciales son incorrectas
     */
    @PostMapping("/login")
    public ResponseEntity<SesionResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthService.LoginResult resultado = authService.login(request, httpRequest.getRemoteAddr());

        // El token viaja solo en la cookie HttpOnly de setAuthCookies; no se
        // repite en el cuerpo, que sería legible por cualquier fetch del
        // frontend y anularía la protección que declaran ADR-002 y ADR-008.
        setAuthCookies(httpResponse, resultado.accessToken(), resultado.refreshToken());
        return ResponseEntity.ok(resultado.sesion());
    }

    /**
     * Cierra la sesión: revoca el token por su {@code jti} y borra las
     * cookies.
     *
     * @param accessToken  token de acceso tomado de la cookie; puede faltar
     * @param httpResponse respuesta a la que se añaden las cookies vacías
     * @return {@code 204 No Content}
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = ACCESS_COOKIE, required = false) String accessToken,
            HttpServletResponse httpResponse) {
        authService.logout(accessToken);
        clearAuthCookies(httpResponse);
        return ResponseEntity.noContent().build();
    }

    /**
     * Emite un nuevo token de acceso a partir del refresh token de la cookie.
     *
     * @param refreshToken refresh token tomado de la cookie; puede faltar
     * @param httpResponse respuesta a la que se añade la cookie de acceso
     *                     renovada
     * @return {@code 204 No Content} con la cookie renovada, o {@code 401} si
     *         el refresh token falta o no es válido
     */
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

    /**
     * Devuelve los datos de la sesión activa.
     *
     * @return {@code 200 OK} con la sesión, o {@code 401} si no hay sesión
     *         autenticada
     */
    @GetMapping("/me")
    public ResponseEntity<SesionResponse> me() {
        return authService.obtenerSesionActual()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Sonda de disponibilidad, sin autenticación.
     *
     * @return {@code 200 OK} con el cuerpo {@code "pong"}
     */
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
