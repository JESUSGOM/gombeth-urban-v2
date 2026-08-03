package com.gombeth.urban.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "remesa_eventos")
public class RemesaEvento {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "remesa_id",
            nullable = false
    )
    private Long remesaId;

    @Column(
            name = "comunidad_id",
            nullable = false
    )
    private Long comunidadId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "tipo_evento",
            nullable = false,
            length = 40
    )
    private RemesaEventoTipo tipoEvento;

    @Column(
            name = "estado_anterior",
            length = 30
    )
    private String estadoAnterior;

    @Column(
            name = "estado_nuevo",
            length = 30
    )
    private String estadoNuevo;

    @Column(length = 10)
    private String formato;

    @Column(
            name = "nombre_archivo",
            length = 255
    )
    private String nombreArchivo;

    @Column(
            name = "fecha_evento",
            nullable = false
    )
    private LocalDateTime fechaEvento;

    @Column(length = 500)
    private String detalle;

    public Long getId() {
        return id;
    }

    public Long getRemesaId() {
        return remesaId;
    }

    public void setRemesaId(
            Long remesaId
    ) {
        this.remesaId = remesaId;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(
            Long comunidadId
    ) {
        this.comunidadId = comunidadId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(
            Long usuarioId
    ) {
        this.usuarioId = usuarioId;
    }

    public RemesaEventoTipo getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(
            RemesaEventoTipo tipoEvento
    ) {
        this.tipoEvento = tipoEvento;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(
            String estadoAnterior
    ) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(
            String estadoNuevo
    ) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(
            String formato
    ) {
        this.formato = formato;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(
            String nombreArchivo
    ) {
        this.nombreArchivo = nombreArchivo;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(
            LocalDateTime fechaEvento
    ) {
        this.fechaEvento = fechaEvento;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(
            String detalle
    ) {
        this.detalle = detalle;
    }
}