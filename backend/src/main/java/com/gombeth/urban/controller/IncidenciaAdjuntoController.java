package com.gombeth.urban.controller;

import com.gombeth.urban.dto.GestionIncidenciaResponse;
import com.gombeth.urban.dto.IncidenciaAdjuntoResponse;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.GestionIncidenciaService;
import com.gombeth.urban.service.IncidenciaAdjuntoRemotoService;
import com.gombeth.urban.service.IncidenciaAdjuntoRemotoService.ContenidoAdjunto;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaAdjuntoController {

    private final IncidenciaAdjuntoRemotoService service;

    private final GestionIncidenciaService
            gestionIncidenciaService;

    private final AccesoComunidadService
            accesoComunidadService;

    public IncidenciaAdjuntoController(
            IncidenciaAdjuntoRemotoService service,
            GestionIncidenciaService gestionIncidenciaService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.service = service;

        this.gestionIncidenciaService =
                gestionIncidenciaService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Impide consultar la relación de fotografías de una
     * incidencia perteneciente a una comunidad ajena.
     */
    @GetMapping("/{incidenciaId}/adjuntos")
    public List<IncidenciaAdjuntoResponse> listarAdjuntos(
            @PathVariable Long incidenciaId,
            Authentication authentication
    ) {
        validarAccesoIncidencia(
                authentication,
                incidenciaId
        );

        return service.listarPorIncidencia(
                incidenciaId
        );
    }

    /**
     * La autorización se comprueba antes de solicitar el
     * archivo al alojamiento remoto.
     */
    @GetMapping(
            "/{incidenciaId}/adjuntos/{adjuntoId}/contenido"
    )
    public ResponseEntity<byte[]> obtenerContenido(
            @PathVariable Long incidenciaId,
            @PathVariable Long adjuntoId,
            Authentication authentication
    ) {
        validarAccesoIncidencia(
                authentication,
                incidenciaId
        );

        ContenidoAdjunto adjunto =
                service.obtenerContenido(
                        incidenciaId,
                        adjuntoId
                );

        byte[] contenido =
                adjunto.getContenido();

        ContentDisposition disposicion =
                ContentDisposition
                        .inline()
                        .filename(
                                adjunto.getNombreOriginal(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity
                .ok()
                .contentType(
                        adjunto.getMediaType()
                )
                .contentLength(
                        contenido.length
                )
                .cacheControl(
                        CacheControl.noStore()
                )
                .header(
                        HttpHeaders.PRAGMA,
                        "no-cache"
                )
                .header(
                        HttpHeaders.EXPIRES,
                        "0"
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposicion.toString()
                )
                .body(
                        contenido
                );
    }

    private void validarAccesoIncidencia(
            Authentication authentication,
            Long incidenciaId
    ) {
        GestionIncidenciaResponse incidencia =
                gestionIncidenciaService.obtener(
                        incidenciaId
                );

        if (
                incidencia.getComunidad() == null
                        || incidencia
                        .getComunidad()
                        .getId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La incidencia no tiene una comunidad "
                            + "asociada correctamente."
            );
        }

        accesoComunidadService.validarAcceso(
                authentication,
                incidencia
                        .getComunidad()
                        .getId()
        );
    }
}