package com.gombeth.urban.repository;

import com.gombeth.urban.entity.MovimientoBancario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoBancarioRepository
        extends JpaRepository<MovimientoBancario, Long> {

    List<MovimientoBancario>
    findByComunidadIdOrderByFechaOperacionAscIdAsc(
            Long comunidadId
    );

    List<MovimientoBancario>
    findAllByOrderByFechaOperacionAscIdAsc();
}