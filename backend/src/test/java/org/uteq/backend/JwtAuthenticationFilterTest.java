package org.uteq.backend;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.uteq.backend.seguridad.auth.security.JwtAuthenticationFilter;
import org.uteq.backend.seguridad.auth.security.JwtService;
import org.uteq.backend.seguridad.auth.security.RedisBlacklistService;
import org.uteq.backend.seguridad.auth.security.UserDetailsServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock private JwtService jwtService;
    @Mock private UserDetailsServiceImpl userDetailsService;
    @Mock private RedisBlacklistService blacklistService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, blacklistService);
        lenient().when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails userDetails(String username, String rol) {
        return User.builder()
                .username(username)
                .password("hash")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + rol)))
                .build();
    }

    @Test
    @DisplayName("sin header ni cookie, continua la cadena sin autenticar")
    void sinTokenContinuaSinAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    @DisplayName("token por header Bearer valido autentica")
    void tokenPorHeaderBearerAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.extractUsername("token-valido")).thenReturn("coach@sged.test");
        when(userDetailsService.loadUserByUsername("coach@sged.test"))
                .thenReturn(userDetails("coach@sged.test", "ENTRENADOR"));
        when(jwtService.isTokenValid("token-valido")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertNotNull(auth.getDetails(), "buildDetails debe poblar los detalles de la peticion");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("sin header, token por cookie sged_access valido autentica")
    void tokenPorCookieAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("sged_access", "token-cookie")});
        when(jwtService.extractUsername("token-cookie")).thenReturn("admin@sged.test");
        when(userDetailsService.loadUserByUsername("admin@sged.test"))
                .thenReturn(userDetails("admin@sged.test", "ADMINISTRADOR"));
        when(jwtService.isTokenValid("token-cookie")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("header sin prefijo Bearer cae a la cookie")
    void headerSinBearerCaeACookie() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic xyz");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("sged_access", "token-cookie")});
        when(jwtService.extractUsername("token-cookie")).thenReturn("admin@sged.test");
        when(userDetailsService.loadUserByUsername("admin@sged.test"))
                .thenReturn(userDetails("admin@sged.test", "ADMINISTRADOR"));
        when(jwtService.isTokenValid("token-cookie")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("header Bearer vacio cae a la cookie")
    void headerBearerVacioCaeACookie() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("sged_access", "token-cookie")});
        when(jwtService.extractUsername("token-cookie")).thenReturn("admin@sged.test");
        when(userDetailsService.loadUserByUsername("admin@sged.test"))
                .thenReturn(userDetails("admin@sged.test", "ADMINISTRADOR"));
        when(jwtService.isTokenValid("token-cookie")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("cookies presentes pero ninguna es sged_access: no autentica")
    void cookiesSinCoincidenciaNoAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("otra_cookie", "valor")});

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    @DisplayName("username nulo (token no reconocido) no autentica")
    void usernameNuloNoAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-raro");
        when(jwtService.extractUsername("token-raro")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("si el contexto ya tiene autenticacion, no la vuelve a resolver")
    void contextoYaAutenticadoNoLoSobreescribe() throws Exception {
        var existente = new UsernamePasswordAuthenticationToken("ya.autenticado", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existente);

        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.extractUsername("token-valido")).thenReturn("otro@sged.test");

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("jti en la lista negra: no autentica, pero continua la cadena")
    void jtiRevocadoNoAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-revocado");
        when(jwtService.extractUsername("token-revocado")).thenReturn("coach@sged.test");
        when(jwtService.extractJti("token-revocado")).thenReturn("jti-revocado");
        when(blacklistService.estaRevocado("jti-revocado")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("token con firma valida pero expirado (isTokenValid=false) no autentica")
    void tokenInvalidoNoAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-expirado");
        when(jwtService.extractUsername("token-expirado")).thenReturn("coach@sged.test");
        when(userDetailsService.loadUserByUsername("coach@sged.test"))
                .thenReturn(userDetails("coach@sged.test", "ENTRENADOR"));
        when(jwtService.isTokenValid("token-expirado")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("una excepcion procesando el token no rompe el filtro: la cadena continua")
    void excepcionAlProcesarNoRompeElFiltro() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-corrupto");
        when(jwtService.extractUsername("token-corrupto")).thenThrow(new RuntimeException("firma invalida"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
