package com.gombeth.urban.controller;

import com.gombeth.urban.dto.DiarioContableDTO;
import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.DiarioContableService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/diario")
public class DiarioContableController {

    private final DiarioContableService
            diarioService;

    private final ContabilidadAsientoRepository
            asientoRepository;

    private final AccesoComunidadService
            accesoComunidadService;

    public DiarioContableController(
            DiarioContableService diarioService,
            ContabilidadAsientoRepository
                    asientoRepository,
            AccesoComunidadService accesoComunidadService
    ) {
        this.diarioService = diarioService;

        this.asientoRepository =
                asientoRepository;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @GetMapping
    public List<ContabilidadAsiento> listar(
            @RequestParam Long comunidadId,
            @RequestParam Integer ejercicio,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        return diarioService.listar(
                comunidadId,
                ejercicio
        );
    }

    /**
     * Obtiene primero el asiento para conocer su comunidad
     * y autoriza el acceso antes de cargar su detalle.
     */
    @GetMapping("/{id}")
    public DiarioContableDTO detalle(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ContabilidadAsiento asiento =
                asientoRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Asiento contable "
                                                        + "no encontrado."
                                        )
                        );

        accesoComunidadService.validarAcceso(
                authentication,
                asiento.getComunidadId()
        );

        return diarioService.detalle(id);
    }
}
