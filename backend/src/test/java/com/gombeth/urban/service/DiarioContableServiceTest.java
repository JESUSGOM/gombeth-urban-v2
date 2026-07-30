package com.gombeth.urban.service;

import com.gombeth.urban.dto.DiarioContableDTO;
import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiarioContableServiceTest {

    @Mock
    private ContabilidadAsientoRepository
            asientoRepository;

    @Mock
    private ContabilidadMovimientoRepository
            movimientoRepository;

    @Mock
    private CuentaContableRepository
            cuentaContableRepository;

    @InjectMocks
    private DiarioContableService service;

    @Test
    void obtieneMovimientosConReferenciaUnicaDelAsiento() {
        ContabilidadAsiento asiento =
                crearAsiento(
                        12L,
                        33L,
                        3L,
                        "RECIBO_COBRADO",
                        1554L
                );

        List<ContabilidadMovimiento> movimientos =
                crearMovimientos(
                        "ASIENTO-12"
                );

        when(
                asientoRepository.findById(12L)
        ).thenReturn(
                Optional.of(asiento)
        );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                33L,
                                "ASIENTO-12"
                        )
        ).thenReturn(
                movimientos
        );

        prepararCuentas();

        DiarioContableDTO resultado =
                service.detalle(12L);

        assertSame(
                asiento,
                resultado.getAsiento()
        );

        assertEquals(
                2,
                resultado.getMovimientos().size()
        );

        assertEquals(
                new BigDecimal("75.50"),
                resultado.getTotalDebe()
        );

        assertEquals(
                new BigDecimal("75.50"),
                resultado.getTotalHaber()
        );

        assertTrue(
                resultado.isCuadrado()
        );

        assertEquals(
                "57200001",
                resultado.getMovimientos()
                        .get(0)
                        .getCodigoCuenta()
        );

        verify(
                movimientoRepository,
                never()
        ).findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                33L,
                "COBRO-RECIBO-1554"
        );
    }

    @Test
    void mantieneCompatibilidadConCobroHistorico() {
        ContabilidadAsiento asiento =
                crearAsiento(
                        10L,
                        33L,
                        1L,
                        "RECIBO_COBRADO",
                        1554L
                );

        when(
                asientoRepository.findById(10L)
        ).thenReturn(
                Optional.of(asiento)
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
                crearMovimientos(
                        "COBRO-RECIBO-1554"
                )
        );

        prepararCuentas();

        DiarioContableDTO resultado =
                service.detalle(10L);

        assertEquals(
                2,
                resultado.getMovimientos().size()
        );

        assertEquals(
                "COBRO-RECIBO-1554",
                resultado.getMovimientos()
                        .get(0)
                        .getNumeroAsiento()
        );

        verify(
                movimientoRepository,
                never()
        ).findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                33L,
                "1"
        );
    }

    @Test
    void recuperaElDevengoHistoricoDelRecibo() {
        ContabilidadAsiento asiento =
                crearAsiento(
                        9L,
                        33L,
                        2L,
                        "RECIBO_EMITIDO",
                        1554L
                );

        when(
                asientoRepository.findById(9L)
        ).thenReturn(
                Optional.of(asiento)
        );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                33L,
                                "ASIENTO-9"
                        )
        ).thenReturn(
                List.of()
        );

        when(
                movimientoRepository
                        .findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                                33L,
                                "DEVENGO-RECIBO-1554"
                        )
        ).thenReturn(
                crearMovimientos(
                        "DEVENGO-RECIBO-1554"
                )
        );

        prepararCuentas();

        DiarioContableDTO resultado =
                service.detalle(9L);

        assertEquals(
                2,
                resultado.getMovimientos().size()
        );

        assertEquals(
                "DEVENGO-RECIBO-1554",
                resultado.getMovimientos()
                        .get(0)
                        .getNumeroAsiento()
        );

        verify(
                movimientoRepository,
                never()
        ).findByComunidadIdAndNumeroAsientoOrderByIdAsc(
                33L,
                "2"
        );
    }

    private ContabilidadAsiento crearAsiento(
            Long id,
            Long comunidadId,
            Long numeroAsiento,
            String origen,
            Long origenId
    ) {
        ContabilidadAsiento asiento =
                org.mockito.Mockito.mock(
                        ContabilidadAsiento.class
                );

        when(asiento.getId())
                .thenReturn(id);

        when(asiento.getComunidadId())
                .thenReturn(comunidadId);

        when(asiento.getNumeroAsiento())
                .thenReturn(numeroAsiento);

        when(asiento.getOrigen())
                .thenReturn(origen);

        when(asiento.getOrigenId())
                .thenReturn(origenId);

        return asiento;
    }

    private List<ContabilidadMovimiento> crearMovimientos(
            String numeroAsiento
    ) {
        ContabilidadMovimiento debeBanco =
                new ContabilidadMovimiento();

        debeBanco.setComunidadId(33L);
        debeBanco.setFecha(
                LocalDate.of(2026, 7, 4)
        );
        debeBanco.setNumeroAsiento(
                numeroAsiento
        );
        debeBanco.setConcepto(
                "Movimiento banco"
        );
        debeBanco.setCuentaId(1958L);
        debeBanco.setDebe(
                new BigDecimal("75.50")
        );
        debeBanco.setHaber(
                BigDecimal.ZERO
        );

        ContabilidadMovimiento haberPropietario =
                new ContabilidadMovimiento();

        haberPropietario.setComunidadId(33L);
        haberPropietario.setFecha(
                LocalDate.of(2026, 7, 4)
        );
        haberPropietario.setNumeroAsiento(
                numeroAsiento
        );
        haberPropietario.setConcepto(
                "Movimiento propietario"
        );
        haberPropietario.setCuentaId(1961L);
        haberPropietario.setDebe(
                BigDecimal.ZERO
        );
        haberPropietario.setHaber(
                new BigDecimal("75.50")
        );

        return List.of(
                debeBanco,
                haberPropietario
        );
    }

    private void prepararCuentas() {
        CuentaContable banco =
                new CuentaContable();

        banco.setId(1958L);
        banco.setCodigo("57200001");
        banco.setNombre("Banco Principal c/c");

        CuentaContable propietario =
                new CuentaContable();

        propietario.setId(1961L);
        propietario.setCodigo("43000362");
        propietario.setNombre(
                "PROPIETARIO: JUAN PÉREZ HERNÁNDEZ"
        );

        when(
                cuentaContableRepository.findAllById(
                        List.of(
                                1958L,
                                1961L
                        )
                )
        ).thenReturn(
                List.of(
                        banco,
                        propietario
                )
        );
    }
}