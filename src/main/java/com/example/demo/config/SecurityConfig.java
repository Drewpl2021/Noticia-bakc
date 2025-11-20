package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // 🔹 ACTIVAR CORS dentro de Spring Security
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Login público
                        .requestMatchers("/api/auth/**").permitAll()
                        // Webhooks de pasarela (Stripe, Culqi, etc.) → deben ser públicos
                        .requestMatchers("/api/webhook/**").permitAll()
                        // Artículos públicos (ajusta si quieres)
                        .requestMatchers("/api/articulos/**").permitAll()
                        .requestMatchers("/api/productos/**").permitAll()

                        // 🔐 APIs restringidas (requieren estar logueado con JWT)
                        .requestMatchers("/api/roles/**").authenticated()
                        .requestMatchers("/api/usuarios/**").authenticated()
                        .requestMatchers("/api/scrappers/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()        // checkout, etc.
                        .requestMatchers("/api/membresias/**").authenticated()  // ver/gestionar membresías

                        // cualquier otra
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🔹 Configuración global de CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origen permitido: tu app Angular
        config.setAllowedOrigins(List.of("http://localhost:4200"));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Headers permitidos (incluye Authorization para el Bearer token)
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Si necesitas enviar credenciales (cookies, aunque con JWT no es obligatorio)
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica la config a todas las rutas
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    // AuthenticationManager para usar en AuthController
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Encoder para las contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
