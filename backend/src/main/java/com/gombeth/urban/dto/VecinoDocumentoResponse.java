package com.gombeth.urban.dto;

import com.gombeth.urban.entity.VecinoDocumento;

import java.time.LocalDateTime;

public class VecinoDocumentoResponse {

    private final Long id;
    private final Long vecinoId;
    private final String tipoDocumento;
    private final String nombreArchivo;
    private final String contentType;
    private final long tamanio;
    private final LocalDateTime fechaSubida;

    public VecinoDocumentoResponse(
            Long id,
            Long vecinoId,
            String tipoDocumento,
            String nombreArchivo,
            String contentType,
            long tamanio,
            LocalDateTime fechaSubida
    ) {
        this.id = id;
        this.vecinoId = vecinoId;
        this.tipoDocumento = tipoDocumento;
        this.nombreArchivo = nombreArchivo;
        this.contentType = contentType;
        this.tamanio = tamanio;
        this.fechaSubida = fechaSubida;
    }

    public static VecinoDocumentoResponse desde(
            VecinoDocumento documento
    ) {
        long tamanioDocumento = 0L;

        if (documento.getContenido() != null) {
            tamanioDocumento = documento.getContenido().length;
        }

        return new VecinoDocumentoResponse(
                documento.getId(),
                documento.getVecinoId(),
                documento.getTipoDocumento(),
                documento.getNombreArchivo(),
                documento.getContentType(),
                tamanioDocumento,
                documento.getFechaSubida()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getVecinoId() {
        return vecinoId;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getContentType() {
        return contentType;
    }

    public long getTamanio() {
        return tamanio;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }
}