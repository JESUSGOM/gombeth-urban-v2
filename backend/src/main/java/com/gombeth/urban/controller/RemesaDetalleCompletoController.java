package com.gombeth.urban.controller;

import com.gombeth.urban.dto.remesa.RemesaDetalleCompletoResponse;
import com.gombeth.urban.service.RemesaDetalleCompletoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/remesas")
@CrossOrigin(origins = "http://localhost:4200")
public class RemesaDetalleCompletoController {

    private final RemesaDetalleCompletoService service;

    public RemesaDetalleCompletoController(
            RemesaDetalleCompletoService service
    ) {
        this.service = service;
    }

    @GetMapping("/{id}/detalle-completo")
    public RemesaDetalleCompletoResponse obtenerDetalleCompleto(
            @PathVariable Long id
    ) {
        return service.obtenerDetalle(id);
    }
}