package com.gombeth.urban.controller;

import com.gombeth.urban.dto.BalanceSumaSaldoDTO;
import com.gombeth.urban.service.BalanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/balance")
@CrossOrigin(origins = "http://localhost:4200")
public class BalanceController {

    private final BalanceService service;

    public BalanceController(BalanceService service) {
        this.service = service;
    }

    @GetMapping
    public List<BalanceSumaSaldoDTO> obtener(
            @RequestParam Long comunidadId
    ) {
        return service.obtenerBalance(comunidadId);
    }
}