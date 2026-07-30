package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ContabilidadRecibo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    List<ContabilidadRecibo>
    findByComunidadIdAndMovimientoBancarioIdOrderByIdAsc(
            Long comunidadId,
            Long movimientoBancarioId
    );

    List<ContabilidadRecibo> findByComunidadIdAndEstadoAndMovimientoBancarioIdIsNotNull(
            Long comunidadId,
            String estado
    );

    @Modifying
    @Transactional
    @Query("""
    DELETE FROM ContabilidadRecibo r
    WHERE r.comunidadId = :comunidadId
      AND MONTH(r.fechaEmision) = :mes
      AND YEAR(r.fechaEmision) = :anio
      AND r.estado = 'PENDIENTE'
""")
    void deletePendientesMes(
            @Param("comunidadId") Long comunidadId,
            @Param("mes") int mes,
            @Param("anio") int anio
    );


    Optional<ContabilidadRecibo> findByCuotaPresupuestoId(
            Long cuotaPresupuestoId
    );

    List<ContabilidadRecibo> findByComunidadIdAndFechaEmisionBetweenOrderByFechaEmisionAscIdAsc(
            Long comunidadId,
            LocalDate fechaDesde,
            LocalDate fechaHasta
    );

    boolean existsByCuotaPresupuestoIdAndFechaEmision(
            Long cuotaPresupuestoId,
            LocalDate fechaEmision
    );

    List<ContabilidadRecibo> findByComunidadIdAndEstadoAndFechaEmisionOrderByIdAsc(
            Long comunidadId,
            String estado,
            LocalDate fechaEmision
    );

    List<ContabilidadRecibo> findByComunidadIdAndFechaEmisionOrderByIdAsc(
            Long comunidadId,
            LocalDate fechaEmision
    );
}