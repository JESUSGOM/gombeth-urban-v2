package com.gombeth.urban.repository;

import com.gombeth.urban.entity.Comunidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComunidadRepository
        extends JpaRepository<Comunidad, Long> {

    Page<Comunidad> findByUsuarioId(
            Long usuarioId,
            Pageable pageable
    );

    Page<Comunidad> findByAdministradorId(
            Long administradorId,
            Pageable pageable
    );

    List<Comunidad> findByUsuarioIdOrderByNombreAsc(
            Long usuarioId
    );

    List<Comunidad> findByAdministradorIdOrderByNombreAsc(
            Long administradorId
    );

    boolean existsByIdAndUsuarioId(
            Long id,
            Long usuarioId
    );

    boolean existsByIdAndAdministradorId(
            Long id,
            Long administradorId
    );

    /**
     * Devuelve únicamente las comunidades asignadas al usuario:
     *
     * 1. Comunidades cuyo campo usuario_id coincide.
     * 2. Comunidades compartidas mediante usuario_comunidades.
     *
     * El usuario se obtiene de la sesión, nunca de la URL.
     */
    @Query(
            value = """
                    SELECT c
                    FROM Comunidad c
                    WHERE c.usuarioId = :usuarioId
                       OR c.id IN (
                            SELECT uc.comunidadId
                            FROM UsuarioComunidad uc
                            WHERE uc.usuarioId = :usuarioId
                       )
                    """,
            countQuery = """
                    SELECT COUNT(c)
                    FROM Comunidad c
                    WHERE c.usuarioId = :usuarioId
                       OR c.id IN (
                            SELECT uc.comunidadId
                            FROM UsuarioComunidad uc
                            WHERE uc.usuarioId = :usuarioId
                       )
                    """
    )
    Page<Comunidad> findAccesiblesPorUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT c
            FROM Comunidad c
            WHERE c.usuarioId = :usuarioId
               OR c.id IN (
                    SELECT uc.comunidadId
                    FROM UsuarioComunidad uc
                    WHERE uc.usuarioId = :usuarioId
               )
            ORDER BY c.nombre ASC
            """)
    List<Comunidad> findAccesiblesPorUsuarioOrdenadas(
            @Param("usuarioId") Long usuarioId
    );
}