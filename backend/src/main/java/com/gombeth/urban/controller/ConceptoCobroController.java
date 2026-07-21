package com.gombeth.urban.controller;

import com.gombeth.urban.dto.ConceptoCobroDTO;
import com.gombeth.urban.entity.ConceptoCobro;
import com.gombeth.urban.service.ConceptoCobroService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conceptos-cobro")
@CrossOrigin(origins = "http://localhost:4200")
public class ConceptoCobroController {

    private final ConceptoCobroService service;

    public ConceptoCobroController(
            ConceptoCobroService service) {

        this.service = service;
    }

    @GetMapping("/comunidad/{id}")
    public List<ConceptoCobroDTO> findByComunidad(
            @PathVariable Long id) {

        return service.findByComunidad(id);
    }

    @GetMapping("/{id}")
    public ConceptoCobroDTO getById(
            @PathVariable Long id) {

        return service.findById(id);
    }

    @PostMapping
    public ConceptoCobroDTO create(
            @RequestBody ConceptoCobro concepto) {

        return service.create(concepto);
    }

    @PutMapping("/{id}")
    public ConceptoCobroDTO update(
            @PathVariable Long id,
            @RequestBody ConceptoCobro concepto) {

        return service.update(
                id,
                concepto
        );
    }
}