package com.gombeth.urban.controller;

import com.gombeth.urban.dto.remesa.RemesaDetalleCompletoResponse;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.repository.FicheroGeneradoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.RemesaDetalleCompletoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/remesas")
public class RemesaDetalleCompletoController {

    private final RemesaDetalleCompletoService service;
    private final FicheroGeneradoRepository ficheroGeneradoRepository;
    private final AccesoComunidadService accesoComunidadService;

    public RemesaDetalleCompletoController(
            RemesaDetalleCompletoService service,
            FicheroGeneradoRepository ficheroGeneradoRepository,
            AccesoComunidadService accesoComunidadService
    ) {
        this.service = service;
        this.ficheroGeneradoRepository = ficheroGeneradoRepository;
        this.accesoComunidadService = accesoComunidadService;
    }

    @GetMapping("/{id}/detalle-completo")
    public RemesaDetalleCompletoResponse obtenerDetalleCompleto(
            @PathVariable Long id,
            Authentication authentication
    ) {
        FicheroGenerado remesa = ficheroGeneradoRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Remesa no encontrada: " + id
                        )
                );

        accesoComunidadService.validarAcceso(
                authentication,
                remesa.getComunidadId()
        );

        return service.obtenerDetalle(id);
    }
}