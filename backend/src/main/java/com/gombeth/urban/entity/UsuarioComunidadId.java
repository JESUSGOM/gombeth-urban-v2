package com.gombeth.urban.entity;

import java.io.Serializable;
import java.util.Objects;

public class UsuarioComunidadId implements Serializable {

    private Long usuarioId;
    private Long comunidadId;

    public UsuarioComunidadId() {}

    public UsuarioComunidadId(Long usuarioId, Long comunidadId) {
        this.usuarioId = usuarioId;
        this.comunidadId = comunidadId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioComunidadId that)) return false;
        return Objects.equals(usuarioId, that.usuarioId)
                && Objects.equals(comunidadId, that.comunidadId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, comunidadId);
    }
}