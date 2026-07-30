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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DiarioContableService {

    private static final String PREFIJO_ASIENTO =
            "ASIENTO-";

    private static final String ORIGEN_RECIBO_COBRADO =
            "RECIBO_COBRADO";

    private static final String ORIGEN_RECIBO_EMITIDO =
            "RECIBO_EMITIDO";

    private static final String ORIGEN_GASTO_CONTABILIZADO =
            "GASTO_CONTABILIZADO";

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

        List<ContabilidadMovimiento> movimientos =
                obtenerMovimientosAsiento(
                        asiento
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

    private List<ContabilidadMovimiento>
    obtenerMovimientosAsiento(
            ContabilidadAsiento asiento
    ) {
        List<String> referencias =
                construirReferenciasAsiento(
                        asiento
                );

        for (String referencia : referencias) {
            List<ContabilidadMovimiento> movimientos =
                    movimientoRepository
                            .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                    asiento.getComunidadId(),
                                    referencia
                            );

            if (!movimientos.isEmpty()) {
                return movimientos;
            }
        }

        return List.of();
    }

    private List<String> construirReferenciasAsiento(
            ContabilidadAsiento asiento
    ) {
        List<String> referencias =
                new ArrayList<>();

        if (asiento.getId() != null) {
            referencias.add(
                    PREFIJO_ASIENTO
                            + asiento.getId()
            );
        }

        String referenciaHistorica =
                obtenerReferenciaHistorica(
                        asiento
                );

        if (
                referenciaHistorica != null
                        && !referencias.contains(
                        referenciaHistorica
                )
        ) {
            referencias.add(
                    referenciaHistorica
            );
        }

        if (asiento.getNumeroAsiento() != null) {
            String referenciaNumerica =
                    String.valueOf(
                            asiento.getNumeroAsiento()
                    );

            if (
                    !referencias.contains(
                            referenciaNumerica
                    )
            ) {
                referencias.add(
                        referenciaNumerica
                );
            }
        }

        return referencias;
    }

    private String obtenerReferenciaHistorica(
            ContabilidadAsiento asiento
    ) {
        if (asiento.getOrigenId() == null) {
            return null;
        }

        if (
                ORIGEN_GASTO_CONTABILIZADO.equals(
                        asiento.getOrigen()
                )
        ) {
            return "GASTO-"
                    + asiento.getOrigenId();
        }

        if (
                ORIGEN_RECIBO_EMITIDO.equals(
                        asiento.getOrigen()
                )
        ) {
            return "DEVENGO-RECIBO-"
                    + asiento.getOrigenId();
        }

        if (
                ORIGEN_RECIBO_COBRADO.equals(
                        asiento.getOrigen()
                )
        ) {
            return "COBRO-RECIBO-"
                    + asiento.getOrigenId();
        }

        return null;
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