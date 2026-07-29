package com.gombeth.urban.service;

import com.gombeth.urban.dto.DiarioContableDTO;
import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DiarioContableService {

    private final ContabilidadAsientoRepository
            asientoRepository;

    private final ContabilidadMovimientoRepository
            movimientoRepository;

    private final CuentaContableRepository
            cuentaContableRepository;

    public DiarioContableService(
            ContabilidadAsientoRepository asientoRepository,
            ContabilidadMovimientoRepository movimientoRepository,
            CuentaContableRepository cuentaContableRepository
    ) {
        this.asientoRepository =
                asientoRepository;

        this.movimientoRepository =
                movimientoRepository;

        this.cuentaContableRepository =
                cuentaContableRepository;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public DiarioContableDTO detalle(
            Long asientoId
    ) {
        ContabilidadAsiento asiento =
                asientoRepository
                        .findById(asientoId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No existe el asiento contable "
                                                + asientoId
                                )
                        );

        String numeroAsientoControl =
                obtenerNumeroAsientoControl(asiento);

        List<ContabilidadMovimiento> movimientos =
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                asiento.getComunidadId(),
                                numeroAsientoControl
                        );

        Map<Long, CuentaContable> cuentasPorId =
                obtenerCuentasPorId(movimientos);

        List<DiarioContableDTO.MovimientoDiarioDTO>
                movimientosDetalle =
                movimientos.stream()
                        .map(movimiento ->
                                convertirMovimiento(
                                        movimiento,
                                        cuentasPorId
                                )
                        )
                        .toList();

        BigDecimal totalDebe =
                movimientosDetalle.stream()
                        .map(
                                DiarioContableDTO
                                        .MovimientoDiarioDTO
                                        ::getDebe
                        )
                        .map(this::importeSeguro)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalHaber =
                movimientosDetalle.stream()
                        .map(
                                DiarioContableDTO
                                        .MovimientoDiarioDTO
                                        ::getHaber
                        )
                        .map(this::importeSeguro)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return new DiarioContableDTO(
                asiento,
                movimientosDetalle,
                totalDebe,
                totalHaber
        );
    }

    private String obtenerNumeroAsientoControl(
            ContabilidadAsiento asiento
    ) {
        if (
                "GASTO_CONTABILIZADO".equals(
                        asiento.getOrigen()
                )
        ) {
            return "GASTO-"
                    + asiento.getOrigenId();
        }

        return "COBRO-RECIBO-"
                + asiento.getOrigenId();
    }

    private Map<Long, CuentaContable> obtenerCuentasPorId(
            List<ContabilidadMovimiento> movimientos
    ) {
        List<Long> cuentaIds =
                movimientos.stream()
                        .map(
                                ContabilidadMovimiento::getCuentaId
                        )
                        .filter(id -> id != null)
                        .distinct()
                        .toList();

        if (cuentaIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return cuentaContableRepository
                .findAllById(cuentaIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                CuentaContable::getId,
                                Function.identity()
                        )
                );
    }

    private DiarioContableDTO.MovimientoDiarioDTO
    convertirMovimiento(
            ContabilidadMovimiento movimiento,
            Map<Long, CuentaContable> cuentasPorId
    ) {
        CuentaContable cuenta =
                cuentasPorId.get(
                        movimiento.getCuentaId()
                );

        String codigoCuenta =
                cuenta != null
                        ? cuenta.getCodigo()
                        : null;

        String nombreCuenta =
                cuenta != null
                        ? cuenta.getNombre()
                        : null;

        return new DiarioContableDTO
                .MovimientoDiarioDTO(
                movimiento.getId(),
                movimiento.getConcepto(),
                importeSeguro(
                        movimiento.getDebe()
                ),
                importeSeguro(
                        movimiento.getHaber()
                ),
                movimiento.getFecha(),
                movimiento.getNumeroAsiento(),
                movimiento.getComunidadId(),
                movimiento.getCuentaId(),
                codigoCuenta,
                nombreCuenta
        );
    }

    private BigDecimal importeSeguro(
            BigDecimal importe
    ) {
        return importe != null
                ? importe
                : BigDecimal.ZERO;
    }
}