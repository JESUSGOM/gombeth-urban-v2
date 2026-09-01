package com.gombeth.urban.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

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

    public UsuarioComunidad() {
    }

    public UsuarioComunidad(
            Long usuarioId,
            Long comunidadId
    ) {
        this.usuarioId = usuarioId;
        this.comunidadId = comunidadId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(
            Long usuarioId
    ) {
        this.usuarioId = usuarioId;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(
            Long comunidadId
    ) {
        this.comunidadId = comunidadId;
    }
}