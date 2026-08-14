package com.gombeth.urban.repository;

import com.gombeth.urban.entity.PresupuestoRepartoConfiguracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PresupuestoRepartoConfiguracionRepository
        extends JpaRepository<PresupuestoRepartoConfiguracion, Long> {

    Optional<PresupuestoRepartoConfiguracion> findByPresupuestoId(
            Long presupuestoId
    );
}
