package com.gombeth.urban.controller;

import com.gombeth.urban.service.AccesoComunidadService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    private final AccesoComunidadService
            accesoComunidadService;

    public DashboardController(
            JdbcTemplate jdbcTemplate,
            AccesoComunidadService accesoComunidadService
    ) {
        this.jdbcTemplate = jdbcTemplate;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Devuelve el resumen correspondiente exclusivamente
     * al usuario de la sesión autenticada.
     *
     * Cualquier usuarioId enviado como parámetro en la URL
     * es ignorado por el backend.
     */
    @GetMapping
    public Map<String, Long> dashboard(
            Authentication authentication
    ) {
        Long usuarioId =
                accesoComunidadService.obtenerUsuarioId(
                        authentication
                );

        Long totalComunidades =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM comunidades c
                        WHERE c.usuario_id = ?
                           OR c.id IN (
                                SELECT uc.comunidad_id
                                FROM usuario_comunidades uc
                                WHERE uc.usuario_id = ?
                           )
                        """,
                        Long.class,
                        usuarioId,
                        usuarioId
                );

        Long totalPropietarios =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM vecinos v
                        WHERE v.comunidad_id IN (
                            SELECT c.id
                            FROM comunidades c
                            WHERE c.usuario_id = ?
                               OR c.id IN (
                                    SELECT uc.comunidad_id
                                    FROM usuario_comunidades uc
                                    WHERE uc.usuario_id = ?
                               )
                        )
                        """,
                        Long.class,
                        usuarioId,
                        usuarioId
                );

        Long totalIncidencias =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM gestion_incidencias gi
                        WHERE gi.comunidad_id IN (
                            SELECT c.id
                            FROM comunidades c
                            WHERE c.usuario_id = ?
                               OR c.id IN (
                                    SELECT uc.comunidad_id
                                    FROM usuario_comunidades uc
                                    WHERE uc.usuario_id = ?
                               )
                        )
                        """,
                        Long.class,
                        usuarioId,
                        usuarioId
                );

        return Map.of(
                "totalComunidades",
                valorSeguro(totalComunidades),

                "totalPropietarios",
                valorSeguro(totalPropietarios),

                "totalIncidencias",
                valorSeguro(totalIncidencias)
        );
    }

    private Long valorSeguro(
            Long valor
    ) {
        return valor != null
                ? valor
                : 0L;
    }
}