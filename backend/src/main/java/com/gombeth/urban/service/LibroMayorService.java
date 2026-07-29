package com.gombeth.urban.service;

import com.gombeth.urban.dto.LibroMayorDTO;
import com.gombeth.urban.dto.LibroMayorLineaDTO;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LibroMayorService {

    private final ContabilidadMovimientoRepository
            movimientoRepository;

    private final CuentaContableRepository
            cuentaContableRepository;

    public LibroMayorService(
            ContabilidadMovimientoRepository movimientoRepository,
            CuentaContableRepository cuentaContableRepository
    ) {
        this.movimientoRepository =
                movimientoRepository;

        this.cuentaContableRepository =
                cuentaContableRepository;
    }

    @Transactional(readOnly = true)
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

        Map<String, List<ContabilidadMovimiento>>
                movimientosPorAsiento =
                cargarMovimientosPorAsiento(
                        comunidadId,
                        movimientos
                );

        Map<Long, CuentaContable> cuentasPorId =
                cargarCuentasPorId(
                        movimientosPorAsiento
                );

        BigDecimal totalDebe =
                BigDecimal.ZERO;

        BigDecimal totalHaber =
                BigDecimal.ZERO;

        BigDecimal saldo =
                BigDecimal.ZERO;

        List<LibroMayorLineaDTO> lineas =
                new ArrayList<>();

        for (
                ContabilidadMovimiento movimiento
                : movimientos
        ) {
            BigDecimal debe =
                    importeSeguro(
                            movimiento.getDebe()
                    );

            BigDecimal haber =
                    importeSeguro(
                            movimiento.getHaber()
                    );

            totalDebe =
                    totalDebe.add(debe);

            totalHaber =
                    totalHaber.add(haber);

            saldo =
                    saldo
                            .add(debe)
                            .subtract(haber);

            List<LibroMayorLineaDTO.ContrapartidaDTO>
                    contrapartidas =
                    obtenerContrapartidas(
                            movimiento,
                            movimientosPorAsiento,
                            cuentasPorId
                    );

            lineas.add(
                    new LibroMayorLineaDTO(
                            movimiento.getId(),
                            movimiento.getFecha(),
                            movimiento.getConcepto(),
                            movimiento.getNumeroAsiento(),
                            debe,
                            haber,
                            saldo,
                            contrapartidas
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

    private List<ContabilidadMovimiento>
    obtenerMovimientos(
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
                LocalDate.of(
                        ejercicio,
                        1,
                        1
                );

        LocalDate fechaHasta =
                LocalDate.of(
                        ejercicio,
                        12,
                        31
                );

        return movimientoRepository
                .findByComunidadIdAndCuentaIdAndFechaBetweenOrderByFechaAscIdAsc(
                        comunidadId,
                        cuentaId,
                        fechaDesde,
                        fechaHasta
                );
    }

    private Map<String, List<ContabilidadMovimiento>>
    cargarMovimientosPorAsiento(
            Long comunidadId,
            List<ContabilidadMovimiento> movimientos
    ) {
        Map<String, List<ContabilidadMovimiento>>
                resultado =
                new HashMap<>();

        for (
                ContabilidadMovimiento movimiento
                : movimientos
        ) {
            String numeroAsiento =
                    movimiento.getNumeroAsiento();

            if (
                    numeroAsiento == null
                            || numeroAsiento.isBlank()
            ) {
                continue;
            }

            resultado.computeIfAbsent(
                    numeroAsiento,
                    clave ->
                            movimientoRepository
                                    .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                            comunidadId,
                                            clave
                                    )
            );
        }

        return resultado;
    }

    private Map<Long, CuentaContable>
    cargarCuentasPorId(
            Map<String, List<ContabilidadMovimiento>>
                    movimientosPorAsiento
    ) {
        List<Long> cuentaIds =
                movimientosPorAsiento
                        .values()
                        .stream()
                        .flatMap(List::stream)
                        .map(
                                ContabilidadMovimiento
                                        ::getCuentaId
                        )
                        .filter(
                                cuentaId ->
                                        cuentaId != null
                        )
                        .distinct()
                        .toList();

        if (cuentaIds.isEmpty()) {
            return Map.of();
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

    private List<LibroMayorLineaDTO.ContrapartidaDTO>
    obtenerContrapartidas(
            ContabilidadMovimiento movimiento,
            Map<String, List<ContabilidadMovimiento>>
                    movimientosPorAsiento,
            Map<Long, CuentaContable> cuentasPorId
    ) {
        String numeroAsiento =
                movimiento.getNumeroAsiento();

        if (
                numeroAsiento == null
                        || numeroAsiento.isBlank()
        ) {
            return List.of();
        }

        List<ContabilidadMovimiento>
                movimientosAsiento =
                movimientosPorAsiento.getOrDefault(
                        numeroAsiento,
                        List.of()
                );

        return movimientosAsiento
                .stream()
                .filter(linea ->
                        !Objects.equals(
                                linea.getCuentaId(),
                                movimiento.getCuentaId()
                        )
                )
                .map(linea ->
                        convertirContrapartida(
                                linea,
                                cuentasPorId
                        )
                )
                .toList();
    }

    private LibroMayorLineaDTO.ContrapartidaDTO
    convertirContrapartida(
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

        return new LibroMayorLineaDTO
                .ContrapartidaDTO(
                movimiento.getCuentaId(),
                codigoCuenta,
                nombreCuenta,
                importeSeguro(
                        movimiento.getDebe()
                ),
                importeSeguro(
                        movimiento.getHaber()
                )
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