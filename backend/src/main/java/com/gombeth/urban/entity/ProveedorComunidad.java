package com.gombeth.urban.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "proveedor_comunidades")
public class ProveedorComunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "proveedor_id",
            nullable = false
    )
    private Long proveedorId;

    @Column(
            name = "comunidad_id",
            nullable = false
    )
    private Long comunidadId;

    @Column(
            name = "cuenta_contable_id",
            nullable = false
    )
    private Long cuentaContableId;

    @Column(nullable = false)
    private Boolean activo = true;

    public ProveedorComunidad() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(
            Long proveedorId
    ) {
        this.proveedorId = proveedorId;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(
            Long comunidadId
    ) {
        this.comunidadId = comunidadId;
    }

    public Long getCuentaContableId() {
        return cuentaContableId;
    }

    public void setCuentaContableId(
            Long cuentaContableId
    ) {
        this.cuentaContableId = cuentaContableId;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(
            Boolean activo
    ) {
        this.activo = activo;
    }
}