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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConciliacionBancariaUsuarioServiceTest {

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
    void conciliacionManualPropagaElUsuarioAlAsientoDeCobro() {
        when(
                movimientoBancarioRepository.findById(
                        328L
                )
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(movimiento.getId())
                .thenReturn(328L);

        when(movimiento.getComunidadId())
                .thenReturn(33L);

        when(movimiento.getConciliado())
                .thenReturn(false);

        when(movimiento.getProcesado())
                .thenReturn(false);

        when(movimiento.getImporte())
                .thenReturn(
                        new BigDecimal("75.50")
                );

        when(movimiento.getSigno())
                .thenReturn("2");

        when(movimiento.getFechaOperacion())
                .thenReturn(
                        LocalDate.of(
                                2026,
                                7,
                                4
                        )
                );

        when(
                reciboRepository.findByIdIn(
                        List.of(1554L)
                )
        ).thenReturn(
                List.of(recibo)
        );

        when(recibo.getId())
                .thenReturn(1554L);

        when(recibo.getComunidadId())
                .thenReturn(33L);

        when(recibo.getEstado())
                .thenReturn("PENDIENTE");

        when(recibo.getMovimientoBancarioId())
                .thenReturn(null);

        when(recibo.getImporte())
                .thenReturn(
                        new BigDecimal("75.50")
                );

        MovimientoBancario resultado =
                service.conciliarMovimientoConRecibos(
                        328L,
                        List.of(1554L),
                        4L
                );

        assertSame(
                movimiento,
                resultado
        );

        verify(contabilidadAutomaticaService)
                .registrarCobroRecibo(
                        recibo,
                        movimiento,
                        4L
                );

        verify(reciboRepository).saveAll(
                List.of(recibo)
        );

        verify(movimientoBancarioRepository).save(
                movimiento
        );
    }
}