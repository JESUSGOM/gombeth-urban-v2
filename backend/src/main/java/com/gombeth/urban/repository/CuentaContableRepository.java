package com.gombeth.urban.repository;

import com.gombeth.urban.entity.CuentaContable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CuentaContableRepository
        extends JpaRepository<CuentaContable, Long> {

    @Query("""
            SELECT c
            FROM CuentaContable c
            WHERE c.comunidad.id = :comunidadId
            """)
    List<CuentaContable> findByComunidadId(
            Long comunidadId
    );

    Optional<CuentaContable>
    findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
            Long comunidadId,
            String codigo
    );

    Optional<CuentaContable> findByIdAndComunidad_Id(
            Long id,
            Long comunidadId
    );
}