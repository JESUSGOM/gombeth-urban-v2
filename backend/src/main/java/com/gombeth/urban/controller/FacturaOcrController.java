package com.gombeth.urban.controller;

import com.gombeth.urban.dto.FacturaOcrResultado;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.FacturaOcrService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/gastos")
public class FacturaOcrController {

    private final FacturaOcrService facturaOcrService;
    private final AccesoComunidadService accesoComunidadService;

    public FacturaOcrController(
            FacturaOcrService facturaOcrService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.facturaOcrService =
                facturaOcrService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @PostMapping(
            value = "/ocr",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public FacturaOcrResultado analizarFactura(
            @RequestParam Long comunidadId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        if (
                comunidadId == null
                        || comunidadId <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad es obligatoria."
            );
        }

        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        try {
            return facturaOcrService.analizarFactura(
                    file
            );

        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage(),
                    error
            );

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    error.getMessage(),
                    error
            );
        }
    }
}