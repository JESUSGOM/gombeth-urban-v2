package com.gombeth.urban.controller;

import com.gombeth.urban.entity.ConceptoCobro;
import com.gombeth.urban.repository.ConceptoCobroRepository;
import com.gombeth.urban.service.ConceptoCobroService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conceptos-cobro")
@CrossOrigin(origins = "http://localhost:4200")
public class ConceptoCobroController {

    private final ConceptoCobroService service;
    private final ConceptoCobroRepository repository;

    public ConceptoCobroController(ConceptoCobroService service,
                                   ConceptoCobroRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    // =========================
    // LISTAR POR COMUNIDAD
    // =========================
    @GetMapping("/comunidad/{id}")
    public List<?> findByComunidad(@PathVariable Long id) {
        return service.findByComunidad(id);
    }

    // =========================
    // GET POR ID
    // =========================
    @GetMapping("/{id}")
    public ConceptoCobro getById(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }

    // =========================
    // CREAR
    // =========================
    @PostMapping
    public ConceptoCobro create(@RequestBody ConceptoCobro c) {
        return service.save(c);
    }

    // =========================
    // ACTUALIZAR
    // =========================
    @PutMapping("/{id}")
    public ConceptoCobro update(@PathVariable Long id,
                                @RequestBody ConceptoCobro c) {

        c.setId(id);
        return service.save(c);
    }
}