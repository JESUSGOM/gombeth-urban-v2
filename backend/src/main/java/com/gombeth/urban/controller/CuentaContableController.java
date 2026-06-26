package com.gombeth.urban.controller;

import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.service.CuentaContableService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas-contables")
@CrossOrigin(origins = "http://localhost:4200")
public class CuentaContableController {

    private final CuentaContableService service;

    public CuentaContableController(CuentaContableService service) {
        this.service = service;
    }

    @GetMapping("/comunidad/{id}")
    public List<CuentaContable> findByComunidad(@PathVariable Long id) {
        return service.findByComunidad(id);
    }
}