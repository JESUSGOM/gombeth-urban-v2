package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ConceptoCobro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ConceptoCobroRepository
        extends JpaRepository<ConceptoCobro, Long> {

    List<ConceptoCobro> findByComunidadIdOrderByDescripcionAsc(
            Long comunidadId
    );

    List<ConceptoCobro> findByComunidadIdAndActivoTrueOrderByDescripcionAsc(
            Long comunidadId
    );

    List<ConceptoCobro> findByComunidadIdAndVecinoIdIsNullOrderByDescripcionAsc(
            Long comunidadId
    );

    List<ConceptoCobro> findByComunidadIdAndVecinoIdOrderByDescripcionAsc(
            Long comunidadId,
            Long vecinoId
    );

    List<ConceptoCobro> findByVecinoIdAndActivoTrueOrderByDescripcionAsc(
            Long vecinoId
    );

    List<ConceptoCobro> findByComunidadId(Long comunidadId);

    List<ConceptoCobro> findByComunidadIdAndVecinoIdAndActivoTrueOrderByDescripcionAsc(
            Long comunidadId,
            Long vecinoId
    );
}