package com.gombeth.urban.dto;

public class ComunidadActivaDTO {

    private Long id;
    private String nombre;

    public ComunidadActivaDTO(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}