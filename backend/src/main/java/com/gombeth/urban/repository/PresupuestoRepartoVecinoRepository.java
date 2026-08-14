package com.gombeth.urban.repository;

import com.gombeth.urban.entity.PresupuestoRepartoVecino;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresupuestoRepartoVecinoRepository
        extends JpaRepository<PresupuestoRepartoVecino, Long> {

    List<PresupuestoRepartoVecino> findByPresupuestoIdOrderByVecinoIdAsc(
            Long presupuestoId
    );

    void deleteByPresupuestoId(Long presupuestoId);
}
