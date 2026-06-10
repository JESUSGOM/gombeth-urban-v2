package com.gombeth.urban.repository;

import com.gombeth.urban.entity.Vecino;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.gombeth.urban.dto.CoeficienteVecinoDetalleResponse;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.math.BigDecimal;

public interface VecinoRepository extends JpaRepository<Vecino, Long> {

    Page<Vecino> findByComunidadIdIn(
            Iterable<Long> comunidadIds,
            Pageable pageable
    );

    Page<Vecino> findByComunidadId(
            Long comunidadId,
            Pageable pageable
    );

    Page<Vecino> findByComunidadIdAndActivo(
            Long comunidadId,
            boolean activo,
            Pageable pageable
    );

    @Query("""
       SELECT COALESCE(SUM(v.coeficiente), 0)
       FROM Vecino v
       WHERE v.comunidadId = :comunidadId
         AND v.activo = true
       """)
    BigDecimal sumarCoeficientesActivosPorComunidad(
            @Param("comunidadId") Long comunidadId
    );

    long countByComunidadIdAndActivo(
            Long comunidadId,
            boolean activo
    );

    @Query("""
       SELECT new com.gombeth.urban.dto.CoeficienteVecinoDetalleResponse(
           v.id,
           v.nombre,
           v.vivienda,
           v.coeficiente,
           v.activo
       )
       FROM Vecino v
       WHERE v.comunidadId = :comunidadId
       ORDER BY v.vivienda ASC, v.nombre ASC
       """)
    List<CoeficienteVecinoDetalleResponse> detalleCoeficientesPorComunidad(
            @Param("comunidadId") Long comunidadId
    );

    List<Vecino> findByComunidadIdAndActivoTrueOrderByViviendaAscNombreAsc(
            Long comunidadId
    );
}