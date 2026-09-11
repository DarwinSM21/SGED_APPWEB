package org.uteq.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.uteq.backend.common.exception.ProblemDetailsAuthHandlers;
import org.uteq.backend.seguridad.auth.security.JwtAuthenticationFilter;

import java.util.List;

/**
 * Cadena de filtros de seguridad de la API: sesiones sin estado (JWT),
 * autorización por ruta y por rol (RNF-06), cabeceras de seguridad (RNF-05)
 * y CORS restringido a orígenes exactos (punto 16 de la Entrega Final).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("#{'${cors.allowed-origin-patterns:http://localhost:4200,http://localhost:80,http://127.0.0.1:4200,http://localhost:5173}'.split(',')}")
    private List<String> corsAllowedOriginPatterns;
    private final ProblemDetailsAuthHandlers problemHandlers;

    /**
     * @param http constructor de configuración HTTP inyectado por Spring Security
     * @return la cadena de filtros configurada: CORS, sin CSRF (API sin estado),
     *         sin sesión de servidor, rutas públicas explícitas y el resto autenticado
     * @throws Exception si Spring Security no puede construir la cadena de filtros
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/docs/**", "/api/docs.json", "/api/swagger-ui/**",
                                "/api/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(problemHandlers.problemAuthEntryPoint())
                        .accessDeniedHandler(problemHandlers.problemAccessDeniedHandler())
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * @return la configuración CORS: orígenes exactos (sin comodín de dominio),
     *         métodos y cabeceras explícitos, y credenciales habilitadas
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(corsAllowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * @return el codificador de contraseñas BCrypt con factor de coste 12 (RNF-03)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * @param config configuración de autenticación de Spring Security
     * @return el gestor de autenticación por defecto, usado por el flujo de login
     * @throws Exception si Spring Security no puede resolver el gestor de autenticación
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
