package com.gombeth.urban.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "vecino_documentos")
public class VecinoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "vecino_id",
            nullable = false
    )
    private Long vecinoId;

    @Column(
            name = "tipo_documento",
            nullable = false,
            length = 50
    )
    private String tipoDocumento;

    @Column(
            name = "nombre_archivo",
            nullable = false,
            length = 255
    )
    private String nombreArchivo;

    @Column(
            name = "content_type",
            nullable = false,
            length = 100
    )
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(
            name = "contenido",
            nullable = false,
            columnDefinition = "LONGBLOB"
    )
    private byte[] contenido;

    @Column(
            name = "fecha_subida",
            nullable = false
    )
    private LocalDateTime fechaSubida;

    public VecinoDocumento() {
        this.fechaSubida = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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