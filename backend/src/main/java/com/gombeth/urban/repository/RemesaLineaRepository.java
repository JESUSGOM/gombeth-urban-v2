package com.gombeth.urban.repository;

import com.gombeth.urban.entity.RemesaLinea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;

public interface RemesaLineaRepository
        extends JpaRepository<RemesaLinea, Long> {

    boolean existsByReciboContableId(Long reciboContableId);

    List<RemesaLinea> findByRemesaIdOrderByIdAsc(Long remesaId);

    @Modifying
    @Transactional
    void deleteByRemesaId(Long remesaId);

    List<RemesaLinea> findByReciboContableId(Long reciboContableId);

    @Query(
            value = """
                SELECT rl.remesa_id
                FROM remesa_lineas rl
                INNER JOIN ficheros_generados fg
                        ON fg.id = rl.remesa_id
                WHERE rl.recibo_contable_id = :reciboContableId
                  AND UPPER(COALESCE(fg.estado, 'GENERADA')) <> 'ANULADA'
                ORDER BY rl.remesa_id DESC
                LIMIT 1
                """,
            nativeQuery = true
    )
    Optional<Long> findRemesaActivaIdByReciboContableId(
            @Param("reciboContableId") Long reciboContableId
    );
}