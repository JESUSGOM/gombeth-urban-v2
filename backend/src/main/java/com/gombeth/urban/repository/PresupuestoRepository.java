package com.gombeth.urban.repository;

import com.gombeth.urban.entity.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresupuestoRepository
        extends JpaRepository<Presupuesto, Long> {

    List<Presupuesto> findByComunidadIdOrderByCuentaCodigoAsc(
            Long comunidadId
    );

    List<Presupuesto> findByComunidadIdAndAnioOrderByCuentaCodigoAsc(
            Long comunidadId,
            int anio
    );

    boolean existsByComunidad_IdAndCuenta_IdAndAnio(
            Long comunidadId,
            Long cuentaId,
            int anio
    );
}