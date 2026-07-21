package com.gombeth.urban.repository;

import com.gombeth.urban.entity.RemesaLinea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RemesaLineaRepository
        extends JpaRepository<RemesaLinea, Long> {

    boolean existsByReciboContableId(Long reciboContableId);

    List<RemesaLinea> findByRemesaIdOrderByIdAsc(Long remesaId);

    @Modifying
    @Transactional
    void deleteByRemesaId(Long remesaId);

    List<RemesaLinea> findByReciboContableId(Long reciboContableId);
}