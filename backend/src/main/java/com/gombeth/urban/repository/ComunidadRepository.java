package com.gombeth.urban.repository;

import com.gombeth.urban.entity.Comunidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ComunidadRepository extends JpaRepository<Comunidad, Long> {

    Page<Comunidad> findByUsuarioId(Long usuarioId, Pageable pageable);

    Page<Comunidad> findByAdministradorId(Long administradorId, Pageable pageable);

    List<Comunidad> findByUsuarioIdOrderByNombreAsc(Long usuarioId);

    List<Comunidad> findByAdministradorIdOrderByNombreAsc(Long administradorId);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByIdAndAdministradorId(Long id, Long administradorId);
}