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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnulacionPagoGastoContableServiceTest {

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

    private AnulacionPagoGastoContableService service;

    @BeforeEach
    void setUp() {
        service =
                new AnulacionPagoGastoContableService(
                        asientoRepository,
                        movimientoRepository,
                        asientoService
                );
    }

    @Test
    void anulaPagoCreandoMovimientosInversos() {

        LocalDate fechaAnulacion =
                LocalDate.of(
                        2026,
                        9,
                        18
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
                true
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "GASTO_PAGADO",
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
                "GENERADO"
        );

        ContabilidadMovimiento proveedor =
                new ContabilidadMovimiento();

        proveedor.setCuentaId(
                410L
        );

        proveedor.setDebe(
                new BigDecimal(
                        "75.50"
                )
        );

        proveedor.setHaber(
                BigDecimal.ZERO
        );

        ContabilidadMovimiento banco =
                new ContabilidadMovimiento();

        banco.setCuentaId(
                572L
        );

        banco.setDebe(
                BigDecimal.ZERO
        );

        banco.setHaber(
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
                        proveedor,
                        banco
                )
        );

        when(
                asientoService.crearAsientoAutomatico(
                        eq(33L),
                        eq(fechaAnulacion),
                        eq("Anulación pago gasto 5"),
                        eq("ANULACION_GASTO_PAGADO"),
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
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                33L,
                                "ASIENTO-100"
                        )
        ).thenReturn(
                false
        );

        service.anularPago(
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

        ContabilidadMovimiento proveedorInverso =
                movimientos.get(0);

        assertEquals(
                410L,
                proveedorInverso.getCuentaId()
        );

        assertEquals(
                BigDecimal.ZERO,
                proveedorInverso.getDebe()
        );

        assertEquals(
                new BigDecimal("75.50"),
                proveedorInverso.getHaber()
        );

        assertEquals(
                "ASIENTO-100",
                proveedorInverso.getNumeroAsiento()
        );

        ContabilidadMovimiento bancoInverso =
                movimientos.get(1);

        assertEquals(
                572L,
                bancoInverso.getCuentaId()
        );

        assertEquals(
                new BigDecimal("75.50"),
                bancoInverso.getDebe()
        );

        assertEquals(
                BigDecimal.ZERO,
                bancoInverso.getHaber()
        );

        assertEquals(
                "ASIENTO-100",
                bancoInverso.getNumeroAsiento()
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
    void noAnulaPagoHistoricoSinAsientoV2() {

        LocalDate fechaAnulacion =
                LocalDate.of(
                        2026,
                        9,
                        18
                );

        /*
         * Caso real representativo del programa antiguo:
         *
         * gasto 12
         * comunidad 16
         * pagado = true
         * asiento histórico FRA-7ED273DD
         *
         * No existe un asiento V2 con origen GASTO_PAGADO.
         */
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
                true
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                16L,
                                "GASTO_PAGADO",
                                12L
                        )
        ).thenReturn(
                Optional.empty()
        );

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.anularPago(
                                        gasto,
                                        7L,
                                        fechaAnulacion
                                )
                );

        assertEquals(
                "No existe el asiento contable "
                        + "del pago del gasto 12.",
                error.getMessage()
        );

        /*
         * Importante:
         *
         * si el pago procede del programa antiguo,
         * V2 no debe crear ningún asiento ni ningún
         * movimiento de anulación automáticamente.
         */
        verifyNoInteractions(
                movimientoRepository,
                asientoService
        );
    }
}