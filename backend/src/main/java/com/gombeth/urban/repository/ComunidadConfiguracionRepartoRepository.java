package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ComunidadConfiguracionReparto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComunidadConfiguracionRepartoRepository
        extends JpaRepository<ComunidadConfiguracionReparto, Long> {

    Optional<ComunidadConfiguracionReparto>
    findByComunidadId(Long comunidadId);

}