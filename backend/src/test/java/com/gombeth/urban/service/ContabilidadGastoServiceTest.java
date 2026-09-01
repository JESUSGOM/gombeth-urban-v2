package com.gombeth.urban.service;

import com.gombeth.urban.dto.GastoGuardarRequest;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.repository.ContabilidadGastoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContabilidadGastoServiceTest {

    @Mock
    private ContabilidadGastoRepository
            gastoRepository;

    private ContabilidadGastoService service;

    @BeforeEach
    void setUp() {
        service =
                new ContabilidadGastoService(
                        gastoRepository
                );
    }

    @Test
    void creaGastoComoPendienteSinEstadoContableInyectable() {

        GastoGuardarRequest request =
                requestValido(
                        33L
                );

        when(
                gastoRepository.save(
                        any(
                                ContabilidadGasto.class
                        )
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(
                                0
                        )
        );

        ContabilidadGasto creado =
                service.crear(
                        request
                );

        assertEquals(
                33L,
                creado.getComunidadId()
        );

        assertEquals(
                "Proveedor de prueba",
                creado.getProveedor()
        );

        assertEquals(
                new BigDecimal("75.50"),
                creado.getImporteTotal()
        );

        assertFalse(
                Boolean.TRUE.equals(
                        creado.getPagado()
                )
        );

        assertNull(
                creado.getFechaPago()
        );

        assertNull(
                creado.getNumeroAsiento()
        );

        assertNull(
                creado.getRutaPdf()
        );
    }

    @Test
    void rechazaImporteNoValido() {

        GastoGuardarRequest request =
                new GastoGuardarRequest(
                        33L,
                        "Electricidad",
                        LocalDate.of(
                                2026,
                                8,
                                24
                        ),
                        BigDecimal.ZERO,
                        "F-001",
                        "Proveedor",
                        10L
                );

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.crear(
                                        request
                                )
                );

        assertEquals(
                "El importe del gasto debe ser mayor que cero.",
                error.getMessage()
        );

        verify(
                gastoRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void actualizaGastoPendienteNoContabilizado() {

        ContabilidadGasto existente =
                new ContabilidadGasto();

        existente.setComunidadId(
                33L
        );

        existente.setPagado(
                false
        );

        when(
                gastoRepository.findById(
                        5L
                )
        ).thenReturn(
                Optional.of(
                        existente
                )
        );

        when(
                gastoRepository.save(
                        existente
                )
        ).thenReturn(
                existente
        );

        GastoGuardarRequest request =
                requestValido(
                        33L
                );

        ContabilidadGasto actualizado =
                service.actualizar(
                        5L,
                        request
                );

        assertEquals(
                "Electricidad comunidad",
                actualizado.getConcepto()
        );

        assertEquals(
                "F-2026-001",
                actualizado.getNumeroFactura()
        );

        verify(
                gastoRepository
        ).save(
                existente
        );
    }

    @Test
    void impideEditarGastoYaContabilizado() {

        ContabilidadGasto existente =
                new ContabilidadGasto();

        existente.setComunidadId(
                33L
        );

        existente.setPagado(
                false
        );

        existente.setNumeroAsiento(
                "GASTO-5-ASIENTO-10"
        );

        when(
                gastoRepository.findById(
                        5L
                )
        ).thenReturn(
                Optional.of(
                        existente
                )
        );

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.actualizar(
                                        5L,
                                        requestValido(
                                                33L
                                        )
                                )
                );

        assertEquals(
                "No se puede editar un gasto ya contabilizado "
                        + "hasta implementar su reversión "
                        + "contable segura.",
                error.getMessage()
        );

        verify(
                gastoRepository,
                never()
        ).save(
                any()
        );
    }

    private GastoGuardarRequest requestValido(
            Long comunidadId
    ) {
        return new GastoGuardarRequest(
                comunidadId,
                "Electricidad comunidad",
                LocalDate.of(
                        2026,
                        8,
                        24
                ),
                new BigDecimal(
                        "75.50"
                ),
                "F-2026-001",
                "Proveedor de prueba",
                10L
        );
    }
}