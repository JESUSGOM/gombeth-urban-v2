package com.gombeth.urban.repository;

import com.gombeth.urban.entity.ContabilidadReciboConcepto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContabilidadReciboConceptoRepository
        extends JpaRepository<ContabilidadReciboConcepto, Long> {

    List<ContabilidadReciboConcepto> findByReciboIdOrderByOrdenAsc(
            Long reciboId
    );

    List<ContabilidadReciboConcepto> findByReciboIdInOrderByReciboIdAscOrdenAsc(
            List<Long> reciboIds
    );

    void deleteByReciboId(Long reciboId);
}