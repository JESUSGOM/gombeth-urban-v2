package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.repository.ComunidadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comunidades")
public class ComunidadController {

    private final ComunidadRepository repository;

    public ComunidadController(ComunidadRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<Comunidad> listar(
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return repository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public Comunidad obtenerPorId(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada con ID: " + id));
    }
}