package com.gombeth.urban.controller;

import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.service.ContabilidadGastoService;
import org.springframework.web.bind.annotation.*;
import com.gombeth.urban.service.ContabilidadAutomaticaService;

import java.util.List;

@RestController
@RequestMapping("/api/gastos")
public class ContabilidadGastoController {

    private final ContabilidadGastoService gastoService;
    private final ContabilidadAutomaticaService contabilidadAutomaticaService;

    public ContabilidadGastoController(
            ContabilidadGastoService gastoService,
            ContabilidadAutomaticaService contabilidadAutomaticaService
    ) {
        this.gastoService = gastoService;
        this.contabilidadAutomaticaService = contabilidadAutomaticaService;
    }

    @PostMapping("/{id}/contabilizar")
    public ContabilidadGasto contabilizar(
            @PathVariable Long id
    ) {
        contabilidadAutomaticaService.contabilizarGasto(id);
        return gastoService.findById(id);
    }

    @GetMapping
    public List<ContabilidadGasto> listar(
            @RequestParam Long comunidadId
    ) {
        return gastoService.listarPorComunidad(comunidadId);
    }

    @PostMapping
    public ContabilidadGasto crear(
            @RequestBody ContabilidadGasto gasto
    ) {
        return gastoService.crear(gasto);
    }
}