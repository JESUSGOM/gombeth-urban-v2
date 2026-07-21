package com.gombeth.urban.dto;

import com.gombeth.urban.entity.IncidenciaAdjunto;

import java.time.LocalDateTime;

public class IncidenciaAdjuntoResponse {

    private final Long id;
    private final Long incidenciaId;
    private final String nombreOriginal;
    private final String contentType;
    private final Long tamanio;
    private final LocalDateTime fechaSubida;

    public IncidenciaAdjuntoResponse(
            Long id,
            Long incidenciaId,
            String nombreOriginal,
            String contentType,
            Long tamanio,
            LocalDateTime fechaSubida
    ) {
        this.id = id;
        this.incidenciaId = incidenciaId;
        this.nombreOriginal = nombreOriginal;
        this.contentType = contentType;
        this.tamanio = tamanio;
        this.fechaSubida = fechaSubida;
    }

    public static IncidenciaAdjuntoResponse desde(
            IncidenciaAdjunto adjunto
    ) {
        return new IncidenciaAdjuntoResponse(
                adjunto.getId(),
                adjunto.getIncidenciaId(),
                adjunto.getNombreOriginal(),
                adjunto.getContentType(),
                adjunto.getTamanio(),
                adjunto.getFechaSubida()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getIncidenciaId() {
        return incidenciaId;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getTamanio() {
        return tamanio;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }
}