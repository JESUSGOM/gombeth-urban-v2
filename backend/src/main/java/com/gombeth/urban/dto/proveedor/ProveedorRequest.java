package com.gombeth.urban.dto.proveedor;

public class ProveedorRequest {

    private String nombre;
    private String nifCif;
    private String telefono;
    private String email;
    private String observaciones;
    private Boolean activo;

    public ProveedorRequest() {
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
