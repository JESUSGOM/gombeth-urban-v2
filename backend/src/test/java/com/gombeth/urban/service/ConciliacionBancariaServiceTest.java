package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConciliacionBancariaServiceTest {

    @Mock
    private MovimientoBancarioRepository
            movimientoBancarioRepository;

    @Mock
    private ContabilidadReciboRepository
            reciboRepository;

    @Mock
    private ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    @Mock
    private AnulacionCobroContableService
            anulacionCobroContableService;

    @Mock
    private MovimientoBancario movimiento;

    @Mock
    private ContabilidadRecibo recibo;

    @InjectMocks
    private ConciliacionBancariaService service;

    @Test
    void conciliacionManualActualizaReciboMovimientoYContabilidad() {
        prepararMovimientoHaber(
                328L,
                33L,
                "75.50"
        );

        prepararReciboPendiente(
                1554L,
                33L,
                "75.50"
        );

        when(
                movimientoBancarioRepository.findById(
                        328L
                )
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(
                reciboRepository.findByIdIn(
                        List.of(1554L)
                )
        ).thenReturn(
                List.of(recibo)
        );

        MovimientoBancario resultado =
                service.conciliarMovimientoConRecibos(
                        328L,
                        List.of(1554L)
                );

        assertEquals(
                movimiento,
                resultado
        );

        verify(recibo).setEstado(
                "COBRADO"
        );

        verify(recibo).setFechaCobroBanco(
                LocalDate.of(2026, 7, 4)
        );

        verify(recibo).setMovimientoBancarioId(
                328L
        );

        verify(recibo).setPagadoAcumulado(
                new BigDecimal("75.50")
        );

        verify(contabilidadAutomaticaService)
                .registrarCobroRecibo(
                        recibo,
                        movimiento
                );

        verify(reciboRepository).saveAll(
                List.of(recibo)
        );

        verify(movimiento).setConciliado(
                true
        );

        verify(movimiento).setProcesado(
                true
        );

        verify(movimientoBancarioRepository)
                .save(movimiento);
    }

    @Test
    void conciliacionManualRechazaImportesDiferentes() {
        prepararMovimientoHaber(
                325L,
                33L,
                "75.00"
        );

        prepararReciboPendiente(
                1554L,
                33L,
                "75.50"
        );

        when(
                movimientoBancarioRepository.findById(
                        325L
                )
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(
                reciboRepository.findByIdIn(
                        List.of(1554L)
                )
        ).thenReturn(
                List.of(recibo)
        );

        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.conciliarMovimientoConRecibos(
                                        325L,
                                        List.of(1554L)
                                )
                );

        assertTrue(
                excepcion.getMessage().contains(
                        "no coincide"
                )
        );

        verify(
                contabilidadAutomaticaService,
                never()
        ).registrarCobroRecibo(
                recibo,
                movimiento
        );

        verify(
                reciboRepository,
                never()
        ).saveAll(
                anyList()
        );

        verify(
                movimientoBancarioRepository,
                never()
        ).save(movimiento);
    }

    @Test
    void conciliacionManualRechazaReciboDeOtraComunidad() {
        prepararMovimientoHaber(
                328L,
                33L,
                "75.50"
        );

        prepararReciboPendiente(
                1554L,
                99L,
                "75.50"
        );

        when(
                movimientoBancarioRepository.findById(
                        328L
                )
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(
                reciboRepository.findByIdIn(
                        List.of(1554L)
                )
        ).thenReturn(
                List.of(recibo)
        );

        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.conciliarMovimientoConRecibos(
                                        328L,
                                        List.of(1554L)
                                )
                );

        assertTrue(
                excepcion.getMessage().contains(
                        "no pertenece"
                )
        );

        verifyNoInteractions(
                contabilidadAutomaticaService
        );

        verify(
                reciboRepository,
                never()
        ).saveAll(
                anyList()
        );
    }

    @Test
    void conciliacionAutomaticaConUnCandidatoGeneraContabilidad() {
        prepararMovimientoHaber(
                328L,
                33L,
                "75.50"
        );

        prepararReciboPendiente(
                1554L,
                33L,
                "75.50"
        );

        when(
                movimientoBancarioRepository
                        .findByComunidadIdOrderByFechaOperacionAscIdAsc(
                                33L
                        )
        ).thenReturn(
                List.of(movimiento)
        );

        when(
                reciboRepository
                        .findByComunidadIdAndEstado(
                                33L,
                                "PENDIENTE"
                        )
        ).thenReturn(
                List.of(recibo)
        );

        int conciliados =
                service.conciliarAutomaticamenteComunidad(
                        33L
                );

        assertEquals(
                1,
                conciliados
        );

        verify(contabilidadAutomaticaService)
                .registrarCobroRecibo(
                        recibo,
                        movimiento
                );

        verify(reciboRepository).saveAll(
                List.of(recibo)
        );

        verify(movimientoBancarioRepository)
                .save(movimiento);
    }

    @Test
    void movimientoDebeNoPuedeConciliarseComoCobro() {
        when(
                movimientoBancarioRepository.findById(
                        326L
                )
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(movimiento.getId())
                .thenReturn(326L);

        when(movimiento.getComunidadId())
                .thenReturn(33L);

        when(movimiento.getConciliado())
                .thenReturn(false);

        when(movimiento.getProcesado())
                .thenReturn(false);

        when(movimiento.getImporte())
                .thenReturn(
                        new BigDecimal("18.50")
                );

        when(movimiento.getSigno())
                .thenReturn("1");

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.conciliarMovimientoConRecibos(
                                        326L,
                                        List.of(1554L)
                                )
                );

        assertTrue(
                excepcion.getMessage().contains(
                        "movimientos de haber"
                )
        );

        verify(
                reciboRepository,
                never()
        ).findByIdIn(
                anyList()
        );

        verifyNoInteractions(
                contabilidadAutomaticaService
        );
    }

    @Test
    void desconciliacionAnulaContabilidadYRestauraReciboYMovimiento() {
        LocalDate fechaAnulacion =
                LocalDate.of(
                        2026,
                        7,
                        30
                );

        prepararMovimientoConciliado(
                328L,
                33L
        );

        prepararReciboCobrado(
                1554L,
                33L,
                328L
        );

        when(
                movimientoBancarioRepository.findById(
                        328L
                )
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(
                reciboRepository
                        .findByComunidadIdAndMovimientoBancarioIdOrderByIdAsc(
                                33L,
                                328L
                        )
        ).thenReturn(
                List.of(recibo)
        );

        MovimientoBancario resultado =
                service.desconciliarMovimiento(
                        328L,
                        4L,
                        fechaAnulacion
                );

        assertEquals(
                movimiento,
                resultado
        );

        verify(anulacionCobroContableService)
                .anularCobroRecibo(
                        recibo,
                        4L,
                        fechaAnulacion
                );

        verify(recibo).setEstado(
                "PENDIENTE"
        );

        verify(recibo).setFechaCobroBanco(
                null
        );

        verify(recibo).setMovimientoBancarioId(
                null
        );

        verify(recibo).setPagadoAcumulado(
                BigDecimal.ZERO
        );

        verify(reciboRepository).saveAll(
                List.of(recibo)
        );

        verify(movimiento).setConciliado(
                false
        );

        verify(movimiento).setProcesado(
                false
        );

        verify(movimientoBancarioRepository).save(
                movimiento
        );
    }

    @Test
    void desconciliacionRechazaMovimientoNoConciliado() {
        when(
                movimientoBancarioRepository.findById(
                        328L
                )
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(movimiento.getComunidadId())
                .thenReturn(33L);

        when(movimiento.getConciliado())
                .thenReturn(false);

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.desconciliarMovimiento(
                                        328L,
                                        4L,
                                        LocalDate.of(
                                                2026,
                                                7,
                                                30
                                        )
                                )
                );

        assertTrue(
                excepcion.getMessage().contains(
                        "no está conciliado"
                )
        );

        verify(
                reciboRepository,
                never()
        ).findByComunidadIdAndMovimientoBancarioIdOrderByIdAsc(
                33L,
                328L
        );

        verifyNoInteractions(
                anulacionCobroContableService
        );

        verify(
                movimientoBancarioRepository,
                never()
        ).save(movimiento);
    }

    private void prepararMovimientoHaber(
            Long movimientoId,
            Long comunidadId,
            String importe
    ) {
        when(movimiento.getId())
                .thenReturn(movimientoId);

        when(movimiento.getComunidadId())
                .thenReturn(comunidadId);

        when(movimiento.getConciliado())
                .thenReturn(false);

        when(movimiento.getProcesado())
                .thenReturn(false);

        when(movimiento.getImporte())
                .thenReturn(
                        new BigDecimal(importe)
                );

        when(movimiento.getSigno())
                .thenReturn("2");

        when(movimiento.getFechaOperacion())
                .thenReturn(
                        LocalDate.of(2026, 7, 4)
                );
    }

    private void prepararMovimientoConciliado(
            Long movimientoId,
            Long comunidadId
    ) {
        when(movimiento.getId())
                .thenReturn(movimientoId);

        when(movimiento.getComunidadId())
                .thenReturn(comunidadId);

        when(movimiento.getConciliado())
                .thenReturn(true);

        when(movimiento.getProcesado())
                .thenReturn(true);
    }

    private void prepararReciboCobrado(
            Long reciboId,
            Long comunidadId,
            Long movimientoId
    ) {
        when(recibo.getId())
                .thenReturn(reciboId);

        when(recibo.getComunidadId())
                .thenReturn(comunidadId);

        when(recibo.getEstado())
                .thenReturn("COBRADO");

        when(recibo.getMovimientoBancarioId())
                .thenReturn(movimientoId);
    }

    private void prepararReciboPendiente(
            Long reciboId,
            Long comunidadId,
            String importe
    ) {
        when(recibo.getId())
                .thenReturn(reciboId);

        when(recibo.getComunidadId())
                .thenReturn(comunidadId);

        when(recibo.getEstado())
                .thenReturn("PENDIENTE");

        when(recibo.getMovimientoBancarioId())
                .thenReturn(null);

        when(recibo.getImporte())
                .thenReturn(
                        new BigDecimal(importe)
                );
    }
}