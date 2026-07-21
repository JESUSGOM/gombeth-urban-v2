package com.gombeth.urban.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Adaptador CSRF para una aplicación Angular de una sola página.
 *
 * Angular lee el valor sin codificar de la cookie XSRF-TOKEN y lo envía
 * en la cabecera X-XSRF-TOKEN. Spring Security, por defecto, utiliza una
 * representación XOR para proteger frente a BREACH. Este adaptador admite
 * ambas representaciones y fuerza la creación de la cookie cuando sea
 * necesario.
 */
public final class SpaCsrfTokenRequestHandler
        implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain =
            new CsrfTokenRequestAttributeHandler();

    private final CsrfTokenRequestHandler xor =
            new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> csrfToken
    ) {
        xor.handle(request, response, csrfToken);

        /*
         * Fuerza la carga del token diferido para que Spring escriba
         * la cookie XSRF-TOKEN cuando todavía no existe.
         */
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        String headerValue = request.getHeader(
                csrfToken.getHeaderName()
        );

        if (StringUtils.hasText(headerValue)) {
            return plain.resolveCsrfTokenValue(
                    request,
                    csrfToken
            );
        }

        return xor.resolveCsrfTokenValue(
                request,
                csrfToken
        );
    }
}