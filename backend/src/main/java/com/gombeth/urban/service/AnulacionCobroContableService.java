package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnulacionCobroContableService {

    private static final String ORIGEN_COBRO =
            "RECIBO_COBRADO";

    private static final String ORIGEN_ANULACION =
            "ANULACION_RECIBO_COBRADO";

    private static final String ESTADO_ANULADO =
            "ANULADO";

    private final ContabilidadAsientoRepository
            asientoRepository;

    private final ContabilidadMovimientoRepository
            movimientoRepository;

    private final ContabilidadAsientoService
            asientoService;

    public AnulacionCobroContableService(
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
    public ContabilidadAsiento anularCobroRecibo(
            ContabilidadRecibo recibo,
            Long usuarioId,
            LocalDate fechaAnulacion
    ) {
        validarRecibo(
                recibo
        );

        ContabilidadAsiento asientoOriginal =
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                recibo.getComunidadId(),
                                ORIGEN_COBRO,
                                recibo.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el asiento contable "
                                                + "del cobro del recibo "
                                                + recibo.getId()
                                                + "."
                                )
                        );

        if (
                ESTADO_ANULADO.equals(
                        asientoOriginal.getEstado()
                )
        ) {
            throw new IllegalStateException(
                    "El asiento del cobro del recibo "
                            + recibo.getId()
                            + " ya está anulado."
            );
        }

        List<ContabilidadMovimiento> movimientosOriginales =
                obtenerMovimientosOriginales(
                        recibo,
                        asientoOriginal
                );

        if (movimientosOriginales.isEmpty()) {
            throw new IllegalStateException(
                    "El asiento del cobro del recibo "
                            + recibo.getId()
                            + " no tiene movimientos contables."
            );
        }

        LocalDate fecha =
                fechaAnulacion != null
                        ? fechaAnulacion
                        : LocalDate.now();

        ContabilidadAsiento asientoAnulacion =
                asientoService.crearAsientoAutomatico(
                        recibo.getComunidadId(),
                        fecha,
                        "Anulación cobro recibo "
                                + recibo.getId(),
                        ORIGEN_ANULACION,
                        asientoOriginal.getId(),
                        usuarioId
                );

        crearMovimientosInversos(
                recibo,
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

    private void validarRecibo(
            ContabilidadRecibo recibo
    ) {
        if (
                recibo == null
                        || recibo.getId() == null
        ) {
            throw new IllegalArgumentException(
                    "El recibo es obligatorio."
            );
        }

        if (recibo.getComunidadId() == null) {
            throw new IllegalArgumentException(
                    "El recibo no tiene comunidad asociada."
            );
        }
    }

    private List<ContabilidadMovimiento>
    obtenerMovimientosOriginales(
            ContabilidadRecibo recibo,
            ContabilidadAsiento asientoOriginal
    ) {
        if (asientoOriginal.getId() != null) {
            String referenciaAsiento =
                    "ASIENTO-"
                            + asientoOriginal.getId();

            List<ContabilidadMovimiento> movimientos =
                    movimientoRepository
                            .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                    recibo.getComunidadId(),
                                    referenciaAsiento
                            );

            if (!movimientos.isEmpty()) {
                return movimientos;
            }
        }

        String referenciaHistorica =
                "COBRO-RECIBO-"
                        + recibo.getId();

        return movimientoRepository
                .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                        recibo.getComunidadId(),
                        referenciaHistorica
                );
    }

    private void crearMovimientosInversos(
            ContabilidadRecibo recibo,
            ContabilidadAsiento asientoAnulacion,
            List<ContabilidadMovimiento> movimientosOriginales,
            LocalDate fecha
    ) {
        if (asientoAnulacion.getId() == null) {
            throw new IllegalStateException(
                    "El asiento de anulación no tiene identificador."
            );
        }

        String referenciaAnulacion =
                "ASIENTO-"
                        + asientoAnulacion.getId();

        boolean movimientosYaCreados =
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                recibo.getComunidadId(),
                                referenciaAnulacion
                        );

        if (movimientosYaCreados) {
            return;
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
                    recibo.getComunidadId()
            );

            movimientoInverso.setFecha(
                    fecha
            );

            movimientoInverso.setNumeroAsiento(
                    referenciaAnulacion
            );

            movimientoInverso.setConcepto(
                    "Anulación cobro recibo "
                            + recibo.getId()
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