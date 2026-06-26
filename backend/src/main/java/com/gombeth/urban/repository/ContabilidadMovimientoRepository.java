package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ContabilidadMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

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
}