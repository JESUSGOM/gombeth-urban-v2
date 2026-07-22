package com.gombeth.urban.controller;

import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.CuentaContableService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas-contables")
public class CuentaContableController {

    private final CuentaContableService service;

    private final AccesoComunidadService
            accesoComunidadService;

    public CuentaContableController(
            CuentaContableService service,
            AccesoComunidadService accesoComunidadService
    ) {
        this.service = service;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @GetMapping("/comunidad/{id}")
    public List<CuentaContable> findByComunidad(
            @PathVariable Long id,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                id
        );

        return service.findByComunidad(id);
    }
}
