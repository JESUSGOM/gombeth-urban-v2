package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ConciliacionBancariaService {

    private static final String ESTADO_PENDIENTE =
            "PENDIENTE";

    private static final String ESTADO_COBRADO =
            "COBRADO";

    private static final String SIGNO_HABER =
            "2";

    private final MovimientoBancarioRepository
            movimientoBancarioRepository;

    private final ContabilidadReciboRepository
            reciboRepository;

    private final ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    private final AnulacionCobroContableService
            anulacionCobroContableService;

    public ConciliacionBancariaService(
            MovimientoBancarioRepository
                    movimientoBancarioRepository,
            ContabilidadReciboRepository
                    reciboRepository,
            ContabilidadAutomaticaService
                    contabilidadAutomaticaService,
            AnulacionCobroContableService
                    anulacionCobroContableService
    ) {
        this.movimientoBancarioRepository =
                movimientoBancarioRepository;

        this.reciboRepository =
                reciboRepository;

        this.contabilidadAutomaticaService =
                contabilidadAutomaticaService;

        this.anulacionCobroContableService =
                anulacionCobroContableService;
    }

    /**
     * Ejecuta la conciliación automática de una comunidad.
     *
     * Solo se concilian movimientos de abono cuyo importe
     * coincide exactamente con un único recibo pendiente.
     *
     * El cambio del recibo, el movimiento bancario y el
     * asiento contable se realizan dentro de la misma
     * transacción.
     */
    @Transactional
    public int conciliarAutomaticamenteComunidad(
            Long comunidadId
    ) {
        if (comunidadId == null) {
            throw new IllegalArgumentException(
                    "La comunidad es obligatoria."
            );
        }

        List<MovimientoBancario> movimientos =
                movimientoBancarioRepository
                        .findByComunidadIdOrderByFechaOperacionAscIdAsc(
                                comunidadId
                        );

        int conciliados = 0;

        for (MovimientoBancario movimiento : movimientos) {

            if (intentarConciliarMovimiento(movimiento)) {
                conciliados++;
            }
        }

        return conciliados;
    }

    /**
     * Intenta conciliar automáticamente un movimiento con
     * un único recibo pendiente del mismo importe.
     */
    @Transactional
    public boolean intentarConciliarMovimiento(
            MovimientoBancario movimiento
    ) {
        if (!esMovimientoConciliable(movimiento)) {
            return false;
        }

        List<ContabilidadRecibo> candidatos =
                reciboRepository
                        .findByComunidadIdAndEstado(
                                movimiento.getComunidadId(),
                                ESTADO_PENDIENTE
                        );

        List<ContabilidadRecibo> mismoImporte =
                candidatos.stream()
                        .filter(this::esReciboPendienteValido)
                        .filter(recibo ->
                                recibo.getImporte().compareTo(
                                        movimiento.getImporte()
                                ) == 0
                        )
                        .toList();

        if (mismoImporte.size() != 1) {
            return false;
        }

        aplicarConciliacion(
                movimiento,
                mismoImporte,
                null
        );

        return true;
    }

    /**
     * Mantiene compatibilidad con las llamadas internas
     * y pruebas que no proporcionan usuario.
     */
    @Transactional
    public MovimientoBancario conciliarMovimientoConRecibos(
            Long movimientoId,
            List<Long> reciboIds
    ) {
        return conciliarMovimientoConRecibos(
                movimientoId,
                reciboIds,
                null
        );
    }

    /**
     * Concilia manualmente un movimiento bancario con uno
     * o varios recibos seleccionados, registrando el usuario
     * que realiza la operación.
     *
     * La operación es atómica:
     *
     * - actualiza los recibos;
     * - registra los asientos contables de cobro;
     * - marca el movimiento como conciliado y procesado.
     *
     * Si falla cualquiera de estas operaciones, se revierte
     * la transacción completa.
     */
    @Transactional
    public MovimientoBancario conciliarMovimientoConRecibos(
            Long movimientoId,
            List<Long> reciboIds,
            Long usuarioId
    ) {
        if (movimientoId == null) {
            throw new IllegalArgumentException(
                    "El movimiento bancario es obligatorio."
            );
        }

        List<Long> idsNormalizados =
                normalizarIdsRecibos(reciboIds);

        MovimientoBancario movimiento =
                movimientoBancarioRepository
                        .findById(movimientoId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No existe el movimiento bancario "
                                                + movimientoId
                                                + "."
                                )
                        );

        validarMovimientoParaConciliacion(
                movimiento
        );

        List<ContabilidadRecibo> recibos =
                reciboRepository.findByIdIn(
                        idsNormalizados
                );

        if (recibos.size() != idsNormalizados.size()) {
            throw new IllegalArgumentException(
                    "No se han encontrado todos los recibos "
                            + "seleccionados."
            );
        }

        validarRecibos(
                movimiento,
                recibos
        );

        validarImporteTotal(
                movimiento,
                recibos
        );

        aplicarConciliacion(
                movimiento,
                recibos,
                usuarioId
        );

        return movimiento;
    }

    /**
     * Deshace una conciliación conservando la trazabilidad
     * contable mediante un contrasiento.
     *
     * La operación es atómica:
     *
     * - anula contablemente cada cobro;
     * - devuelve los recibos a pendiente;
     * - libera su movimiento bancario;
     * - marca el movimiento como no conciliado y no procesado.
     */
    @Transactional
    public MovimientoBancario desconciliarMovimiento(
            Long movimientoId,
            Long usuarioId,
            LocalDate fechaAnulacion
    ) {
        if (
                movimientoId == null
                        || movimientoId <= 0
        ) {
            throw new IllegalArgumentException(
                    "El movimiento bancario es obligatorio."
            );
        }

        if (
                usuarioId == null
                        || usuarioId <= 0
        ) {
            throw new IllegalArgumentException(
                    "El usuario es obligatorio para "
                            + "registrar la anulación."
            );
        }

        MovimientoBancario movimiento =
                movimientoBancarioRepository
                        .findById(movimientoId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No existe el movimiento bancario "
                                                + movimientoId
                                                + "."
                                )
                        );

        validarMovimientoParaDesconciliacion(
                movimiento
        );

        List<ContabilidadRecibo> recibos =
                reciboRepository
                        .findByComunidadIdAndMovimientoBancarioIdOrderByIdAsc(
                                movimiento.getComunidadId(),
                                movimiento.getId()
                        );

        if (recibos.isEmpty()) {
            throw new IllegalStateException(
                    "El movimiento conciliado no tiene "
                            + "recibos asociados."
            );
        }

        validarRecibosParaDesconciliacion(
                movimiento,
                recibos
        );

        LocalDate fecha =
                fechaAnulacion != null
                        ? fechaAnulacion
                        : LocalDate.now();

        for (ContabilidadRecibo recibo : recibos) {

            anulacionCobroContableService
                    .anularCobroRecibo(
                            recibo,
                            usuarioId,
                            fecha
                    );

            recibo.setEstado(
                    ESTADO_PENDIENTE
            );

            recibo.setFechaCobroBanco(
                    null
            );

            recibo.setMovimientoBancarioId(
                    null
            );

            recibo.setPagadoAcumulado(
                    BigDecimal.ZERO
            );
        }

        reciboRepository.saveAll(
                recibos
        );

        movimiento.setConciliado(false);
        movimiento.setProcesado(false);

        movimientoBancarioRepository.save(
                movimiento
        );

        return movimiento;
    }

    private void aplicarConciliacion(
            MovimientoBancario movimiento,
            List<ContabilidadRecibo> recibos,
            Long usuarioId
    ) {
        for (ContabilidadRecibo recibo : recibos) {

            recibo.setEstado(
                    ESTADO_COBRADO
            );

            recibo.setFechaCobroBanco(
                    movimiento.getFechaOperacion()
            );

            recibo.setMovimientoBancarioId(
                    movimiento.getId()
            );

            recibo.setPagadoAcumulado(
                    recibo.getImporte()
            );

            if (usuarioId == null) {
                contabilidadAutomaticaService
                        .registrarCobroRecibo(
                                recibo,
                                movimiento
                        );
            } else {
                contabilidadAutomaticaService
                        .registrarCobroRecibo(
                                recibo,
                                movimiento,
                                usuarioId
                        );
            }
        }

        reciboRepository.saveAll(
                recibos
        );

        movimiento.setConciliado(true);
        movimiento.setProcesado(true);

        movimientoBancarioRepository.save(
                movimiento
        );
    }

    private void validarMovimientoParaDesconciliacion(
            MovimientoBancario movimiento
    ) {
        if (movimiento.getComunidadId() == null) {
            throw new IllegalStateException(
                    "El movimiento no tiene comunidad asociada."
            );
        }

        if (!Boolean.TRUE.equals(
                movimiento.getConciliado()
        )) {
            throw new IllegalStateException(
                    "El movimiento no está conciliado."
            );
        }
    }

    private void validarRecibosParaDesconciliacion(
            MovimientoBancario movimiento,
            List<ContabilidadRecibo> recibos
    ) {
        for (ContabilidadRecibo recibo : recibos) {

            if (
                    recibo == null
                            || recibo.getId() == null
            ) {
                throw new IllegalStateException(
                        "La conciliación contiene un recibo "
                                + "no válido."
                );
            }

            if (!movimiento.getComunidadId().equals(
                    recibo.getComunidadId()
            )) {
                throw new IllegalStateException(
                        "El recibo "
                                + recibo.getId()
                                + " no pertenece a la comunidad "
                                + "del movimiento."
                );
            }

            if (!ESTADO_COBRADO.equals(
                    recibo.getEstado()
            )) {
                throw new IllegalStateException(
                        "El recibo "
                                + recibo.getId()
                                + " no está cobrado."
                );
            }

            if (!movimiento.getId().equals(
                    recibo.getMovimientoBancarioId()
            )) {
                throw new IllegalStateException(
                        "El recibo "
                                + recibo.getId()
                                + " no está asociado al movimiento "
                                + movimiento.getId()
                                + "."
                );
            }
        }
    }

    private void validarMovimientoParaConciliacion(
            MovimientoBancario movimiento
    ) {
        if (movimiento.getComunidadId() == null) {
            throw new IllegalStateException(
                    "El movimiento no tiene comunidad asociada."
            );
        }

        if (Boolean.TRUE.equals(
                movimiento.getConciliado()
        )) {
            throw new IllegalStateException(
                    "El movimiento ya está conciliado."
            );
        }

        if (Boolean.TRUE.equals(
                movimiento.getProcesado()
        )) {
            throw new IllegalStateException(
                    "El movimiento ya está procesado."
            );
        }

        if (
                movimiento.getImporte() == null
                        || movimiento.getImporte()
                        .compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new IllegalStateException(
                    "El movimiento no tiene un importe válido."
            );
        }

        if (!SIGNO_HABER.equals(
                movimiento.getSigno()
        )) {
            throw new IllegalStateException(
                    "Solo pueden conciliarse como cobros "
                            + "los movimientos de haber."
            );
        }
    }

    private void validarRecibos(
            MovimientoBancario movimiento,
            List<ContabilidadRecibo> recibos
    ) {
        for (ContabilidadRecibo recibo : recibos) {

            if (recibo.getComunidadId() == null) {
                throw new IllegalStateException(
                        "El recibo "
                                + recibo.getId()
                                + " no tiene comunidad asociada."
                );
            }

            if (!recibo.getComunidadId().equals(
                    movimiento.getComunidadId()
            )) {
                throw new IllegalArgumentException(
                        "El recibo "
                                + recibo.getId()
                                + " no pertenece a la comunidad "
                                + "del movimiento."
                );
            }

            if (!ESTADO_PENDIENTE.equals(
                    recibo.getEstado()
            )) {
                throw new IllegalStateException(
                        "El recibo "
                                + recibo.getId()
                                + " ya no está pendiente."
                );
            }

            if (
                    recibo.getMovimientoBancarioId()
                            != null
            ) {
                throw new IllegalStateException(
                        "El recibo "
                                + recibo.getId()
                                + " ya está asociado a otro "
                                + "movimiento bancario."
                );
            }

            if (!esReciboPendienteValido(recibo)) {
                throw new IllegalStateException(
                        "El recibo "
                                + recibo.getId()
                                + " no tiene un importe válido."
                );
            }
        }
    }

    private void validarImporteTotal(
            MovimientoBancario movimiento,
            List<ContabilidadRecibo> recibos
    ) {
        BigDecimal totalSeleccionado =
                recibos.stream()
                        .map(
                                ContabilidadRecibo::getImporte
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        if (totalSeleccionado.compareTo(
                movimiento.getImporte()
        ) != 0) {
            throw new IllegalArgumentException(
                    "El total seleccionado no coincide "
                            + "con el importe del movimiento "
                            + "bancario."
            );
        }
    }

    private List<Long> normalizarIdsRecibos(
            List<Long> reciboIds
    ) {
        if (
                reciboIds == null
                        || reciboIds.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "Debe seleccionar al menos un recibo."
            );
        }

        Set<Long> idsUnicos =
                new LinkedHashSet<>();

        for (Long reciboId : reciboIds) {

            if (
                    reciboId == null
                            || reciboId <= 0
            ) {
                throw new IllegalArgumentException(
                        "Hay identificadores de recibo "
                                + "no válidos."
                );
            }

            idsUnicos.add(reciboId);
        }

        return List.copyOf(idsUnicos);
    }

    private boolean esMovimientoConciliable(
            MovimientoBancario movimiento
    ) {
        return movimiento != null
                && movimiento.getId() != null
                && movimiento.getComunidadId() != null
                && !Boolean.TRUE.equals(
                movimiento.getConciliado()
        )
                && !Boolean.TRUE.equals(
                movimiento.getProcesado()
        )
                && movimiento.getImporte() != null
                && movimiento.getImporte()
                .compareTo(BigDecimal.ZERO) > 0
                && SIGNO_HABER.equals(
                movimiento.getSigno()
        );
    }

    private boolean esReciboPendienteValido(
            ContabilidadRecibo recibo
    ) {
        return recibo != null
                && recibo.getId() != null
                && ESTADO_PENDIENTE.equals(
                recibo.getEstado()
        )
                && recibo.getImporte() != null
                && recibo.getImporte()
                .compareTo(BigDecimal.ZERO) > 0;
    }
}