package com.gombeth.urban.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "contabilidad_recibo_conceptos")
public class ContabilidadReciboConcepto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recibo_id", nullable = false)
    private Long reciboId;

    @Column(name = "concepto_cobro_id")
    private Long conceptoCobroId;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(nullable = false, precision = 11, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false)
    private Integer orden;

    public Long getId() {
        return id;
    }

    public Long getReciboId() {
        return reciboId;
    }

    public void setReciboId(Long reciboId) {
        this.reciboId = reciboId;
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
}