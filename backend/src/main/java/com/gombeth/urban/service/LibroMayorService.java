package com.gombeth.urban.service;

import com.gombeth.urban.dto.LibroMayorDTO;
import com.gombeth.urban.dto.LibroMayorLineaDTO;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
            Long cuentaId
    ) {
        List<ContabilidadMovimiento> movimientos =
                movimientoRepository.findByComunidadIdAndCuentaIdOrderByFechaAscIdAsc(
                        comunidadId,
                        cuentaId
                );

        BigDecimal totalDebe = BigDecimal.ZERO;
        BigDecimal totalHaber = BigDecimal.ZERO;
        BigDecimal saldo = BigDecimal.ZERO;

        List<LibroMayorLineaDTO> lineas = new ArrayList<>();

        for (ContabilidadMovimiento m : movimientos) {

            BigDecimal debe = m.getDebe() != null
                    ? m.getDebe()
                    : BigDecimal.ZERO;

            BigDecimal haber = m.getHaber() != null
                    ? m.getHaber()
                    : BigDecimal.ZERO;

            totalDebe = totalDebe.add(debe);
            totalHaber = totalHaber.add(haber);

            saldo = saldo.add(debe).subtract(haber);

            lineas.add(
                    new LibroMayorLineaDTO(
                            m.getId(),
                            m.getFecha(),
                            m.getConcepto(),
                            m.getNumeroAsiento(),
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
}