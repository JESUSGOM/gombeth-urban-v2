package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class PagoGastoContableService {

    private static final String ORIGEN_PAGO_GASTO =
            "GASTO_PAGADO";

    private static final String ESTADO_ANULADO =
            "ANULADO";

    private final ContabilidadAsientoRepository
            asientoRepository;

    private final ContabilidadMovimientoRepository
            movimientoRepository;

    private final CuentaContableRepository
            cuentaRepository;

    private final ContabilidadAsientoService
            asientoService;

    public PagoGastoContableService(
            ContabilidadAsientoRepository asientoRepository,
            ContabilidadMovimientoRepository movimientoRepository,
            CuentaContableRepository cuentaRepository,
            ContabilidadAsientoService asientoService
    ) {
        this.asientoRepository =
                asientoRepository;

        this.movimientoRepository =
                movimientoRepository;

        this.cuentaRepository =
                cuentaRepository;

        this.asientoService =
                asientoService;
    }

    @Transactional
    public ContabilidadAsiento registrarPago(
            ContabilidadGasto gasto,
            Long usuarioId,
            LocalDate fechaPago
    ) {
        validarGasto(
                gasto
        );

        comprobarPagoActivo(
                gasto
        );

        CuentaContable cuentaProveedor =
                buscarCuentaProveedorContabilizada(
                        gasto
                );

        CuentaContable cuentaBanco =
                buscarCuentaPorPrefijo(
                        gasto.getComunidadId(),
                        "572",
                        "No existe cuenta bancaria 572 para la comunidad "
                );

        LocalDate fecha =
                fechaPago != null
                        ? fechaPago
                        : LocalDate.now();

        ContabilidadAsiento asiento =
                asientoService.crearAsientoAutomatico(
                        gasto.getComunidadId(),
                        fecha,
                        "Pago factura proveedor "
                                + gasto.getProveedor(),
                        ORIGEN_PAGO_GASTO,
                        gasto.getId(),
                        usuarioId
                );

        if (
                asiento == null
                        || asiento.getId() == null
        ) {
            throw new IllegalStateException(
                    "No se pudo crear el asiento "
                            + "contable del pago del gasto."
            );
        }

        String referenciaAsiento =
                "ASIENTO-"
                        + asiento.getId();

        boolean movimientosYaCreados =
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                gasto.getComunidadId(),
                                referenciaAsiento
                        );

        if (movimientosYaCreados) {
            throw new IllegalStateException(
                    "Ya existen movimientos contables "
                            + "para el pago del gasto "
                            + gasto.getId()
                            + "."
            );
        }

        String concepto =
                "Pago factura "
                        + gasto.getNumeroFactura()
                        + " - "
                        + gasto.getProveedor()
                        + " - asiento "
                        + asiento.getNumeroAsiento();

        ContabilidadMovimiento debeProveedor =
                new ContabilidadMovimiento();

        debeProveedor.setComunidadId(
                gasto.getComunidadId()
        );

        debeProveedor.setFecha(
                fecha
        );

        debeProveedor.setNumeroAsiento(
                referenciaAsiento
        );

        debeProveedor.setConcepto(
                concepto
        );

        debeProveedor.setCuentaId(
                cuentaProveedor.getId()
        );

        debeProveedor.setDebe(
                gasto.getImporteTotal()
        );

        debeProveedor.setHaber(
                BigDecimal.ZERO
        );

        ContabilidadMovimiento haberBanco =
                new ContabilidadMovimiento();

        haberBanco.setComunidadId(
                gasto.getComunidadId()
        );

        haberBanco.setFecha(
                fecha
        );

        haberBanco.setNumeroAsiento(
                referenciaAsiento
        );

        haberBanco.setConcepto(
                concepto
        );

        haberBanco.setCuentaId(
                cuentaBanco.getId()
        );

        haberBanco.setDebe(
                BigDecimal.ZERO
        );

        haberBanco.setHaber(
                gasto.getImporteTotal()
        );

        movimientoRepository.save(
                debeProveedor
        );

        movimientoRepository.save(
                haberBanco
        );

        return asiento;
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
            throw new IllegalStateException(
                    "El gasto no tiene comunidad asociada."
            );
        }

        if (
                gasto.getImporteTotal() == null
                        || gasto.getImporteTotal()
                        .compareTo(
                                BigDecimal.ZERO
                        ) <= 0
        ) {
            throw new IllegalStateException(
                    "El gasto no tiene importe válido."
            );
        }

        if (
                gasto.getNumeroAsiento() == null
                        || gasto.getNumeroAsiento()
                        .isBlank()
        ) {
            throw new IllegalStateException(
                    "El gasto debe estar contabilizado "
                            + "antes de poder pagarse."
            );
        }

        if (
                Boolean.TRUE.equals(
                        gasto.getPagado()
                )
        ) {
            throw new IllegalStateException(
                    "El gasto ya está pagado."
            );
        }
    }

    private void comprobarPagoActivo(
            ContabilidadGasto gasto
    ) {
        Optional<ContabilidadAsiento> ultimoPago =
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                gasto.getComunidadId(),
                                ORIGEN_PAGO_GASTO,
                                gasto.getId()
                        );

        if (
                ultimoPago.isPresent()
                        && !ESTADO_ANULADO.equals(
                        ultimoPago.get().getEstado()
                )
        ) {
            throw new IllegalStateException(
                    "Ya existe un pago contable activo "
                            + "para el gasto "
                            + gasto.getId()
                            + "."
            );
        }
    }

    private CuentaContable buscarCuentaProveedorContabilizada(
            ContabilidadGasto gasto
    ) {

        /*
         * Formato actual V2.
         *
         * Los movimientos de la contabilización del gasto
         * se identifican por el id del asiento:
         *
         * ASIENTO-{idAsiento}
         */
        Optional<ContabilidadAsiento> asientoContabilizacion =
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                gasto.getComunidadId(),
                                "GASTO_CONTABILIZADO",
                                gasto.getId()
                        );

        if (asientoContabilizacion.isPresent()) {

            ContabilidadAsiento asiento =
                    asientoContabilizacion.get();

            if (
                    asiento.getId() != null
                            && !ESTADO_ANULADO.equals(
                            asiento.getEstado()
                    )
            ) {
                String referenciaAsiento =
                        "ASIENTO-"
                                + asiento.getId();

                CuentaContable cuenta =
                        buscar410EnMovimientos(
                                gasto.getComunidadId(),
                                referenciaAsiento
                        );

                if (cuenta != null) {
                    return cuenta;
                }
            }
        }

        /*
         * Compatibilidad con gastos V2 contabilizados antes
         * del cambio a referencias ASIENTO-{id}.
         *
         * Esos movimientos utilizaban:
         *
         * GASTO-{id}
         */
        String referenciaV2Anterior =
                "GASTO-"
                        + gasto.getId();

        CuentaContable cuenta =
                buscar410EnMovimientos(
                        gasto.getComunidadId(),
                        referenciaV2Anterior
                );

        if (cuenta != null) {
            return cuenta;
        }

        /*
         * Compatibilidad con gastos contabilizados por el
         * programa antiguo.
         *
         * numeroAsiento puede contener referencias históricas
         * como FRA-..., GAS-..., etc.
         */
        String referenciaLegacy =
                gasto.getNumeroAsiento();

        if (
                referenciaLegacy != null
                        && !referenciaLegacy.isBlank()
                        && !referenciaLegacy.equals(
                        referenciaV2Anterior
                )
        ) {
            cuenta =
                    buscar410EnMovimientos(
                            gasto.getComunidadId(),
                            referenciaLegacy
                    );

            if (cuenta != null) {
                return cuenta;
            }
        }

        throw new IllegalStateException(
                "No se ha podido localizar la cuenta 410 "
                        + "con la que fue contabilizado el gasto "
                        + gasto.getId()
                        + "."
        );
    }

    private CuentaContable buscar410EnMovimientos(
            Long comunidadId,
            String numeroAsiento
    ) {
        java.util.List<ContabilidadMovimiento> movimientos =
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                comunidadId,
                                numeroAsiento
                        );

        for (
                ContabilidadMovimiento movimiento
                : movimientos
        ) {
            if (
                    movimiento.getCuentaId() == null
                            || movimiento.getHaber() == null
                            || movimiento.getHaber()
                            .compareTo(
                                    BigDecimal.ZERO
                            ) <= 0
            ) {
                continue;
            }

            CuentaContable cuenta =
                    cuentaRepository
                            .findByIdAndComunidad_Id(
                                    movimiento.getCuentaId(),
                                    comunidadId
                            )
                            .orElse(null);

            if (
                    cuenta != null
                            && cuenta.getCodigo() != null
                            && cuenta.getCodigo()
                            .startsWith(
                                    "410"
                            )
            ) {
                return cuenta;
            }
        }

        return null;
    }

    private CuentaContable buscarCuentaPorPrefijo(
            Long comunidadId,
            String prefijo,
            String mensajeError
    ) {
        return cuentaRepository
                .findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                        comunidadId,
                        prefijo
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                mensajeError
                                        + comunidadId
                        )
                );
    }
}