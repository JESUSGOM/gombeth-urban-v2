package com.gombeth.urban.service;

import com.gombeth.urban.dto.LibroMayorDTO;
import com.gombeth.urban.dto.LibroMayorLineaDTO;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LibroMayorService {

    private final ContabilidadMovimientoRepository movimientoRepository;

    public LibroMayorService(
            ContabilidadMovimientoRepository movimientoRepository
    ) {
        this.movimientoRepository = movimientoRepository;
    }

    public LibroMayorDTO obtenerMayor(
            Long comunidadId,
            Long cuentaId,
            Integer ejercicio
    ) {
        List<ContabilidadMovimiento> movimientos =
                obtenerMovimientos(
                        comunidadId,
                        cuentaId,
                        ejercicio
                );

        BigDecimal totalDebe = BigDecimal.ZERO;
        BigDecimal totalHaber = BigDecimal.ZERO;
        BigDecimal saldo = BigDecimal.ZERO;

        List<LibroMayorLineaDTO> lineas = new ArrayList<>();

        for (ContabilidadMovimiento movimiento : movimientos) {

            BigDecimal debe = movimiento.getDebe() != null
                    ? movimiento.getDebe()
                    : BigDecimal.ZERO;

            BigDecimal haber = movimiento.getHaber() != null
                    ? movimiento.getHaber()
                    : BigDecimal.ZERO;

            totalDebe = totalDebe.add(debe);
            totalHaber = totalHaber.add(haber);

            saldo = saldo.add(debe).subtract(haber);

            lineas.add(
                    new LibroMayorLineaDTO(
                            movimiento.getId(),
                            movimiento.getFecha(),
                            movimiento.getConcepto(),
                            movimiento.getNumeroAsiento(),
                            debe,
                            haber,
                            saldo
                    )
            );
        }

        return new LibroMayorDTO(
                comunidadId,
                cuentaId,
                totalDebe,
                totalHaber,
                saldo,
                lineas
        );
    }

    private List<ContabilidadMovimiento> obtenerMovimientos(
            Long comunidadId,
            Long cuentaId,
            Integer ejercicio
    ) {
        if (ejercicio == null) {
            return movimientoRepository
                    .findByComunidadIdAndCuentaIdOrderByFechaAscIdAsc(
                            comunidadId,
                            cuentaId
                    );
        }

        LocalDate fechaDesde =
                LocalDate.of(ejercicio, 1, 1);

        LocalDate fechaHasta =
                LocalDate.of(ejercicio, 12, 31);

        return movimientoRepository
                .findByComunidadIdAndCuentaIdAndFechaBetweenOrderByFechaAscIdAsc(
                        comunidadId,
                        cuentaId,
                        fechaDesde,
                        fechaHasta
                );
    }
}
