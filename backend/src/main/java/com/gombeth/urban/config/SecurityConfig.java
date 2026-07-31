package com.gombeth.urban.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CookieCsrfTokenRepository csrfTokenRepository,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                csrfTokenRepository
                        )
                        .csrfTokenRequestHandler(
                                new SpaCsrfTokenRequestHandler()
                        )
                )

                .cors(cors -> cors
                        .configurationSource(
                                corsConfigurationSource
                        )
                )

                .securityContext(context -> context
                        .securityContextRepository(
                                securityContextRepository
                        )
                        .requireExplicitSave(true)
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                        .sessionFixation(fixation ->
                                fixation.changeSessionId()
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR,
                                DispatcherType.FORWARD
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
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
                                "/*.map",
                                "/*.ico",
                                "/*.png",
                                "/*.svg",
                                "/*.woff",
                                "/*.woff2",
                                "/api/health",
                                "/api/auth/login",
                                "/api/auth/csrf",
                                "/api/auth/cambiar-password"
                        )
                        .permitAll()

                        /*
                         * Todas las APIs de negocio exigen
                         * una sesión autenticada.
                         */
                        .requestMatchers("/api/**")
                        .authenticated()

                        /*
                         * Las rutas Angular se sirven desde
                         * index.html. Los datos quedan protegidos
                         * dentro del backend.
                         */
                        .anyRequest()
                        .permitAll()
                )

                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, exception) -> {
                                    response.setStatus(
                                            HttpStatus
                                                    .UNAUTHORIZED
                                                    .value()
                                    );

                                    response.setCharacterEncoding(
                                            StandardCharsets
                                                    .UTF_8
                                                    .name()
                                    );

                                    response.setContentType(
                                            "application/json"
                                    );

                                    response.getWriter().write(
                                            "{\"mensaje\":"
                                                    + "\"Sesión no iniciada "
                                                    + "o caducada.\"}"
                                    );
                                }
                        )

                        .accessDeniedHandler(
                                (request, response, exception) -> {
                                    response.setStatus(
                                            HttpStatus
                                                    .FORBIDDEN
                                                    .value()
                                    );

                                    response.setCharacterEncoding(
                                            StandardCharsets
                                                    .UTF_8
                                                    .name()
                                    );

                                    response.setContentType(
                                            "application/json"
                                    );

                                    response.getWriter().write(
                                            "{\"mensaje\":"
                                                    + "\"No tiene permiso "
                                                    + "para realizar esta "
                                                    + "operación.\"}"
                                    );
                                }
                        )
                )

                .logout(logout -> logout
                        .logoutUrl(
                                "/api/auth/logout"
                        )
                        .invalidateHttpSession(
                                true
                        )
                        .clearAuthentication(
                                true
                        )
                        .deleteCookies(
                                "JSESSIONID"
                        )
                        .logoutSuccessHandler(
                                (
                                        request,
                                        response,
                                        authentication
                                ) ->
                                        response.setStatus(
                                                HttpStatus
                                                        .NO_CONTENT
                                                        .value()
                                        )
                        )
                        .permitAll()
                )

                .formLogin(
                        form -> form.disable()
                )
                .httpBasic(
                        basic -> basic.disable()
                )
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration
                .getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository
    securityContextRepository() {

        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    public SessionAuthenticationStrategy
    sessionAuthenticationStrategy() {

        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public CookieCsrfTokenRepository
    csrfTokenRepository() {

        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository
                        .withHttpOnlyFalse();

        repository.setCookiePath(
                "/"
        );

        return repository;
    }

    /**
     * Obtiene los orígenes autorizados desde:
     *
     * app.cors.allowed-origins
     *
     * Se pueden indicar varios orígenes separados
     * mediante comas.
     */
    @Bean
    public CorsConfigurationSource
    corsConfigurationSource(
            Environment environment
    ) {
        String allowedOrigins =
                environment.getRequiredProperty(
                        "app.cors.allowed-origins"
                );

        List<String> origins =
                Arrays.stream(
                                allowedOrigins.split(",")
                        )
                        .map(String::trim)
                        .filter(origin ->
                                !origin.isBlank()
                        )
                        .toList();

        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "La propiedad "
                            + "app.cors.allowed-origins "
                            + "no contiene ningún origen válido."
            );
        }

        CorsConfiguration config =
                new CorsConfiguration();

        config.setAllowedOrigins(
                origins
        );

        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                List.of(
                        "Accept",
                        "Content-Type",
                        "X-XSRF-TOKEN",
                        "X-Requested-With"
                )
        );

        config.setAllowCredentials(
                true
        );

        config.setMaxAge(
                3600L
        );

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