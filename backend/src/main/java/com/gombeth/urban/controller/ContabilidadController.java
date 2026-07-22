package com.gombeth.urban.controller;

import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ContabilidadAutomaticaService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/contabilidad")
public class ContabilidadController {

    private final ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    private final ContabilidadMovimientoRepository
            movimientoRepository;

    private final AccesoComunidadService
            accesoComunidadService;

    public ContabilidadController(
            ContabilidadAutomaticaService
                    contabilidadAutomaticaService,
            ContabilidadMovimientoRepository
                    movimientoRepository,
            AccesoComunidadService accesoComunidadService
    ) {
        this.contabilidadAutomaticaService =
                contabilidadAutomaticaService;

        this.movimientoRepository =
                movimientoRepository;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Genera los asientos pendientes exclusivamente para
     * una comunidad accesible por el usuario autenticado.
     */
    @PostMapping("/regularizar-cobros")
    public Map<String, Object> regularizarCobros(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        int asientosGenerados =
                contabilidadAutomaticaService
                        .regularizarCobrosConciliadosComunidad(
                                comunidadId
                        );

        return Map.of(
                "comunidadId",
                comunidadId,

                "asientosGenerados",
                asientosGenerados,

                "mensaje",
                "Regularización contable "
                        + "de cobros finalizada"
        );
    }

    /**
     * Devuelve el resumen contable ICAC después de comprobar
     * el acceso a la comunidad.
     */
    @GetMapping("/icac/resumen")
    public Map<String, Object> resumenIcac(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        BigDecimal saldoDeudores =
                movimientoRepository
                        .sumDebeHaberByCuentaPrefix(
                                comunidadId,
                                "447",
                                "430"
                        );

        BigDecimal saldoIngresos =
                movimientoRepository
                        .sumDebeHaberByCuentaPrefix(
                                comunidadId,
                                "705",
                                "731"
                        );

        BigDecimal saldoBanco =
                movimientoRepository
                        .sumDebeHaberByCuentaPrefix(
                                comunidadId,
                                "572",
                                null
                        );

        return Map.of(
                "comunidadId",
                comunidadId,

                "deudores",
                saldoDeudores,

                "ingresos",
                saldoIngresos,

                "banco",
                saldoBanco
        );
    }
}