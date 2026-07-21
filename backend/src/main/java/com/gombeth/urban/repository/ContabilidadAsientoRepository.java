package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ContabilidadAsiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ContabilidadAsientoRepository
        extends JpaRepository<ContabilidadAsiento, Long> {

    List<ContabilidadAsiento> findByComunidadIdAndEjercicioOrderByNumeroAsientoAsc(
            Long comunidadId,
            Integer ejercicio
    );

    Optional<ContabilidadAsiento> findTopByComunidadIdAndEjercicioOrderByNumeroAsientoDesc(
            Long comunidadId,
            Integer ejercicio
    );

    boolean existsByComunidadIdAndEjercicioAndNumeroAsiento(
            Long comunidadId,
            Integer ejercicio,
            Long numeroAsiento
    );

    Optional<ContabilidadAsiento> findByComunidadIdAndOrigenAndOrigenId(
            Long comunidadId,
            String origen,
            Long origenId
    );

    @Modifying
    @Transactional
    void deleteByComunidadIdAndOrigenAndOrigenId(
            Long comunidadId,
            String origen,
            Long origenId
    );
}