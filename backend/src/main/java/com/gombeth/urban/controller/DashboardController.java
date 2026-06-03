package com.gombeth.urban.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    public DashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Long> dashboard(@RequestParam Long usuarioId) {

        Long totalComunidades = jdbcTemplate.queryForObject(
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

        Long totalPropietarios = jdbcTemplate.queryForObject(
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

        Long totalIncidencias = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM cola_incidencias ci
                WHERE ci.comunidad_id IN (
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
                "totalComunidades", totalComunidades != null ? totalComunidades : 0L,
                "totalPropietarios", totalPropietarios != null ? totalPropietarios : 0L,
                "totalIncidencias", totalIncidencias != null ? totalIncidencias : 0L
        );
    }
}