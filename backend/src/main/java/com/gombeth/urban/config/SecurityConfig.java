package com.gombeth.urban.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        )
                        .permitAll()

                        .requestMatchers(
                                "/",
                                "/error",
                                "/index.html",
                                "/favicon.ico",
                                "/images/**",
                                "/*.js",
                                "/*.css",
                                "/api/health",
                                "/api/auth/login",
                                "/api/usuario/**",
                                "/api/comunidades/**",
                                "/api/dashboard/**",
                                "/api/vecinos/**",
                                "/api/documentos/**",
                                "/api/vecino-documentos/**",
                                "/api/propiedades/**",
                                "/api/presupuestos/**",
                                "/api/conceptos-cobro/**",
                                "/api/cuentas-contables/**",
                                "/api/recibos/**",
                                "/api/remesas/**",
                                "/api/movimientos/**",
                                "/api/norma43/**",
                                "/api/contabilidad/**",
                                "/api/gastos/**",
                                "/api/diario/**",
                                "/api/mayor/**",
                                "/api/balance/",
                                "/api/balance/**",
                                "/api/incidencias/",
                                "/api/incidencias/**"
                        )
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        config.setAllowedOrigins(
                List.of(
                        "http://localhost:4200"
                )
        );

        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                List.of("*")
        );

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config
        );

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}