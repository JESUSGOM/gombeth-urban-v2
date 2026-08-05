package com.gombeth.urban.repository;

import com.gombeth.urban.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository
        extends JpaRepository<Administrador, Long> {
}