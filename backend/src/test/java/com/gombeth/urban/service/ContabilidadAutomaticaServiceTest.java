package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadGastoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContabilidadAutomaticaServiceTest {

    @Mock
    private ContabilidadMovimientoRepository
            movimientoRepository;

    @Mock
    private CuentaContableRepository
            cuentaRepository;

    @Mock
    private ContabilidadReciboRepository
            reciboRepository;

    @Mock
    private MovimientoBancarioRepository
            movimientoBancarioRepository;

    @Mock
    private ContabilidadAsientoService
            asientoService;

    @Mock
    private ContabilidadGastoRepository
            gastoRepository;

    @Mock
    private ContabilidadAsientoRepository
            asientoRepository;

    @Mock
    private ContabilidadRecibo recibo;

    @Mock
    private ContabilidadAsiento asiento;

    @Captor
    private ArgumentCaptor<ContabilidadMovimiento>
            movimientoCaptor;

    @InjectMocks
    private ContabilidadAutomaticaService service;

    @Test
    void creaCobroConReferenciaUnicaDelAsiento() {
        MovimientoBancario movimiento =
                crearMovimientoBancario();

        prepararRecibo();
        prepararCuentas();

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "RECIBO_COBRADO",
                                1554L
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                asientoService.crearAsientoAutomatico(
                        33L,
                        LocalDate.of(2026, 7, 4),
                        "Cobro recibo 1554",
                        "RECIBO_COBRADO",
                        1554L,
                        null
                )
        ).thenReturn(
                asiento
        );

        when(asiento.getId())
                .thenReturn(12L);

        when(asiento.getNumeroAsiento())
                .thenReturn(3L);

        when(
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                33L,
                                "ASIENTO-12"
                        )
        ).thenReturn(false);

        service.registrarCobroRecibo(
                recibo,
                movimiento
        );

        verify(
                movimientoRepository,
                times(2)
        ).save(
                movimientoCaptor.capture()
        );

        List<ContabilidadMovimiento> movimientos =
                movimientoCaptor.getAllValues();

        assertEquals(
                2,
                movimientos.size()
        );

        ContabilidadMovimiento debeBanco =
                movimientos.get(0);

        assertEquals(
                "ASIENTO-12",
                debeBanco.getNumeroAsiento()
        );

        assertEquals(
                1958L,
                debeBanco.getCuentaId()
        );

        assertEquals(
                new BigDecimal("75.50"),
                debeBanco.getDebe()
        );

        assertEquals(
                BigDecimal.ZERO,
                debeBanco.getHaber()
        );

        ContabilidadMovimiento haberDeudores =
                movimientos.get(1);

        assertEquals(
                "ASIENTO-12",
                haberDeudores.getNumeroAsiento()
        );

        assertEquals(
                1961L,
                haberDeudores.getCuentaId()
        );

        assertEquals(
                BigDecimal.ZERO,
                haberDeudores.getDebe()
        );

        assertEquals(
                new BigDecimal("75.50"),
                haberDeudores.getHaber()
        );
    }

    @Test
    void noDuplicaUnCobroHistoricoConfirmado() {
        MovimientoBancario movimiento =
                crearMovimientoBancario();

        prepararRecibo();

        ContabilidadAsiento asientoHistorico =
                org.mockito.Mockito.mock(
                        ContabilidadAsiento.class
                );

        when(asientoHistorico.getId())
                .thenReturn(10L);

        when(asientoHistorico.getEstado())
                .thenReturn("CONFIRMADO");

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "RECIBO_COBRADO",
                                1554L
                        )
        ).thenReturn(
                Optional.of(asientoHistorico)
        );

        when(
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                33L,
                                "ASIENTO-10"
                        )
        ).thenReturn(false);

        when(
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                33L,
                                "COBRO-RECIBO-1554"
                        )
        ).thenReturn(true);

        service.registrarCobroRecibo(
                recibo,
                movimiento
        );

        verify(
                movimientoRepository,
                never()
        ).save(
                org.mockito.ArgumentMatchers.any(
                        ContabilidadMovimiento.class
                )
        );

        verifyNoInteractions(
                cuentaRepository,
                asientoService
        );
    }

    @Test
    void creaNuevoCobroCuandoElAnteriorEstaAnulado() {
        MovimientoBancario movimiento =
                crearMovimientoBancario();

        prepararRecibo();
        prepararCuentas();

        ContabilidadAsiento asientoAnterior =
                org.mockito.Mockito.mock(
                        ContabilidadAsiento.class
                );

        when(asientoAnterior.getEstado())
                .thenReturn("ANULADO");

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "RECIBO_COBRADO",
                                1554L
                        )
        ).thenReturn(
                Optional.of(asientoAnterior)
        );

        when(
                asientoService.crearAsientoAutomatico(
                        33L,
                        LocalDate.of(2026, 7, 4),
                        "Cobro recibo 1554",
                        "RECIBO_COBRADO",
                        1554L,
                        null
                )
        ).thenReturn(
                asiento
        );

        when(asiento.getId())
                .thenReturn(13L);

        when(asiento.getNumeroAsiento())
                .thenReturn(4L);

        when(
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                33L,
                                "ASIENTO-13"
                        )
        ).thenReturn(false);

        service.registrarCobroRecibo(
                recibo,
                movimiento
        );

        verify(
                movimientoRepository,
                times(2)
        ).save(
                movimientoCaptor.capture()
        );

        for (
                ContabilidadMovimiento movimientoCreado
                : movimientoCaptor.getAllValues()
        ) {
            assertEquals(
                    "ASIENTO-13",
                    movimientoCreado.getNumeroAsiento()
            );
        }

        verify(
                movimientoRepository,
                never()
        ).existsByComunidadIdAndNumeroAsiento(
                33L,
                "COBRO-RECIBO-1554"
        );
    }

    private void prepararRecibo() {
        when(recibo.getId())
                .thenReturn(1554L);

        when(recibo.getComunidadId())
                .thenReturn(33L);

        when(recibo.getImporte())
                .thenReturn(
                        new BigDecimal("75.50")
                );
    }

    private void prepararCuentas() {
        CuentaContable cuentaBanco =
                new CuentaContable();

        cuentaBanco.setId(
                1958L
        );

        CuentaContable cuentaDeudores =
                new CuentaContable();

        cuentaDeudores.setId(
                1961L
        );

        when(
                cuentaRepository
                        .findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                                33L,
                                "572"
                        )
        ).thenReturn(
                Optional.of(cuentaBanco)
        );

        when(
                cuentaRepository
                        .findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                                33L,
                                "447"
                        )
        ).thenReturn(
                Optional.of(cuentaDeudores)
        );
    }

    private MovimientoBancario crearMovimientoBancario() {
        MovimientoBancario movimiento =
                new MovimientoBancario();

        movimiento.setFechaOperacion(
                LocalDate.of(
                        2026,
                        7,
                        4
                )
        );

        return movimiento;
    }
}