package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.repository.VecinoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vecinos")
public class VecinoController {

    private final VecinoRepository vecinoRepository;
    private final JdbcTemplate jdbcTemplate;

    public VecinoController(
            VecinoRepository vecinoRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.vecinoRepository = vecinoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Page<Vecino> listar(
            @RequestParam Long usuarioId,
            Pageable pageable
    ) {
        List<Long> comunidadIds = jdbcTemplate.queryForList(
                """
                SELECT c.id
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

        if (comunidadIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return vecinoRepository.findByComunidadIdIn(
                comunidadIds,
                pageable
        );
    }

    @GetMapping("/comunidad/{comunidadId}")
    public Page<Vecino> listarPorComunidad(
            @PathVariable Long comunidadId,
            Pageable pageable
    ) {
        return vecinoRepository.findByComunidadId(comunidadId, pageable);
    }
}