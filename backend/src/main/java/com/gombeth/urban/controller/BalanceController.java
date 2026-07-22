package com.gombeth.urban.controller;

import com.gombeth.urban.dto.BalanceSumaSaldoDTO;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.BalanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/balance")
@CrossOrigin(origins = "http://localhost:4200")
public class BalanceController {

    private final BalanceService service;

    private final AccesoComunidadService
            accesoComunidadService;

    public BalanceController(
            BalanceService service,
            AccesoComunidadService accesoComunidadService
    ) {
        this.service = service;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @GetMapping
    public List<BalanceSumaSaldoDTO> obtener(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        return service.obtenerBalance(
                comunidadId
        );
    }
}