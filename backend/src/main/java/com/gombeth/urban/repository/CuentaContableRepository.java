package com.gombeth.urban.repository;

import com.gombeth.urban.entity.CuentaContable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CuentaContableRepository extends JpaRepository<CuentaContable, Long> {

    @Query("SELECT c FROM CuentaContable c WHERE c.comunidad.id = :comunidadId")
    List<CuentaContable> findByComunidadId(Long comunidadId);
}