package org.uteq.backend.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        ))

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**",
                                "/api/test/public"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/api/admin/**"
                        )
                        .hasRole("ADMINISTRADOR")

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    // Esto se hizo para la comunicacion con el frontend angular
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // Configuración ESPECÍFICA para login (solo POST)
        CorsConfiguration loginConfig = new CorsConfiguration();
        loginConfig.setAllowedOrigins(List.of("http://localhost:4200"));
        loginConfig.setAllowedMethods(List.of("POST", "OPTIONS"));  // Solo POST para login
        loginConfig.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        loginConfig.setAllowCredentials(true);
        loginConfig.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/auth/login", loginConfig);
        
        // Configuración GENERAL para el resto de endpoints
        CorsConfiguration otherConfig = new CorsConfiguration();
        otherConfig.setAllowedOrigins(List.of("http://localhost:4200"));
        otherConfig.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        otherConfig.setAllowedHeaders(List.of(
            "Content-Type", 
            "Authorization", 
            "Accept"
        ));
        otherConfig.setAllowCredentials(true);
        otherConfig.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/**", otherConfig);
        
        return source;
     }
}