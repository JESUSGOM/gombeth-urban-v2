package com.gombeth.urban.controller;

import com.gombeth.urban.dto.DiarioContableDTO;
import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.service.DiarioContableService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diario")
@CrossOrigin(origins = "http://localhost:4200")
public class DiarioContableController {

    private final DiarioContableService diarioService;

    public DiarioContableController(
            DiarioContableService diarioService
    ) {
        this.diarioService = diarioService;
    }

    @GetMapping
    public List<ContabilidadAsiento> listar(
            @RequestParam Long comunidadId,
            @RequestParam Integer ejercicio
    ) {
        return diarioService.listar(
                comunidadId,
                ejercicio
        );
    }

    @GetMapping("/{id}")
    public DiarioContableDTO detalle(
            @PathVariable Long id
    ) {
        return diarioService.detalle(id);
    }
}