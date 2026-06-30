package com.gombeth.urban.repository;

import com.gombeth.urban.dto.BalanceSumaSaldoDTO;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContabilidadMovimientoRepository
        extends JpaRepository<ContabilidadMovimiento, Long> {

    List<ContabilidadMovimiento> findByComunidadIdOrderByFechaAscIdAsc(
            Long comunidadId
    );

    boolean existsByComunidadIdAndNumeroAsiento(
            Long comunidadId,
            String numeroAsiento
    );

    List<ContabilidadMovimiento> findByComunidadIdAndNumeroAsientoOrderByIdAsc(
            Long comunidadId,
            String numeroAsiento
    );

    List<ContabilidadMovimiento> findByComunidadIdAndCuentaIdOrderByFechaAscIdAsc(
            Long comunidadId,
            Long cuentaId
    );

    @Query("""
    SELECT new com.gombeth.urban.dto.BalanceSumaSaldoDTO(
        c.id,
        c.codigo,
        c.nombre,
        COALESCE(SUM(m.debe), 0),
        COALESCE(SUM(m.haber), 0),
        COALESCE(SUM(m.debe), 0) - COALESCE(SUM(m.haber), 0)
    )
    FROM ContabilidadMovimiento m, CuentaContable c
    WHERE m.cuentaId = c.id
      AND m.comunidadId = :comunidadId
    GROUP BY c.id, c.codigo, c.nombre
    ORDER BY c.codigo
""")
    List<BalanceSumaSaldoDTO> obtenerBalance(
            @Param("comunidadId") Long comunidadId
    );
}