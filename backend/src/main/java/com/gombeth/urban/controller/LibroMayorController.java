package com.gombeth.urban.controller;

import com.gombeth.urban.dto.LibroMayorDTO;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.CuentaContableService;
import com.gombeth.urban.service.LibroMayorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@RestController
@RequestMapping("/api/mayor")
public class LibroMayorController {

    private final LibroMayorService
            libroMayorService;

    private final CuentaContableService
            cuentaContableService;

    private final AccesoComunidadService
            accesoComunidadService;

    public LibroMayorController(
            LibroMayorService libroMayorService,
            CuentaContableService cuentaContableService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.libroMayorService =
                libroMayorService;

        this.cuentaContableService =
                cuentaContableService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @GetMapping
    public LibroMayorDTO obtenerMayor(
            @RequestParam Long comunidadId,
            @RequestParam Long cuentaId,
            @RequestParam(required = false)
            Integer ejercicio,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        validarCuentaPerteneceAComunidad(
                comunidadId,
                cuentaId
        );

        return libroMayorService.obtenerMayor(
                comunidadId,
                cuentaId,
                ejercicio
        );
    }

    private void validarCuentaPerteneceAComunidad(
            Long comunidadId,
            Long cuentaId
    ) {
        boolean pertenece =
                cuentaContableService
                        .findByComunidad(comunidadId)
                        .stream()
                        .anyMatch(
                                cuenta ->
                                        Objects.equals(
                                                cuenta.getId(),
                                                cuentaId
                                        )
                        );

        if (!pertenece) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La cuenta contable no existe "
                            + "en la comunidad indicada."
            );
        }
    }
}
