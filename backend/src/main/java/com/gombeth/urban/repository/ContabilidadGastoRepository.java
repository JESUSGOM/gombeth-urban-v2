package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ContabilidadGasto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContabilidadGastoRepository
        extends JpaRepository<ContabilidadGasto, Long> {

    List<ContabilidadGasto> findByComunidadIdOrderByFechaFacturaDescIdDesc(
            Long comunidadId
    );

    List<ContabilidadGasto> findByComunidadIdAndPagadoOrderByFechaFacturaAscIdAsc(
            Long comunidadId,
            Boolean pagado
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT g
            FROM ContabilidadGasto g
            WHERE g.id = :id
            """)
    Optional<ContabilidadGasto> findByIdForUpdate(
            @Param("id") Long id
    );
}