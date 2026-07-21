package com.gombeth.urban.controller;

import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.service.ContabilidadAutomaticaService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/contabilidad")
public class ContabilidadController {

    private final ContabilidadAutomaticaService contabilidadAutomaticaService;
    private final ContabilidadMovimientoRepository movimientoRepository;

    public ContabilidadController(
            ContabilidadAutomaticaService contabilidadAutomaticaService,
            ContabilidadMovimientoRepository movimientoRepository
    ) {
        this.contabilidadAutomaticaService = contabilidadAutomaticaService;
        this.movimientoRepository = movimientoRepository;
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

    @GetMapping("/icac/resumen")
    public Map<String, Object> resumenIcac(
            @RequestParam Long comunidadId
    ) {
        BigDecimal saldoDeudores =
                movimientoRepository.sumDebeHaberByCuentaPrefix(
                        comunidadId,
                        "447",
                        "430"
                );

        BigDecimal saldoIngresos =
                movimientoRepository.sumDebeHaberByCuentaPrefix(
                        comunidadId,
                        "705",
                        "731"
                );

        BigDecimal saldoBanco =
                movimientoRepository.sumDebeHaberByCuentaPrefix(
                        comunidadId,
                        "572",
                        null
                );

        return Map.of(
                "comunidadId", comunidadId,
                "deudores", saldoDeudores,
                "ingresos", saldoIngresos,
                "banco", saldoBanco
        );
    }
}