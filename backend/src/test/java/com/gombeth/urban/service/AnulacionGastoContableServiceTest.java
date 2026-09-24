package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnulacionGastoContableServiceTest {

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
    private ContabilidadGasto
            gasto;

    @Mock
    private ContabilidadAsiento
            asientoOriginal;

    @Mock
    private ContabilidadAsiento
            asientoAnulacion;

    private AnulacionGastoContableService service;

    @BeforeEach
    void setUp() {
        service =
                new AnulacionGastoContableService(
                        asientoRepository,
                        movimientoRepository,
                        asientoService
                );
    }

    @Test
    void anulaContabilizacionCreandoMovimientosInversos() {

        LocalDate fechaAnulacion =
                LocalDate.of(
                        2026,
                        9,
                        21
                );

        when(
                gasto.getId()
        ).thenReturn(
                5L
        );

        when(
                gasto.getComunidadId()
        ).thenReturn(
                33L
        );

        when(
                gasto.getPagado()
        ).thenReturn(
                false
        );

        when(
                gasto.getNumeroAsiento()
        ).thenReturn(
                "GASTO-5-ASIENTO-10"
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "GASTO_CONTABILIZADO",
                                5L
                        )
        ).thenReturn(
                Optional.of(
                        asientoOriginal
                )
        );

        when(
                asientoOriginal.getId()
        ).thenReturn(
                99L
        );

        when(
                asientoOriginal.getEstado()
        ).thenReturn(
                "CONFIRMADO"
        );

        ContabilidadMovimiento gastoOriginal =
                new ContabilidadMovimiento();

        gastoOriginal.setCuentaId(
                600L
        );

        gastoOriginal.setDebe(
                new BigDecimal(
                        "75.50"
                )
        );

        gastoOriginal.setHaber(
                BigDecimal.ZERO
        );

        ContabilidadMovimiento proveedorOriginal =
                new ContabilidadMovimiento();

        proveedorOriginal.setCuentaId(
                410L
        );

        proveedorOriginal.setDebe(
                BigDecimal.ZERO
        );

        proveedorOriginal.setHaber(
                new BigDecimal(
                        "75.50"
                )
        );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                33L,
                                "ASIENTO-99"
                        )
        ).thenReturn(
                List.of(
                        gastoOriginal,
                        proveedorOriginal
                )
        );

        when(
                asientoService.crearAsientoAutomatico(
                        eq(33L),
                        eq(fechaAnulacion),
                        eq("Anulación gasto 5"),
                        eq("ANULACION_GASTO_CONTABILIZADO"),
                        eq(99L),
                        eq(7L)
                )
        ).thenReturn(
                asientoAnulacion
        );

        when(
                asientoAnulacion.getId()
        ).thenReturn(
                100L
        );

        when(
                asientoAnulacion.getNumeroAsiento()
        ).thenReturn(
                11L
        );

        when(
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                33L,
                                "ASIENTO-100"
                        )
        ).thenReturn(
                false
        );

        service.anularContabilizacion(
                gasto,
                7L,
                fechaAnulacion
        );

        ArgumentCaptor<List<ContabilidadMovimiento>>
                captor =
                ArgumentCaptor.forClass(
                        List.class
                );

        verify(
                movimientoRepository
        ).saveAll(
                captor.capture()
        );

        List<ContabilidadMovimiento> movimientos =
                captor.getValue();

        assertEquals(
                2,
                movimientos.size()
        );

        ContabilidadMovimiento gastoInverso =
                movimientos.get(0);

        assertEquals(
                600L,
                gastoInverso.getCuentaId()
        );

        assertEquals(
                BigDecimal.ZERO,
                gastoInverso.getDebe()
        );

        assertEquals(
                new BigDecimal("75.50"),
                gastoInverso.getHaber()
        );

        assertEquals(
                "ASIENTO-100",
                gastoInverso.getNumeroAsiento()
        );

        ContabilidadMovimiento proveedorInverso =
                movimientos.get(1);

        assertEquals(
                410L,
                proveedorInverso.getCuentaId()
        );

        assertEquals(
                new BigDecimal("75.50"),
                proveedorInverso.getDebe()
        );

        assertEquals(
                BigDecimal.ZERO,
                proveedorInverso.getHaber()
        );

        verify(
                asientoOriginal
        ).setEstado(
                "ANULADO"
        );

        verify(
                asientoRepository
        ).save(
                asientoOriginal
        );
    }

    @Test
    void noAnulaGastoHistoricoSinAsientoV2() {

        when(
                gasto.getId()
        ).thenReturn(
                12L
        );

        when(
                gasto.getComunidadId()
        ).thenReturn(
                16L
        );

        when(
                gasto.getPagado()
        ).thenReturn(
                false
        );

        when(
                gasto.getNumeroAsiento()
        ).thenReturn(
                "FRA-7ED273DD"
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                16L,
                                "GASTO_CONTABILIZADO",
                                12L
                        )
        ).thenReturn(
                Optional.empty()
        );

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.anularContabilizacion(
                                        gasto,
                                        7L,
                                        LocalDate.of(
                                                2026,
                                                9,
                                                21
                                        )
                                )
                );

        assertEquals(
                "No existe el asiento contable "
                        + "del gasto 12.",
                error.getMessage()
        );

        verifyNoInteractions(
                movimientoRepository,
                asientoService
        );
    }

    @Test
    void noAnulaContabilizacionDeGastoPagado() {

        when(
                gasto.getId()
        ).thenReturn(
                5L
        );

        when(
                gasto.getComunidadId()
        ).thenReturn(
                33L
        );

        when(
                gasto.getPagado()
        ).thenReturn(
                true
        );

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.anularContabilizacion(
                                        gasto,
                                        7L,
                                        LocalDate.of(
                                                2026,
                                                9,
                                                21
                                        )
                                )
                );

        assertEquals(
                "No se puede anular la contabilización "
                        + "de un gasto pagado. "
                        + "Primero debe deshacerse el pago.",
                error.getMessage()
        );

        verifyNoInteractions(
                asientoRepository,
                movimientoRepository,
                asientoService
        );
    }

    @Test
    void usaReferenciaHistoricaSiNoExistenMovimientosPorAsiento() {

        LocalDate fechaAnulacion =
                LocalDate.of(
                        2026,
                        9,
                        22
                );

        when(
                gasto.getId()
        ).thenReturn(
                5L
        );

        when(
                gasto.getComunidadId()
        ).thenReturn(
                33L
        );

        when(
                gasto.getNumeroAsiento()
        ).thenReturn(
                "GASTO-5-ASIENTO-10"
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "GASTO_CONTABILIZADO",
                                5L
                        )
        ).thenReturn(
                Optional.of(
                        asientoOriginal
                )
        );

        when(
                asientoOriginal.getId()
        ).thenReturn(
                99L
        );

        ContabilidadMovimiento movimientoHistorico =
                new ContabilidadMovimiento();

        movimientoHistorico.setComunidadId(
                33L
        );

        movimientoHistorico.setCuentaId(
                1957L
        );

        movimientoHistorico.setDebe(
                new BigDecimal(
                        "23.45"
                )
        );

        movimientoHistorico.setHaber(
                BigDecimal.ZERO
        );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                33L,
                                "ASIENTO-99"
                        )
        ).thenReturn(
                List.of()
        );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                33L,
                                "GASTO-5"
                        )
        ).thenReturn(
                List.of(
                        movimientoHistorico
                )
        );

        when(
                asientoService.crearAsientoAutomatico(
                        33L,
                        fechaAnulacion,
                        "Anulación gasto 5",
                        "ANULACION_GASTO_CONTABILIZADO",
                        99L,
                        7L
                )
        ).thenReturn(
                asientoAnulacion
        );

        when(
                asientoAnulacion.getId()
        ).thenReturn(
                100L
        );

        when(
                asientoAnulacion.getNumeroAsiento()
        ).thenReturn(
                20L
        );

        service.anularContabilizacion(
                gasto,
                7L,
                fechaAnulacion
        );

        verify(
                movimientoRepository
        ).findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                33L,
                "ASIENTO-99"
        );

        verify(
                movimientoRepository
        ).findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                33L,
                "GASTO-5"
        );

        verify(
                asientoOriginal
        ).setEstado(
                "ANULADO"
        );
    }
}