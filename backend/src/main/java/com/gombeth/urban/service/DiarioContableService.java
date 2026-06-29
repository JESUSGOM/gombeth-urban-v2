package com.gombeth.urban.service;

import com.gombeth.urban.dto.DiarioContableDTO;
import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DiarioContableService {

    private final ContabilidadAsientoRepository asientoRepository;
    private final ContabilidadMovimientoRepository movimientoRepository;

    public DiarioContableService(
            ContabilidadAsientoRepository asientoRepository,
            ContabilidadMovimientoRepository movimientoRepository
    ) {
        this.asientoRepository = asientoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public List<ContabilidadAsiento> listar(
            Long comunidadId,
            Integer ejercicio
    ) {
        return asientoRepository
                .findByComunidadIdAndEjercicioOrderByNumeroAsientoAsc(
                        comunidadId,
                        ejercicio
                );
    }

    public DiarioContableDTO detalle(Long asientoId) {

        ContabilidadAsiento asiento = asientoRepository.findById(asientoId)
                .orElseThrow();

        List<ContabilidadMovimiento> movimientos =
                movimientoRepository.findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                        asiento.getComunidadId(),
                        asiento.getOrigen().equals("GASTO_CONTABILIZADO")
                                ? "GASTO-" + asiento.getOrigenId()
                                : "COBRO-RECIBO-" + asiento.getOrigenId()
                );

        BigDecimal debe = movimientos.stream()
                .map(ContabilidadMovimiento::getDebe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal haber = movimientos.stream()
                .map(ContabilidadMovimiento::getHaber)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DiarioContableDTO(
                asiento,
                movimientos,
                debe,
                haber
        );
    }
}