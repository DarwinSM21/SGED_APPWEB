package org.uteq.backend.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Filtro de autenticación JWT.
 * Desde la Entrega 3 el token viaja en una cookie HttpOnly+Secure+SameSite=Strict
 * (Bloque A.1). Se conserva el header Authorization: Bearer como mecanismo
 * secundario para clientes de API (Postman, k6).
 * Los tokens revocados (logout) se rechazan consultando la blacklist Redis por jti.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ACCESS_COOKIE = "sged_access";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RedisBlacklistService blacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = resolverToken(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(jwt);

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtService.isTokenValid(jwt)
                    && !blacklistService.estaRevocado(jwtService.extractJti(jwt))) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ignored) {
            // Token malformado o firma inválida: continúa sin autenticación
            // y el entry point responderá 401 ProblemDetails.
        }

        filterChain.doFilter(request, response);
    }

    /** Prioridad 1: cookie HttpOnly. Prioridad 2: header Authorization Bearer. */
    private String resolverToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            String deCookie = Arrays.stream(request.getCookies())
                    .filter(c -> ACCESS_COOKIE.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
            if (deCookie != null && !deCookie.isBlank()) {
                return deCookie;
            }
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
