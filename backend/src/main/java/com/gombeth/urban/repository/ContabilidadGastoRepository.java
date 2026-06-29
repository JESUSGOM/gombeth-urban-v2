package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ContabilidadGasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContabilidadGastoRepository
        extends JpaRepository<ContabilidadGasto, Long> {

    List<ContabilidadGasto> findByComunidadIdOrderByFechaFacturaDescIdDesc(
            Long comunidadId
    );

    List<ContabilidadGasto> findByComunidadIdAndPagadoOrderByFechaFacturaAscIdAsc(
            Long comunidadId,
            Boolean pagado
    );
}