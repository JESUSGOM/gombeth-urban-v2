package com.gombeth.urban.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario_comunidades")
@IdClass(UsuarioComunidadId.class)
public class UsuarioComunidad {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Id
    @Column(name = "comunidad_id")
    private Long comunidadId;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getComunidadId() {
        return comunidadId;
    }
}