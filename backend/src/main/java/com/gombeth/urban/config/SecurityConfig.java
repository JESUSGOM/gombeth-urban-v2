package com.gombeth.urban.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/api/auth/login",
                                "/api/comunidades",
                                "/api/comunidades/**",
                                "/api/dashboard",
                                "/api/dashboard/**",
                                "/api/vecinos",
                                "/api/vecinos/**",
                                "/api/documentos",
                                "/api/documentos/**",
                                "/api/propiedades",
                                "/api/propiedades/**",
                                "/api/presupuestos",
                                "/api/presupuestos/**",
                                "/api/conceptos-cobro",
                                "/api/conceptos-cobro/**",
                                "/api/cuentas-contables",
                                "/api/cuentas-contables/**",
                                "/api/recibos",
                                "/api/recibos/**",
                                "/api/remesas",
                                "/api/remesas/**",
                                "/api/movimientos",
                                "/api/movimientos/**",
                                "/api/norma43/",
                                "/api/norma43/**",
                                "/api/contabilidad",
                                "/api/contabilidad/**",
                                "/api/gastos",
                                "/api/gastos/**",
                                "/api/cuentas-contables",
                                "/api/cuentas-contables/**"
                        )
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}