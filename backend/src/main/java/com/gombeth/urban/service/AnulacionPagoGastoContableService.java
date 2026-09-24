package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnulacionPagoGastoContableService {

    private static final String ORIGEN_PAGO_GASTO =
            "GASTO_PAGADO";

    private static final String ORIGEN_ANULACION =
            "ANULACION_GASTO_PAGADO";

    private static final String ESTADO_ANULADO =
            "ANULADO";

    private final ContabilidadAsientoRepository
            asientoRepository;

    private final ContabilidadMovimientoRepository
            movimientoRepository;

    private final ContabilidadAsientoService
            asientoService;

    public AnulacionPagoGastoContableService(
            ContabilidadAsientoRepository asientoRepository,
            ContabilidadMovimientoRepository movimientoRepository,
            ContabilidadAsientoService asientoService
    ) {
        this.asientoRepository =
                asientoRepository;

        this.movimientoRepository =
                movimientoRepository;

        this.asientoService =
                asientoService;
    }

    @Transactional
    public ContabilidadAsiento anularPago(
            ContabilidadGasto gasto,
            Long usuarioId,
            LocalDate fechaAnulacion
    ) {
        validarGasto(
                gasto
        );

        ContabilidadAsiento asientoOriginal =
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                gasto.getComunidadId(),
                                ORIGEN_PAGO_GASTO,
                                gasto.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el asiento contable "
                                                + "del pago del gasto "
                                                + gasto.getId()
                                                + "."
                                )
                        );

        if (
                ESTADO_ANULADO.equals(
                        asientoOriginal.getEstado()
                )
        ) {
            throw new IllegalStateException(
                    "El asiento del pago del gasto "
                            + gasto.getId()
                            + " ya está anulado."
            );
        }

        if (asientoOriginal.getId() == null) {
            throw new IllegalStateException(
                    "El asiento del pago del gasto "
                            + gasto.getId()
                            + " no tiene identificador."
            );
        }

        String referenciaOriginal =
                "ASIENTO-"
                        + asientoOriginal.getId();

        List<ContabilidadMovimiento> movimientosOriginales =
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                gasto.getComunidadId(),
                                referenciaOriginal
                        );

        if (movimientosOriginales.isEmpty()) {
            throw new IllegalStateException(
                    "El asiento del pago del gasto "
                            + gasto.getId()
                            + " no tiene movimientos contables."
            );
        }

        LocalDate fecha =
                fechaAnulacion != null
                        ? fechaAnulacion
                        : LocalDate.now();

        ContabilidadAsiento asientoAnulacion =
                asientoService.crearAsientoAutomatico(
                        gasto.getComunidadId(),
                        fecha,
                        "Anulación pago gasto "
                                + gasto.getId(),
                        ORIGEN_ANULACION,
                        asientoOriginal.getId(),
                        usuarioId
                );

        if (
                asientoAnulacion == null
                        || asientoAnulacion.getId() == null
        ) {
            throw new IllegalStateException(
                    "No se pudo crear el asiento "
                            + "de anulación del pago."
            );
        }

        crearMovimientosInversos(
                gasto,
                asientoAnulacion,
                movimientosOriginales,
                fecha
        );

        asientoOriginal.setEstado(
                ESTADO_ANULADO
        );

        asientoRepository.save(
                asientoOriginal
        );

        return asientoAnulacion;
    }

    private void validarGasto(
            ContabilidadGasto gasto
    ) {
        if (
                gasto == null
                        || gasto.getId() == null
        ) {
            throw new IllegalArgumentException(
                    "El gasto es obligatorio."
            );
        }

        if (gasto.getComunidadId() == null) {
            throw new IllegalArgumentException(
                    "El gasto no tiene comunidad asociada."
            );
        }

        if (
                !Boolean.TRUE.equals(
                        gasto.getPagado()
                )
        ) {
            throw new IllegalStateException(
                    "Solo se puede deshacer el pago "
                            + "de un gasto pagado."
            );
        }
    }

    private void crearMovimientosInversos(
            ContabilidadGasto gasto,
            ContabilidadAsiento asientoAnulacion,
            List<ContabilidadMovimiento> movimientosOriginales,
            LocalDate fecha
    ) {
        String referenciaAnulacion =
                "ASIENTO-"
                        + asientoAnulacion.getId();

        boolean movimientosYaCreados =
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                gasto.getComunidadId(),
                                referenciaAnulacion
                        );

        if (movimientosYaCreados) {
            throw new IllegalStateException(
                    "Ya existen movimientos contables "
                            + "para la anulación del pago."
            );
        }

        List<ContabilidadMovimiento> movimientosInversos =
                new ArrayList<>();

        for (
                ContabilidadMovimiento movimientoOriginal
                : movimientosOriginales
        ) {
            ContabilidadMovimiento movimientoInverso =
                    new ContabilidadMovimiento();

            movimientoInverso.setComunidadId(
                    gasto.getComunidadId()
            );

            movimientoInverso.setFecha(
                    fecha
            );

            movimientoInverso.setNumeroAsiento(
                    referenciaAnulacion
            );

            movimientoInverso.setConcepto(
                    "Anulación pago gasto "
                            + gasto.getId()
                            + " - asiento "
                            + asientoAnulacion.getNumeroAsiento()
            );

            movimientoInverso.setCuentaId(
                    movimientoOriginal.getCuentaId()
            );

            movimientoInverso.setDebe(
                    importeSeguro(
                            movimientoOriginal.getHaber()
                    )
            );

            movimientoInverso.setHaber(
                    importeSeguro(
                            movimientoOriginal.getDebe()
                    )
            );

            movimientosInversos.add(
                    movimientoInverso
            );
        }

        movimientoRepository.saveAll(
                movimientosInversos
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