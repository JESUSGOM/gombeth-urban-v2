package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ReciboCobroService {

    private static final String ESTADO_PENDIENTE =
            "PENDIENTE";

    private static final String ESTADO_COBRADO =
            "COBRADO";

    private final ContabilidadReciboRepository
            reciboRepository;

    private final ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    private final AnulacionCobroContableService
            anulacionCobroContableService;

    public ReciboCobroService(
            ContabilidadReciboRepository reciboRepository,
            ContabilidadAutomaticaService
                    contabilidadAutomaticaService,
            AnulacionCobroContableService
                    anulacionCobroContableService
    ) {
        this.reciboRepository =
                reciboRepository;

        this.contabilidadAutomaticaService =
                contabilidadAutomaticaService;

        this.anulacionCobroContableService =
                anulacionCobroContableService;
    }

    @Transactional
    public ContabilidadRecibo cobrarManualmente(
            ContabilidadRecibo recibo,
            Long usuarioId,
            LocalDate fechaCobro
    ) {
        validarRecibo(recibo);

        if (!ESTADO_PENDIENTE.equalsIgnoreCase(
                recibo.getEstado()
        )) {
            throw new IllegalStateException(
                    "Solo se puede cobrar manualmente "
                            + "un recibo pendiente."
            );
        }

        if (recibo.getImporte() == null
                || recibo.getImporte()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "El recibo no tiene un importe válido."
            );
        }

        LocalDate fecha =
                fechaCobro != null
                        ? fechaCobro
                        : LocalDate.now();

        /*
         * El cobro manual no crea un movimiento bancario.
         * Se utiliza este objeto únicamente para trasladar
         * la fecha al servicio contable existente.
         */
        MovimientoBancario movimientoManual =
                new MovimientoBancario();

        movimientoManual.setComunidadId(
                recibo.getComunidadId()
        );

        movimientoManual.setFechaOperacion(
                fecha
        );

        movimientoManual.setFechaValor(
                fecha
        );

        movimientoManual.setImporte(
                recibo.getImporte()
        );

        movimientoManual.setSigno(
                "+"
        );

        movimientoManual.setConcepto(
                "Cobro manual recibo " + recibo.getId()
        );

        contabilidadAutomaticaService
                .registrarCobroRecibo(
                        recibo,
                        movimientoManual,
                        usuarioId
                );

        recibo.setEstado(
                ESTADO_COBRADO
        );

        recibo.setFechaCobroBanco(
                fecha
        );

        recibo.setMovimientoBancarioId(
                null
        );

        recibo.setPagadoAcumulado(
                recibo.getImporte()
        );

        return reciboRepository.save(
                recibo
        );
    }

    @Transactional
    public ContabilidadRecibo anularCobroManual(
            ContabilidadRecibo recibo,
            Long usuarioId,
            LocalDate fechaAnulacion
    ) {
        validarRecibo(recibo);

        if (!ESTADO_COBRADO.equalsIgnoreCase(
                recibo.getEstado()
        )) {
            throw new IllegalStateException(
                    "Solo se puede anular un recibo cobrado."
            );
        }

        if (recibo.getMovimientoBancarioId() != null) {
            throw new IllegalStateException(
                    "Este recibo está asociado a un movimiento "
                            + "bancario. Debe utilizarse el proceso "
                            + "de desconciliación."
            );
        }

        LocalDate fecha =
                fechaAnulacion != null
                        ? fechaAnulacion
                        : LocalDate.now();

        ContabilidadAsiento asientoAnulacion =
                anulacionCobroContableService
                        .anularCobroRecibo(
                                recibo,
                                usuarioId,
                                fecha
                        );

        if (asientoAnulacion == null
                || asientoAnulacion.getId() == null) {
            throw new IllegalStateException(
                    "No se pudo crear el asiento "
                            + "de anulación del cobro."
            );
        }

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

        return reciboRepository.save(
                recibo
        );
    }

    private void validarRecibo(
            ContabilidadRecibo recibo
    ) {
        if (recibo == null
                || recibo.getId() == null) {
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
}