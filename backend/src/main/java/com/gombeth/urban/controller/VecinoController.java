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

    @GetMapping("/{id}")
    public Vecino obtenerPorId(@PathVariable Long id) {
        return vecinoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vecino no encontrado"));
    }

    @PutMapping("/{id}")
    public Vecino actualizar(
            @PathVariable Long id,
            @RequestBody Vecino datos
    ) {
        Vecino vecino = vecinoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vecino no encontrado"));

        vecino.setNombre(datos.getNombre());
        vecino.setNif(datos.getNif());
        vecino.setIban(datos.getIban());
        vecino.setBic(datos.getBic());
        vecino.setEmail(datos.getEmail());

        vecino.setTelefono1(datos.getTelefono1());
        vecino.setTelefono2(datos.getTelefono2());
        vecino.setTelefono3(datos.getTelefono3());

        vecino.setDireccion(datos.getDireccion());
        vecino.setPoblacion(datos.getPoblacion());
        vecino.setProvincia(datos.getProvincia());
        vecino.setCodigoPostal(datos.getCodigoPostal());
        vecino.setPaisCod(datos.getPaisCod());

        vecino.setVivienda(datos.getVivienda());
        vecino.setDomiciliado(datos.isDomiciliado());
        vecino.setActivo(datos.isActivo());

        return vecinoRepository.save(vecino);
    }

    @PostMapping
    public Vecino crear(@RequestBody Vecino datos) {
        if (datos.getComunidadId() == null) {
            throw new RuntimeException("La comunidad es obligatoria");
        }

        datos.setActivo(true);

        if (datos.getPaisCod() == null || datos.getPaisCod().isBlank()) {
            datos.setPaisCod("ES");
        }

        return vecinoRepository.save(datos);
    }
}