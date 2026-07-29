package org.uteq.backend.seguridad.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT híbrido.
 * 1. Intenta extraer el token del encabezado 'Authorization: Bearer <token>' (React / Angular / Mobile).
 * 2. Si no existe, busca en la cookie HttpOnly 'sged_access'.
 * 3. Valida la firma y expiración con JwtService.
 * 4. Consulta Redis blacklist para verificar que el JTI no esté revocado.
 * 5. Establece el UsernamePasswordAuthenticationToken en SecurityContextHolder.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_COOKIE = "sged_access";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final RedisBlacklistService blacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Extrae el token (Primero prueba por Header Bearer, luego por Cookie)
        String token = extractAccessToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String jti = jwtService.extractJti(token);

                if (jti != null && blacklistService.estaRevocado(jti)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token inválido o expirado: deja que Spring Security maneje el 401
            logger.debug("Error procesando autenticación JWT: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Busca el token JWT de dos formas:
     * 1. Header 'Authorization: Bearer ...' (Prioridad Alta)
     * 2. Cookie 'sged_access' (Prioridad Secundaria)
     */
    private String extractAccessToken(HttpServletRequest request) {
        // 1. Intentar obtener desde el Header Authorization (Bearer)
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String bearerToken = authHeader.substring(BEARER_PREFIX.length()).trim();
            if (!bearerToken.isEmpty()) {
                return bearerToken;
            }
        }

        // 2. Si no hay Header, intentar obtener desde las Cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}