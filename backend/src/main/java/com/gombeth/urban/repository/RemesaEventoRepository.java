package com.gombeth.urban.repository;

import com.gombeth.urban.entity.RemesaEvento;
import com.gombeth.urban.entity.RemesaEventoTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemesaEventoRepository
        extends JpaRepository<RemesaEvento, Long> {

    List<RemesaEvento>
    findByRemesaIdOrderByFechaEventoAscIdAsc(
            Long remesaId
    );

    boolean existsByRemesaIdAndTipoEvento(
            Long remesaId,
            RemesaEventoTipo tipoEvento
    );
}