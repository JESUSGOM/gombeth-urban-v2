package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ContabilidadRecibo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

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

    List<ContabilidadRecibo> findByComunidadIdAndEstadoAndFechaEmisionBetweenOrderByFechaEmisionAscIdAsc(
            Long comunidadId,
            String estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta
    );

    List<ContabilidadRecibo> findByComunidadIdAndEstado(
            Long comunidadId,
            String estado
    );

    List<ContabilidadRecibo> findByComunidadIdAndEstadoOrderByImporte(
            Long comunidadId,
            String estado
    );

    List<ContabilidadRecibo> findByIdIn(
            List<Long> ids
    );

    List<ContabilidadRecibo> findByComunidadIdAndEstadoAndMovimientoBancarioIdIsNotNull(
            Long comunidadId,
            String estado
    );
}