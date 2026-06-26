package com.gombeth.urban.dto;

import java.math.BigDecimal;

public class ConceptoCobroDTO {

    private Long id;

    private String descripcion;

    private BigDecimal importe;

    private String periodicidad;

    private Long cuentaContableId;

    private String cuentaContableNombre;

    private Long comunidadId;

    private Boolean activo;

    private String cuentaContableCodigo;

    // GETTERS Y SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPeriodicidad() {
        return periodicidad;
    }

    public void setPeriodicidad(String periodicidad) {
        this.periodicidad = periodicidad;
    }

    public Long getCuentaContableId() {
        return cuentaContableId;
    }

    public void setCuentaContableId(Long cuentaContableId) {
        this.cuentaContableId = cuentaContableId;
    }

    public String getCuentaContableNombre() {
        return cuentaContableNombre;
    }

    public void setCuentaContableNombre(String cuentaContableNombre) {
        this.cuentaContableNombre = cuentaContableNombre;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getCuentaContableCodigo() {
        return cuentaContableCodigo;
    }

    public void setCuentaContableCodigo(String cuentaContableCodigo) {
        this.cuentaContableCodigo = cuentaContableCodigo;
    }
}