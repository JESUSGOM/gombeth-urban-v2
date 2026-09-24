package com.gombeth.urban.repository;

import com.gombeth.urban.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository
        extends JpaRepository<Proveedor, Long> {

    List<Proveedor>
    findByAdministradorIdAndActivoTrueOrderByNombreAsc(
            Long administradorId
    );

    Optional<Proveedor>
    findByAdministradorIdAndNifCifIgnoreCase(
            Long administradorId,
            String nifCif
    );
}