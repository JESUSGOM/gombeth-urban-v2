package com.gombeth.urban.controller;

import com.gombeth.urban.entity.ComunidadConfiguracionReparto;
import com.gombeth.urban.repository.ComunidadConfiguracionRepartoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comunidades")
public class ComunidadConfiguracionRepartoController {

    private final ComunidadConfiguracionRepartoRepository
            repository;

    private final AccesoComunidadService
            accesoComunidadService;

    public ComunidadConfiguracionRepartoController(
            ComunidadConfiguracionRepartoRepository repository,
            AccesoComunidadService accesoComunidadService
    ) {
        this.repository = repository;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Devuelve la configuración de reparto únicamente
     * cuando el usuario autenticado tiene acceso
     * a la comunidad.
     */
    @GetMapping("/{comunidadId}/configuracion-reparto")
    public ComunidadConfiguracionReparto obtener(
            @PathVariable Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        return repository
                .findByComunidadId(comunidadId)
                .orElseGet(() -> {
                    ComunidadConfiguracionReparto config =
                            new ComunidadConfiguracionReparto();

                    config.setComunidadId(
                            comunidadId
                    );

                    config.setMetodoReparto(
                            "COEFICIENTE"
                    );

                    return config;
                });
    }

    /**
     * Guarda la configuración únicamente cuando
     * el usuario autenticado tiene acceso
     * a la comunidad indicada en la URL.
     *
     * El comunidadId recibido en el cuerpo JSON
     * no se utiliza.
     */
    @PutMapping("/{comunidadId}/configuracion-reparto")
    public ComunidadConfiguracionReparto guardar(
            @PathVariable Long comunidadId,
            @RequestBody
            ComunidadConfiguracionReparto datos,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        ComunidadConfiguracionReparto config =
                repository
                        .findByComunidadId(comunidadId)
                        .orElseGet(
                                ComunidadConfiguracionReparto::new
                        );

        config.setComunidadId(
                comunidadId
        );

        config.setMetodoReparto(
                datos.getMetodoReparto()
        );

        return repository.save(
                config
        );
    }
}