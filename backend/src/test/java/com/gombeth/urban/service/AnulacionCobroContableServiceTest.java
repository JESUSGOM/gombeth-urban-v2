package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnulacionCobroContableServiceTest {

    @Mock
    private ContabilidadAsientoRepository
            asientoRepository;

    @Mock
    private ContabilidadMovimientoRepository
            movimientoRepository;

    @Mock
    private ContabilidadAsientoService
            asientoService;

    @Mock
    private ContabilidadRecibo recibo;

    @Mock
    private ContabilidadAsiento asientoOriginal;

    @Mock
    private ContabilidadAsiento asientoAnulacion;

    @Captor
    private ArgumentCaptor<List<ContabilidadMovimiento>>
            movimientosCaptor;

    @InjectMocks
    private AnulacionCobroContableService service;

    @Test
    void creaContrasientoHistoricoYMarcaOriginalAnulado() {
        LocalDate fechaAnulacion =
                LocalDate.of(
                        2026,
                        7,
                        30
                );

        when(recibo.getId())
                .thenReturn(1554L);

        when(recibo.getComunidadId())
                .thenReturn(33L);

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "RECIBO_COBRADO",
                                1554L
                        )
        ).thenReturn(
                Optional.of(asientoOriginal)
        );

        when(asientoOriginal.getId())
                .thenReturn(10L);

        when(asientoOriginal.getEstado())
                .thenReturn("CONFIRMADO");

        ContabilidadMovimiento debeBanco =
                crearMovimiento(
                        1958L,
                        "75.50",
                        "0.00"
                );

        ContabilidadMovimiento haberPropietario =
                crearMovimiento(
                        1961L,
                        "0.00",
                        "75.50"
                );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                33L,
                                "ASIENTO-10"
                        )
        ).thenReturn(
                List.of()
        );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                33L,
                                "COBRO-RECIBO-1554"
                        )
        ).thenReturn(
                List.of(
                        debeBanco,
                        haberPropietario
                )
        );

        when(
                asientoService.crearAsientoAutomatico(
                        33L,
                        fechaAnulacion,
                        "Anulación cobro recibo 1554",
                        "ANULACION_RECIBO_COBRADO",
                        10L,
                        4L
                )
        ).thenReturn(
                asientoAnulacion
        );

        when(asientoAnulacion.getId())
                .thenReturn(11L);

        when(asientoAnulacion.getNumeroAsiento())
                .thenReturn(2L);

        when(
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                33L,
                                "ASIENTO-11"
                        )
        ).thenReturn(false);

        ContabilidadAsiento resultado =
                service.anularCobroRecibo(
                        recibo,
                        4L,
                        fechaAnulacion
                );

        assertSame(
                asientoAnulacion,
                resultado
        );

        verify(
                movimientoRepository
        ).saveAll(
                movimientosCaptor.capture()
        );

        List<ContabilidadMovimiento> inversos =
                movimientosCaptor.getValue();

        assertEquals(
                2,
                inversos.size()
        );

        ContabilidadMovimiento inversoBanco =
                inversos.get(0);

        assertEquals(
                1958L,
                inversoBanco.getCuentaId()
        );

        assertEquals(
                new BigDecimal("0.00"),
                inversoBanco.getDebe()
        );

        assertEquals(
                new BigDecimal("75.50"),
                inversoBanco.getHaber()
        );

        assertEquals(
                "ASIENTO-11",
                inversoBanco.getNumeroAsiento()
        );

        assertEquals(
                fechaAnulacion,
                inversoBanco.getFecha()
        );

        ContabilidadMovimiento inversoPropietario =
                inversos.get(1);

        assertEquals(
                1961L,
                inversoPropietario.getCuentaId()
        );

        assertEquals(
                new BigDecimal("75.50"),
                inversoPropietario.getDebe()
        );

        assertEquals(
                new BigDecimal("0.00"),
                inversoPropietario.getHaber()
        );

        assertEquals(
                "ASIENTO-11",
                inversoPropietario.getNumeroAsiento()
        );

        verify(asientoOriginal).setEstado(
                "ANULADO"
        );

        verify(asientoRepository).save(
                asientoOriginal
        );
    }

    @Test
    void rechazaCobroQueYaEstaAnulado() {
        when(recibo.getId())
                .thenReturn(1554L);

        when(recibo.getComunidadId())
                .thenReturn(33L);

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "RECIBO_COBRADO",
                                1554L
                        )
        ).thenReturn(
                Optional.of(asientoOriginal)
        );

        when(asientoOriginal.getEstado())
                .thenReturn("ANULADO");

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.anularCobroRecibo(
                                        recibo,
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
                        "ya está anulado"
                )
        );

        verify(
                asientoRepository,
                never()
        ).save(
                asientoOriginal
        );

        verifyNoInteractions(
                movimientoRepository,
                asientoService
        );
    }

    private ContabilidadMovimiento crearMovimiento(
            Long cuentaId,
            String debe,
            String haber
    ) {
        ContabilidadMovimiento movimiento =
                new ContabilidadMovimiento();

        movimiento.setComunidadId(
                33L
        );

        movimiento.setCuentaId(
                cuentaId
        );

        movimiento.setDebe(
                new BigDecimal(debe)
        );

        movimiento.setHaber(
                new BigDecimal(haber)
        );

        return movimiento;
    }
}