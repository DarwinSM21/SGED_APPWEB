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
 * Configuracion de Spring Security 6.
 * - CORS para frontend Angular (localhost:4200)
 * - BCrypt costo 12
 * - Stateless (sin sesiones HTTP)
 * - @PreAuthorize habilitado para control por rol
 * - Filtro JWT en la cadena de filtros
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Patrones (no origenes exactos: admiten "*", p.ej. para probar desde un
     * celular en la misma red LAN sin fijar una IP en el codigo fuente).
     * Cada quien agrega la suya en su propio .env, nunca en este archivo.
     */
    @Value("#{'${cors.allowed-origin-patterns:http://localhost:4200,http://localhost:80,http://127.0.0.1:4200,http://localhost:5173}'.split(',')}")
    private List<String> corsAllowedOriginPatterns;
    private final ProblemDetailsAuthHandlers problemHandlers;

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // setAllowedOriginPatterns (no setAllowedOrigins): admite "*" en un
        // segmento, necesario para permitir la LAN entera sin fijar una IP.
        // Sigue siendo compatible con allowCredentials=true porque Spring
        // igual refleja el origen puntual que hizo match, nunca "*" literal.
        config.setAllowedOriginPatterns(corsAllowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
