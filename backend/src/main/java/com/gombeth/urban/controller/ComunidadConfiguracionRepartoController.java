package com.gombeth.urban.controller;

import com.gombeth.urban.entity.ComunidadConfiguracionReparto;
import com.gombeth.urban.repository.ComunidadConfiguracionRepartoRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comunidades")
public class ComunidadConfiguracionRepartoController {

    private final ComunidadConfiguracionRepartoRepository repository;

    public ComunidadConfiguracionRepartoController(
            ComunidadConfiguracionRepartoRepository repository
    ) {
        this.repository = repository;
    }

    @GetMapping("/{comunidadId}/configuracion-reparto")
    public ComunidadConfiguracionReparto obtener(
            @PathVariable Long comunidadId
    ) {
        return repository.findByComunidadId(comunidadId)
                .orElseGet(() -> {
                    ComunidadConfiguracionReparto config =
                            new ComunidadConfiguracionReparto();

                    config.setComunidadId(comunidadId);
                    config.setMetodoReparto("COEFICIENTE");

                    return config;
                });
    }

    @PutMapping("/{comunidadId}/configuracion-reparto")
    public ComunidadConfiguracionReparto guardar(
            @PathVariable Long comunidadId,
            @RequestBody ComunidadConfiguracionReparto datos
    ) {
        ComunidadConfiguracionReparto config =
                repository.findByComunidadId(comunidadId)
                        .orElseGet(ComunidadConfiguracionReparto::new);

        config.setComunidadId(comunidadId);
        config.setMetodoReparto(datos.getMetodoReparto());

        return repository.save(config);
    }
}