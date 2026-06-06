package com.gombeth.urban.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_vecino")
public class DocumentoVecino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vecinoId;

    private String tipoDocumento;

    private String nombreArchivo;

    private String contentType;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] contenido;

    private LocalDateTime fechaSubida;

    public Long getId() {
        return id;
    }

    public Long getVecinoId() {
        return vecinoId;
    }

    public void setVecinoId(Long vecinoId) {
        this.vecinoId = vecinoId;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }
}