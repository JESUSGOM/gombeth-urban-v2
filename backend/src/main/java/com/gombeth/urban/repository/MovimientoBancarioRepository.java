package com.gombeth.urban.repository;

import com.gombeth.urban.entity.MovimientoBancario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MovimientoBancarioRepository
        extends JpaRepository<MovimientoBancario, Long> {

    List<MovimientoBancario>
    findByComunidadIdOrderByFechaOperacionAscIdAsc(
            Long comunidadId
    );

    List<MovimientoBancario>
    findAllByOrderByFechaOperacionAscIdAsc();

    boolean existsByComunidadIdAndFechaOperacionAndFechaValorAndImporteAndSignoAndReferenciaBancaria(
            Long comunidadId,
            LocalDate fechaOperacion,
            LocalDate fechaValor,
            BigDecimal importe,
            String signo,
            String referenciaBancaria
    );
}