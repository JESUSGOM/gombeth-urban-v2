package com.gombeth.urban.controller;

import com.gombeth.urban.dto.ConceptoCobroDTO;
import com.gombeth.urban.entity.ConceptoCobro;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ConceptoCobroService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/conceptos-cobro")
public class ConceptoCobroController {

    private final ConceptoCobroService service;

    private final AccesoComunidadService accesoComunidadService;

    public ConceptoCobroController(
            ConceptoCobroService service,
            AccesoComunidadService accesoComunidadService
    ) {
        this.service = service;
        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Lista únicamente los conceptos de una comunidad
     * accesible por el usuario autenticado.
     */
    @GetMapping("/comunidad/{id}")
    public List<ConceptoCobroDTO> findByComunidad(
            @PathVariable Long id,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                id
        );

        return service.findByComunidad(
                id
        );
    }

    /**
     * Comprueba que el concepto pertenece a una comunidad
     * accesible antes de devolverlo.
     */
    @GetMapping("/{id}")
    public ConceptoCobroDTO getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ConceptoCobroDTO concepto =
                service.findById(
                        id
                );

        accesoComunidadService.validarAcceso(
                authentication,
                concepto.getComunidadId()
        );

        return concepto;
    }

    /**
     * Crea un concepto únicamente dentro de una comunidad
     * accesible por el usuario autenticado.
     *
     * Una operación de alta no puede recibir un identificador,
     * porque podría provocar la modificación de un concepto
     * existente mediante repository.save(...).
     */
    @PostMapping
    public ConceptoCobroDTO create(
            @RequestBody ConceptoCobro concepto,
            Authentication authentication
    ) {
        if (concepto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del concepto son obligatorios."
            );
        }

        if (concepto.getId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se permite indicar un identificador "
                            + "al crear un concepto de cobro."
            );
        }

        if (concepto.getComunidadId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad del concepto es obligatoria."
            );
        }

        accesoComunidadService.validarAcceso(
                authentication,
                concepto.getComunidadId()
        );

        return service.create(
                concepto
        );
    }

    /**
     * La autorización se comprueba usando la comunidad
     * del concepto existente en la base de datos.
     *
     * ConceptoCobroService conserva el comunidadId original
     * durante la actualización.
     */
    @PutMapping("/{id}")
    public ConceptoCobroDTO update(
            @PathVariable Long id,
            @RequestBody ConceptoCobro concepto,
            Authentication authentication
    ) {
        ConceptoCobroDTO existente =
                service.findById(
                        id
                );

        accesoComunidadService.validarAcceso(
                authentication,
                existente.getComunidadId()
        );

        return service.update(
                id,
                concepto
        );
    }
}