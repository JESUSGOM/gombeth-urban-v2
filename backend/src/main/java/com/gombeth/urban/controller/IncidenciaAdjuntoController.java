package com.gombeth.urban.controller;

import com.gombeth.urban.dto.IncidenciaAdjuntoResponse;
import com.gombeth.urban.service.IncidenciaAdjuntoRemotoService;
import com.gombeth.urban.service.IncidenciaAdjuntoRemotoService.ContenidoAdjunto;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaAdjuntoController {

    private final IncidenciaAdjuntoRemotoService service;

    public IncidenciaAdjuntoController(
            IncidenciaAdjuntoRemotoService service
    ) {
        this.service = service;
    }

    @GetMapping("/{incidenciaId}/adjuntos")
    public List<IncidenciaAdjuntoResponse> listarAdjuntos(
            @PathVariable Long incidenciaId
    ) {
        return service.listarPorIncidencia(
                incidenciaId
        );
    }

    @GetMapping(
            "/{incidenciaId}/adjuntos/{adjuntoId}/contenido"
    )
    public ResponseEntity<byte[]> obtenerContenido(
            @PathVariable Long incidenciaId,
            @PathVariable Long adjuntoId
    ) {
        ContenidoAdjunto adjunto = service.obtenerContenido(
                incidenciaId,
                adjuntoId
        );

        byte[] contenido = adjunto.getContenido();

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
                .contentType(adjunto.getMediaType())
                .contentLength(contenido.length)
                .cacheControl(CacheControl.noStore())
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
                .body(contenido);
    }
}