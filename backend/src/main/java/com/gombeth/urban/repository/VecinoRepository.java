package com.gombeth.urban.repository;

import com.gombeth.urban.entity.Vecino;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VecinoRepository extends JpaRepository<Vecino, Long> {

    Page<Vecino> findByComunidadIdIn(
            Iterable<Long> comunidadIds,
            Pageable pageable
    );

    Page<Vecino> findByComunidadId(Long comunidadId, Pageable pageable);
}