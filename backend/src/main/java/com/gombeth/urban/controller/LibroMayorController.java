package com.gombeth.urban.controller;

import com.gombeth.urban.dto.LibroMayorDTO;
import com.gombeth.urban.service.LibroMayorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mayor")
@CrossOrigin(origins = "http://localhost:4200")
public class LibroMayorController {

    private final LibroMayorService libroMayorService;

    public LibroMayorController(
            LibroMayorService libroMayorService
    ) {
        this.libroMayorService = libroMayorService;
    }

    @GetMapping
    public LibroMayorDTO obtenerMayor(
            @RequestParam Long comunidadId,
            @RequestParam Long cuentaId,
            @RequestParam(required = false) Integer ejercicio
    ) {
        return libroMayorService.obtenerMayor(
                comunidadId,
                cuentaId
        );
    }
}