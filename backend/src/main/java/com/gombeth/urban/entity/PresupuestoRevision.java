package com.gombeth.urban.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "presupuestos_revision")
public class PresupuestoRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comunidad_id", nullable = false)
    private Long comunidadId;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "mes_inicio", nullable = false)
    private Integer mesInicio;

    @Column(name = "mes_fin", nullable = false)
    private Integer mesFin;

    @Column(name = "importe_revision", precision = 10, scale = 2)
    private BigDecimal importeRevision;

    @Column(nullable = false)
    private String estado;

    @Column(name = "motivo_revision")
    private String motivoRevision;

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    public Long getId() {
        return id;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getMesInicio() {
        return mesInicio;
    }

    public void setMesInicio(Integer mesInicio) {
        this.mesInicio = mesInicio;
    }

    public Integer getMesFin() {
        return mesFin;
    }

    public void setMesFin(Integer mesFin) {
        this.mesFin = mesFin;
    }

    public BigDecimal getImporteRevision() {
        return importeRevision;
    }

    public void setImporteRevision(BigDecimal importeRevision) {
        this.importeRevision = importeRevision;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivoRevision() {
        return motivoRevision;
    }

    public void setMotivoRevision(String motivoRevision) {
        this.motivoRevision = motivoRevision;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }
}