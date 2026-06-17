package com.gombeth.urban.repository;

import com.gombeth.urban.entity.FicheroGenerado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FicheroGeneradoRepository
        extends JpaRepository<FicheroGenerado, Long> {
}