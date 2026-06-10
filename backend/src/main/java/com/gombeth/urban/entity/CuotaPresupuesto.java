package com.gombeth.urban.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuotas_presupuesto")
public class CuotaPresupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comunidad_id", nullable = false)
    private Long comunidadId;

    @Column(name = "vecino_id", nullable = false)
    private Long vecinoId;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(precision = 10, scale = 4)
    private BigDecimal coeficiente;

    @Column(name = "importe_anual", precision = 10, scale = 2)
    private BigDecimal importeAnual;

    @Column(name = "importe_mensual", precision = 10, scale = 2)
    private BigDecimal importeMensual;

    @Column(length = 20)
    private String estado;

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    @Column(name = "mes_inicio")
    private Integer mesInicio = 1;

    @Column(name = "mes_fin")
    private Integer mesFin = 12;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "motivo_revision")
    private String motivoRevision;

    @Column(name = "revision_id")
    private Long revisionId;

    public Long getId() {
        return id;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public Long getVecinoId() {
        return vecinoId;
    }

    public void setVecinoId(Long vecinoId) {
        this.vecinoId = vecinoId;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCoeficiente() {
        return coeficiente;
    }

    public void setCoeficiente(BigDecimal coeficiente) {
        this.coeficiente = coeficiente;
    }

    public BigDecimal getImporteAnual() {
        return importeAnual;
    }

    public void setImporteAnual(BigDecimal importeAnual) {
        this.importeAnual = importeAnual;
    }

    public BigDecimal getImporteMensual() {
        return importeMensual;
    }

    public void setImporteMensual(BigDecimal importeMensual) {
        this.importeMensual = importeMensual;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getMotivoRevision() {
        return motivoRevision;
    }

    public Long getRevisionId() {
        return revisionId;
    }

    public void setMotivoRevision(String motivoRevision) {
        this.motivoRevision = motivoRevision;
    }

    public void setRevisionId(Long revisionId) {
        this.revisionId = revisionId;
    }

}