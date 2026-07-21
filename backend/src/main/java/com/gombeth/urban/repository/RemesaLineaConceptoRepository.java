package com.gombeth.urban.repository;

import com.gombeth.urban.entity.RemesaLineaConcepto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RemesaLineaConceptoRepository
        extends JpaRepository<RemesaLineaConcepto, Long> {

    List<RemesaLineaConcepto> findByRemesaLineaIdOrderByOrdenAsc(
            Long remesaLineaId
    );

    List<RemesaLineaConcepto> findByRemesaLineaIdInOrderByRemesaLineaIdAscOrdenAsc(
            List<Long> remesaLineaIds
    );

    void deleteByRemesaLineaId(Long remesaLineaId);

    @Modifying
    @Transactional
    void deleteByRemesaLineaIdIn(
            List<Long> remesaLineaIds
    );
}