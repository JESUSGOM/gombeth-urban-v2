package com.gombeth.urban.repository;

import com.gombeth.urban.entity.UsuarioComunidad;
import com.gombeth.urban.entity.UsuarioComunidadId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioComunidadRepository
        extends JpaRepository<UsuarioComunidad, UsuarioComunidadId> {

    boolean existsByUsuarioIdAndComunidadId(
            Long usuarioId,
            Long comunidadId
    );
}