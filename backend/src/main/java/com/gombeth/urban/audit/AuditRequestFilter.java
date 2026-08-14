package com.gombeth.urban.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuditRequestFilter
        extends OncePerRequestFilter {

    private static final int REQUEST_CACHE_LIMIT =
            128 * 1024;

    private final AuditLogService auditLogService;

    public AuditRequestFilter(
            AuditLogService auditLogService
    ) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String uri =
                request.getRequestURI();

        return uri == null
                || !uri.startsWith(
                        "/api/"
                );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(
                        request,
                        REQUEST_CACHE_LIMIT
                );

        Authentication authenticationAntes =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        long inicio =
                System.nanoTime();

        Throwable error = null;

        try {
            filterChain.doFilter(
                    wrappedRequest,
                    response
            );

        } catch (
                ServletException
                | IOException
                | RuntimeException exception
        ) {
            error = exception;
            throw exception;

        } catch (Error exception) {
            error = exception;
            throw exception;

        } finally {

            long duracionMs =
                    TimeUnit.NANOSECONDS
                            .toMillis(
                                    System.nanoTime()
                                            - inicio
                            );

            Authentication authenticationDespues =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication();

            auditLogService.registrarPeticion(
                    wrappedRequest,
                    response.getStatus(),
                    duracionMs,
                    authenticationAntes,
                    authenticationDespues,
                    response.getContentType(),
                    response.getHeader(
                            "Content-Length"
                    ),
                    error
            );
        }
    }
}
