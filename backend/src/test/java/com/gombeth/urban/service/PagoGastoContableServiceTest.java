package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoGastoContableServiceTest {

    @Mock
    private ContabilidadAsientoRepository
            asientoRepository;

    @Mock
    private ContabilidadMovimientoRepository
            movimientoRepository;

    @Mock
    private CuentaContableRepository
            cuentaRepository;

    @Mock
    private ContabilidadAsientoService
            asientoService;

    @Mock
    private ContabilidadGasto
            gasto;

    @Mock
    private CuentaContable
            cuentaProveedor;

    @Mock
    private CuentaContable
            cuentaBanco;

    @Mock
    private ContabilidadAsiento
            asiento;

    private PagoGastoContableService service;

    @BeforeEach
    void setUp() {
        service =
                new PagoGastoContableService(
                        asientoRepository,
                        movimientoRepository,
                        cuentaRepository,
                        asientoService
                );
    }

    @Test
    void registraPagoConLaMisma410DeLaContabilizacion() {

        LocalDate fechaPago =
                LocalDate.of(
                        2026,
                        9,
                        18
                );

        BigDecimal importe =
                new BigDecimal(
                        "75.50"
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
                gasto.getImporteTotal()
        ).thenReturn(
                importe
        );

        when(
                gasto.getNumeroAsiento()
        ).thenReturn(
                "GASTO-5-ASIENTO-10"
        );

        when(
                gasto.getPagado()
        ).thenReturn(
                false
        );

        when(
                gasto.getProveedor()
        ).thenReturn(
                "Proveedor de prueba"
        );

        when(
                gasto.getNumeroFactura()
        ).thenReturn(
                "F-2026-001"
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "GASTO_PAGADO",
                                5L
                        )
        ).thenReturn(
                Optional.empty()
        );

        ContabilidadAsiento asientoContabilizacion =
                org.mockito.Mockito.mock(
                        ContabilidadAsiento.class
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
                        asientoContabilizacion
                )
        );

        when(
                asientoContabilizacion.getId()
        ).thenReturn(
                77L
        );

        /*
         * Movimiento original de la contabilización:
         *
         * 6xxxxx DEBE
         * 41003892 HABER
         *
         * Para localizar la cuenta del proveedor nos interesa
         * precisamente la línea del HABER.
         */
        ContabilidadMovimiento movimientoProveedor =
                new ContabilidadMovimiento();

        movimientoProveedor.setComunidadId(
                33L
        );

        movimientoProveedor.setNumeroAsiento(
                "ASIENTO-77"
        );

        movimientoProveedor.setCuentaId(
                3000L
        );

        movimientoProveedor.setDebe(
                BigDecimal.ZERO
        );

        movimientoProveedor.setHaber(
                importe
        );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                33L,
                                "ASIENTO-77"
                        )
        ).thenReturn(
                List.of(
                        movimientoProveedor
                )
        );

        /*
         * La cuenta 3000 es la 410 concreta utilizada cuando
         * se contabilizó la factura.
         */
        when(
                cuentaRepository
                        .findByIdAndComunidad_Id(
                                3000L,
                                33L
                        )
        ).thenReturn(
                Optional.of(
                        cuentaProveedor
                )
        );

        when(
                cuentaProveedor.getId()
        ).thenReturn(
                3000L
        );

        when(
                cuentaProveedor.getCodigo()
        ).thenReturn(
                "41003892"
        );

        /*
         * La 572 se sigue resolviendo por prefijo porque es
         * la cuenta bancaria de la comunidad.
         */
        when(
                cuentaRepository
                        .findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                                33L,
                                "572"
                        )
        ).thenReturn(
                Optional.of(
                        cuentaBanco
                )
        );

        when(
                cuentaBanco.getId()
        ).thenReturn(
                572L
        );

        when(
                asientoService.crearAsientoAutomatico(
                        eq(33L),
                        eq(fechaPago),
                        eq(
                                "Pago factura proveedor "
                                        + "Proveedor de prueba"
                        ),
                        eq("GASTO_PAGADO"),
                        eq(5L),
                        eq(7L)
                )
        ).thenReturn(
                asiento
        );

        when(
                asiento.getId()
        ).thenReturn(
                99L
        );

        when(
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                33L,
                                "ASIENTO-99"
                        )
        ).thenReturn(
                false
        );

        service.registrarPago(
                gasto,
                7L,
                fechaPago
        );

        /*
         * Esta comprobación es fundamental:
         *
         * PagoGastoContableService YA NO puede buscar
         * simplemente "la primera 410".
         */
        verify(
                cuentaRepository,
                never()
        ).findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                33L,
                "410"
        );

        verify(
                movimientoRepository
        ).findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                33L,
                "ASIENTO-77"
        );

        verify(
                cuentaRepository
        ).findByIdAndComunidad_Id(
                3000L,
                33L
        );

        ArgumentCaptor<ContabilidadMovimiento>
                captor =
                ArgumentCaptor.forClass(
                        ContabilidadMovimiento.class
                );

        verify(
                movimientoRepository,
                times(2)
        ).save(
                captor.capture()
        );

        List<ContabilidadMovimiento> movimientos =
                captor.getAllValues();

        assertEquals(
                2,
                movimientos.size()
        );

        /*
         * Pago:
         *
         * DEBE 41003892
         */
        ContabilidadMovimiento debeProveedor =
                movimientos.get(0);

        assertEquals(
                3000L,
                debeProveedor.getCuentaId()
        );

        assertEquals(
                importe,
                debeProveedor.getDebe()
        );

        assertEquals(
                BigDecimal.ZERO,
                debeProveedor.getHaber()
        );

        assertEquals(
                fechaPago,
                debeProveedor.getFecha()
        );

        assertEquals(
                "ASIENTO-99",
                debeProveedor.getNumeroAsiento()
        );

        /*
         * HABER 572
         */
        ContabilidadMovimiento haberBanco =
                movimientos.get(1);

        assertEquals(
                572L,
                haberBanco.getCuentaId()
        );

        assertEquals(
                BigDecimal.ZERO,
                haberBanco.getDebe()
        );

        assertEquals(
                importe,
                haberBanco.getHaber()
        );

        assertEquals(
                fechaPago,
                haberBanco.getFecha()
        );

        assertEquals(
                "ASIENTO-99",
                haberBanco.getNumeroAsiento()
        );
    }

    @Test
    void pagaGastoHistoricoUsandoLa410DeSuAsientoLegacy() {

        LocalDate fechaPago =
                LocalDate.of(
                        2026,
                        9,
                        18
                );

        BigDecimal importe =
                new BigDecimal(
                        "82.82"
                );

        when(
                gasto.getId()
        ).thenReturn(
                22L
        );

        when(
                gasto.getComunidadId()
        ).thenReturn(
                17L
        );

        when(
                gasto.getImporteTotal()
        ).thenReturn(
                importe
        );

        when(
                gasto.getNumeroAsiento()
        ).thenReturn(
                "FRA-E8AD54A3"
        );

        when(
                gasto.getPagado()
        ).thenReturn(
                false
        );

        when(
                gasto.getProveedor()
        ).thenReturn(
                "María Bueviaje Martín"
        );

        when(
                gasto.getNumeroFactura()
        ).thenReturn(
                "37"
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                17L,
                                "GASTO_PAGADO",
                                22L
                        )
        ).thenReturn(
                Optional.empty()
        );

        /*
         * V2 intenta primero su referencia moderna.
         * Al tratarse de un gasto histórico no existe.
         */
        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                17L,
                                "GASTO-22"
                        )
        ).thenReturn(
                List.of()
        );

        /*
         * Movimientos reales del programa antiguo:
         *
         * 628... DEBE 82,82
         * 41014130 HABER 82,82
         *
         * Ambos con referencia FRA-E8AD54A3.
         */
        ContabilidadMovimiento movimientoGasto =
                new ContabilidadMovimiento();

        movimientoGasto.setComunidadId(
                17L
        );

        movimientoGasto.setNumeroAsiento(
                "FRA-E8AD54A3"
        );

        movimientoGasto.setCuentaId(
                1862L
        );

        movimientoGasto.setDebe(
                importe
        );

        movimientoGasto.setHaber(
                BigDecimal.ZERO
        );

        ContabilidadMovimiento movimientoProveedor =
                new ContabilidadMovimiento();

        movimientoProveedor.setComunidadId(
                17L
        );

        movimientoProveedor.setNumeroAsiento(
                "FRA-E8AD54A3"
        );

        movimientoProveedor.setCuentaId(
                1890L
        );

        movimientoProveedor.setDebe(
                BigDecimal.ZERO
        );

        movimientoProveedor.setHaber(
                importe
        );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                17L,
                                "FRA-E8AD54A3"
                        )
        ).thenReturn(
                List.of(
                        movimientoGasto,
                        movimientoProveedor
                )
        );

        /*
         * 1890 = 41014130
         * PROV: María Bueviaje Martín
         */
        when(
                cuentaRepository
                        .findByIdAndComunidad_Id(
                                1890L,
                                17L
                        )
        ).thenReturn(
                Optional.of(
                        cuentaProveedor
                )
        );

        when(
                cuentaProveedor.getId()
        ).thenReturn(
                1890L
        );

        when(
                cuentaProveedor.getCodigo()
        ).thenReturn(
                "41014130"
        );

        /*
         * Banco histórico real de comunidad 17.
         */
        when(
                cuentaRepository
                        .findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                                17L,
                                "572"
                        )
        ).thenReturn(
                Optional.of(
                        cuentaBanco
                )
        );

        when(
                cuentaBanco.getId()
        ).thenReturn(
                1869L
        );

        when(
                asientoService.crearAsientoAutomatico(
                        eq(17L),
                        eq(fechaPago),
                        eq(
                                "Pago factura proveedor "
                                        + "María Bueviaje Martín"
                        ),
                        eq("GASTO_PAGADO"),
                        eq(22L),
                        eq(7L)
                )
        ).thenReturn(
                asiento
        );

        when(
                asiento.getId()
        ).thenReturn(
                100L
        );

        when(
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                17L,
                                "ASIENTO-100"
                        )
        ).thenReturn(
                false
        );

        service.registrarPago(
                gasto,
                7L,
                fechaPago
        );

        /*
         * Debe haber intentado primero el formato nuevo...
         */
        verify(
                movimientoRepository
        ).findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                17L,
                "GASTO-22"
        );

        /*
         * ...y después recuperar correctamente el histórico.
         */
        verify(
                movimientoRepository
        ).findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                17L,
                "FRA-E8AD54A3"
        );

        /*
         * Nunca debe buscar simplemente la primera 410.
         */
        verify(
                cuentaRepository,
                never()
        ).findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                17L,
                "410"
        );

        ArgumentCaptor<ContabilidadMovimiento>
                captor =
                ArgumentCaptor.forClass(
                        ContabilidadMovimiento.class
                );

        verify(
                movimientoRepository,
                times(2)
        ).save(
                captor.capture()
        );

        List<ContabilidadMovimiento> movimientosPago =
                captor.getAllValues();

        /*
         * DEBE 41014130
         */
        ContabilidadMovimiento debeProveedor =
                movimientosPago.get(0);

        assertEquals(
                1890L,
                debeProveedor.getCuentaId()
        );

        assertEquals(
                importe,
                debeProveedor.getDebe()
        );

        assertEquals(
                BigDecimal.ZERO,
                debeProveedor.getHaber()
        );

        /*
         * HABER 57200001
         */
        ContabilidadMovimiento haberBanco =
                movimientosPago.get(1);

        assertEquals(
                1869L,
                haberBanco.getCuentaId()
        );

        assertEquals(
                BigDecimal.ZERO,
                haberBanco.getDebe()
        );

        assertEquals(
                importe,
                haberBanco.getHaber()
        );
    }
}