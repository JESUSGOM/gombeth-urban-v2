package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ProveedorComunidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorComunidadRepository
        extends JpaRepository<ProveedorComunidad, Long> {

    List<ProveedorComunidad>
    findByComunidadIdAndActivoTrueOrderByIdAsc(
            Long comunidadId
    );

    Optional<ProveedorComunidad>
    findByProveedorIdAndComunidadId(
            Long proveedorId,
            Long comunidadId
    );

    Optional<ProveedorComunidad>
    findByIdAndComunidadId(
            Long id,
            Long comunidadId
    );

    boolean existsByComunidadIdAndCuentaContableId(
            Long comunidadId,
            Long cuentaContableId
    );
}