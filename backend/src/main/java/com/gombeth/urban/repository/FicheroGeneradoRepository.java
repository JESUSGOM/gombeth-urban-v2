package com.gombeth.urban.repository;

import com.gombeth.urban.entity.FicheroGenerado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FicheroGeneradoRepository
        extends JpaRepository<FicheroGenerado, Long> {

    List<FicheroGenerado> findByComunidadIdOrderByIdDesc(Long comunidadId);
}