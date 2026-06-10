package com.gombeth.urban.repository;

import com.gombeth.urban.entity.PresupuestoRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresupuestoRevisionRepository
        extends JpaRepository<PresupuestoRevision, Long> {

    List<PresupuestoRevision>
    findByComunidadIdAndAnioOrderByVersionAsc(
            Long comunidadId,
            Integer anio
    );
}