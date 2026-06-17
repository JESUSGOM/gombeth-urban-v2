package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ContabilidadRecibo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContabilidadReciboRepository extends JpaRepository<ContabilidadRecibo, Long> {

    boolean existsByCuotaPresupuestoId(Long cuotaPresupuestoId);

    List<ContabilidadRecibo> findByComunidadIdOrderByFechaEmisionDescIdDesc(
            Long comunidadId
    );

    List<ContabilidadRecibo> findByComunidadIdAndEstadoOrderByFechaEmisionAscIdAsc(
            Long comunidadId,
            String estado
    );
}