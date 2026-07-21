package com.gombeth.urban.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "remesa_linea_conceptos")
public class RemesaLineaConcepto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "remesa_linea_id", nullable = false)
    private Long remesaLineaId;

    @Column(name = "concepto_cobro_id")
    private Long conceptoCobroId;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(nullable = false, precision = 11, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "agrupado_en_ultima_linea", nullable = false)
    private Boolean agrupadoEnUltimaLinea = false;

    public Long getId() {
        return id;
    }

    public Long getRemesaLineaId() {
        return remesaLineaId;
    }

    public void setRemesaLineaId(Long remesaLineaId) {
        this.remesaLineaId = remesaLineaId;
    }

    public Long getConceptoCobroId() {
        return conceptoCobroId;
    }

    public void setConceptoCobroId(Long conceptoCobroId) {
        this.conceptoCobroId = conceptoCobroId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getAgrupadoEnUltimaLinea() {
        return agrupadoEnUltimaLinea;
    }

    public void setAgrupadoEnUltimaLinea(Boolean agrupadoEnUltimaLinea) {
        this.agrupadoEnUltimaLinea = agrupadoEnUltimaLinea;
    }
}