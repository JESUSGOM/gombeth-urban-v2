package com.gombeth.urban.repository;

import com.gombeth.urban.entity.CuotaPresupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface CuotaPresupuestoRepository extends JpaRepository<CuotaPresupuesto, Long> {

    List<CuotaPresupuesto> findByComunidadIdAndAnioOrderByIdAsc(
            Long comunidadId,
            Integer anio
    );

    List<CuotaPresupuesto> findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
            Long comunidadId,
            Integer anio,
            String estado
    );

    List<CuotaPresupuesto> findByRevisionIdOrderByIdAsc(
            Long revisionId
    );

    @Transactional
    @Modifying
    void deleteByComunidadIdAndAnioAndEstado(
            Long comunidadId,
            Integer anio,
            String estado
    );

    @Transactional
    @Modifying
    void deleteByRevisionId(Long revisionId);

    boolean existsByComunidadIdAndAnioAndEstado(
            Long comunidadId,
            Integer anio,
            String estado
    );
}