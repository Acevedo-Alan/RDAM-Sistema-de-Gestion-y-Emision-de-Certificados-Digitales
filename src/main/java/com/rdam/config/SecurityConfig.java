package com.rdam.config;

import com.rdam.security.filter.RateLimitFilter;
import com.rdam.security.jwt.JwtAuthenticationFilter;
import com.rdam.security.provider.CiudadanoAuthProvider;
import com.rdam.security.provider.EmpleadoAuthProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Configuracion central de seguridad.
 * - Sesiones STATELESS (JWT)
 * - Dos AuthenticationProviders: ciudadano y empleado
 * - Filtro JWT antes de UsernamePasswordAuthenticationFilter
 * - Rate limiting en /auth/login
 * - @PreAuthorize habilitado via @EnableMethodSecurity
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    // 1. Borramos los AuthProviders de las variables y del constructor
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/pagos/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalogos/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 3. Y aquí inyectamos los Providers directo en el método, ¡rompiendo el bucle!
    @Bean
    public AuthenticationManager authenticationManager(
            CiudadanoAuthProvider ciudadanoAuthProvider,
            EmpleadoAuthProvider empleadoAuthProvider) {
        return new ProviderManager(List.of(ciudadanoAuthProvider, empleadoAuthProvider));
    }
}
