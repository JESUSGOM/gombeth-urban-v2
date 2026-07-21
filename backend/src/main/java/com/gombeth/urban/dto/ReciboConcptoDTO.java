package com.gombeth.urban.dto;

import java.math.BigDecimal;

public class ReciboConcptoDTO {

    private Long conceptoCobroId;
    private String desciprcion;
    private BigDecimal importe;
    private Integer orden;
    private Boolean agrupado;

    public ReciboConcptoDTO() {
    }

    public ReciboConcptoDTO(Long conceptoCobroId, String desciprcion, BigDecimal importe, Integer orden, Boolean agrupado) {
        this.conceptoCobroId = conceptoCobroId;
        this.desciprcion = desciprcion;
        this.importe = importe;
        this.orden = orden;
        this.agrupado = agrupado;
    }
    public Long getConceptoCobroId() {
        return conceptoCobroId;
    }

    public void setConceptoCobroId(Long conceptoCobroId) {
        this.conceptoCobroId = conceptoCobroId;
    }

    public String getDescripcion() {
        return desciprcion;
    }

    public void setDescripcion(String descripcion) {
        this.desciprcion = descripcion;
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

    public Boolean getAgrupado() {
        return agrupado;
    }

    public void setAgrupado(Boolean agrupado) {
        this.agrupado = agrupado;
    }
}