package com.gombeth.urban.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "proveedores")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "administrador_id",
            nullable = false
    )
    private Long administradorId;

    @Column(
            nullable = false,
            length = 255
    )
    private String nombre;

    @Column(
            name = "nif_cif",
            length = 20
    )
    private String nifCif;

    @Column(length = 50)
    private String telefono;

    @Column(length = 255)
    private String email;

    @Column(length = 1000)
    private String observaciones;

    @Column(nullable = false)
    private Boolean activo = true;

    public Proveedor() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Long getAdministradorId() {
        return administradorId;
    }

    public void setAdministradorId(
            Long administradorId
    ) {
        this.administradorId = administradorId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(
            String nombre
    ) {
        this.nombre = nombre;
    }

    public String getNifCif() {
        return nifCif;
    }

    public void setNifCif(
            String nifCif
    ) {
        this.nifCif = nifCif;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(
            String telefono
    ) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(
            String email
    ) {
        this.email = email;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(
            String observaciones
    ) {
        this.observaciones = observaciones;
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