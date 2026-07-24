package com.gombeth.urban.repository;

import com.gombeth.urban.entity.CuentaPresentador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaPresentadorRepository
        extends JpaRepository<CuentaPresentador, Long> {

    List<CuentaPresentador>
    findByAdministradorIdOrderByAliasAsc(
            Long administradorId
    );

    List<CuentaPresentador>
    findByAdministradorIdAndActivaTrueOrderByAliasAsc(
            Long administradorId
    );

    Optional<CuentaPresentador>
    findByIdAndAdministradorId(
            Long id,
            Long administradorId
    );
}