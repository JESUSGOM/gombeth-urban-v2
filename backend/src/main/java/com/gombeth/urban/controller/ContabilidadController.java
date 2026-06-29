package com.gombeth.urban.controller;

import com.gombeth.urban.service.ContabilidadAutomaticaService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contabilidad")
public class ContabilidadController {

    private final ContabilidadAutomaticaService contabilidadAutomaticaService;

    public ContabilidadController(
            ContabilidadAutomaticaService contabilidadAutomaticaService
    ) {
        this.contabilidadAutomaticaService = contabilidadAutomaticaService;
    }

    @PostMapping("/regularizar-cobros")
    public Map<String, Object> regularizarCobros(
            @RequestParam Long comunidadId
    ) {
        int asientosGenerados =
                contabilidadAutomaticaService.regularizarCobrosConciliadosComunidad(
                        comunidadId
                );

        return Map.of(
                "comunidadId", comunidadId,
                "asientosGenerados", asientosGenerados,
                "mensaje", "Regularización contable de cobros finalizada"
        );
    }
}