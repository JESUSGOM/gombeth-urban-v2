package com.gombeth.urban.service;

import com.gombeth.urban.dto.GastoGuardarRequest;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.entity.Proveedor;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContabilidadGastoServiceTest {

    @Mock
    private ContabilidadGastoRepository
            gastoRepository;

    @Mock
    private ProveedorService
            proveedorService;

    @Mock
    private PagoGastoContableService
            pagoGastoContableService;

    @Mock
    private AnulacionPagoGastoContableService
            anulacionPagoGastoContableService;

    @Mock
    private AnulacionGastoContableService
            anulacionGastoContableService;

    private ContabilidadGastoService service;

    @BeforeEach
    void setUp() {
        service =
                new ContabilidadGastoService(
                        gastoRepository,
                        proveedorService,
                        pagoGastoContableService,
                        anulacionPagoGastoContableService,
                        anulacionGastoContableService
                );
    }

    @Test
    void creaGastoComoPendienteSinEstadoContableInyectable() {

        /*
         * Este test mantiene expresamente el flujo histórico:
         *
         * no se informa proveedorComunidadId y el proveedor
         * continúa llegando como texto libre.
         */
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

        /*
         * En el flujo histórico no debe consultarse
         * el maestro estructurado de proveedores.
         */
        verify(
                proveedorService,
                never()
        ).obtenerProveedorActivoPorAsociacion(
                any(),
                any()
        );
    }

    @Test
    void creaGastoConProveedorAsociadoUsaNombreDelMaestro() {

        GastoGuardarRequest request =
                new GastoGuardarRequest(
                        33L,
                        "Electricidad comunidad",
                        LocalDate.of(
                                2026,
                                9,
                                25
                        ),
                        new BigDecimal(
                                "125.75"
                        ),
                        "FAC-2026-100",
                        "Texto enviado desde el formulario",
                        10L,
                        100L
                );

        Proveedor proveedor =
                new Proveedor();

        proveedor.setNombre(
                "Proveedor Maestro SL"
        );

        when(
                proveedorService
                        .obtenerProveedorActivoPorAsociacion(
                                33L,
                                100L
                        )
        ).thenReturn(
                proveedor
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

        /*
         * Aunque el request traiga un texto de proveedor,
         * cuando existe proveedorComunidadId usamos el nombre
         * validado del maestro.
         */
        assertEquals(
                "Proveedor Maestro SL",
                creado.getProveedor()
        );

        assertEquals(
                33L,
                creado.getComunidadId()
        );

        assertEquals(
                new BigDecimal("125.75"),
                creado.getImporteTotal()
        );

        verify(
                proveedorService
        ).obtenerProveedorActivoPorAsociacion(
                33L,
                100L
        );

        verify(
                gastoRepository
        ).save(
                any(
                        ContabilidadGasto.class
                )
        );
    }

    @Test
    void noGuardaGastoSiAsociacionProveedorNoEsValida() {

        GastoGuardarRequest request =
                new GastoGuardarRequest(
                        33L,
                        "Electricidad comunidad",
                        LocalDate.of(
                                2026,
                                9,
                                25
                        ),
                        new BigDecimal(
                                "125.75"
                        ),
                        "FAC-2026-101",
                        "Proveedor enviado",
                        10L,
                        999L
                );

        when(
                proveedorService
                        .obtenerProveedorActivoPorAsociacion(
                                33L,
                                999L
                        )
        ).thenThrow(
                new IllegalArgumentException(
                        "La asociación de proveedor indicada "
                                + "no pertenece a la comunidad."
                )
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
                "La asociación de proveedor indicada "
                        + "no pertenece a la comunidad.",
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
    void rechazaImporteNoValido() {

        /*
         * Seguimos utilizando el constructor histórico
         * de siete parámetros.
         */
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

        verify(
                proveedorService,
                never()
        ).obtenerProveedorActivoPorAsociacion(
                any(),
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

        assertEquals(
                "Proveedor de prueba",
                actualizado.getProveedor()
        );

        verify(
                gastoRepository
        ).save(
                existente
        );

        verify(
                proveedorService,
                never()
        ).obtenerProveedorActivoPorAsociacion(
                any(),
                any()
        );
    }

    @Test
    void actualizaGastoConProveedorAsociadoUsaNombreDelMaestro() {

        ContabilidadGasto existente =
                new ContabilidadGasto();

        existente.setComunidadId(
                33L
        );

        existente.setProveedor(
                "Proveedor anterior"
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

        Proveedor proveedor =
                new Proveedor();

        proveedor.setNombre(
                "Proveedor Maestro Actualizado SL"
        );

        when(
                proveedorService
                        .obtenerProveedorActivoPorAsociacion(
                                33L,
                                100L
                        )
        ).thenReturn(
                proveedor
        );

        when(
                gastoRepository.save(
                        existente
                )
        ).thenReturn(
                existente
        );

        GastoGuardarRequest request =
                new GastoGuardarRequest(
                        33L,
                        "Electricidad actualizada",
                        LocalDate.of(
                                2026,
                                9,
                                25
                        ),
                        new BigDecimal(
                                "217.71"
                        ),
                        "FAC-2026-200",
                        "Texto distinto enviado",
                        10L,
                        100L
                );

        ContabilidadGasto actualizado =
                service.actualizar(
                        5L,
                        request
                );

        assertEquals(
                "Proveedor Maestro Actualizado SL",
                actualizado.getProveedor()
        );

        assertEquals(
                "Electricidad actualizada",
                actualizado.getConcepto()
        );

        assertEquals(
                "FAC-2026-200",
                actualizado.getNumeroFactura()
        );

        verify(
                proveedorService
        ).obtenerProveedorActivoPorAsociacion(
                33L,
                100L
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

        verify(
                proveedorService,
                never()
        ).obtenerProveedorActivoPorAsociacion(
                any(),
                any()
        );
    }

    @Test
    void pagaGastoContabilizadoYGuardaFechaPago() {

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

        existente.setImporteTotal(
                new BigDecimal(
                        "75.50"
                )
        );

        LocalDate fechaPago =
                LocalDate.of(
                        2026,
                        9,
                        18
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

        ContabilidadGasto pagado =
                service.pagar(
                        5L,
                        7L,
                        fechaPago
                );

        assertTrue(
                Boolean.TRUE.equals(
                        pagado.getPagado()
                )
        );

        assertEquals(
                fechaPago,
                pagado.getFechaPago()
        );

        verify(
                pagoGastoContableService
        ).registrarPago(
                existente,
                7L,
                fechaPago
        );

        verify(
                gastoRepository
        ).save(
                existente
        );
    }

    @Test
    void impidePagarGastoYaPagado() {

        ContabilidadGasto existente =
                new ContabilidadGasto();

        existente.setComunidadId(
                33L
        );

        existente.setPagado(
                true
        );

        existente.setFechaPago(
                LocalDate.of(
                        2026,
                        9,
                        17
                )
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
                                service.pagar(
                                        5L,
                                        7L,
                                        LocalDate.of(
                                                2026,
                                                9,
                                                18
                                        )
                                )
                );

        assertEquals(
                "El gasto ya está pagado.",
                error.getMessage()
        );

        verify(
                pagoGastoContableService,
                never()
        ).registrarPago(
                any(),
                any(),
                any()
        );

        verify(
                gastoRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void deshacePagoYDejaGastoPendiente() {

        ContabilidadGasto existente =
                new ContabilidadGasto();

        existente.setComunidadId(
                33L
        );

        existente.setPagado(
                true
        );

        existente.setFechaPago(
                LocalDate.of(
                        2026,
                        9,
                        17
                )
        );

        existente.setNumeroAsiento(
                "GASTO-5-ASIENTO-10"
        );

        LocalDate fechaAnulacion =
                LocalDate.of(
                        2026,
                        9,
                        18
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

        ContabilidadGasto actualizado =
                service.deshacerPago(
                        5L,
                        7L,
                        fechaAnulacion
                );

        assertFalse(
                Boolean.TRUE.equals(
                        actualizado.getPagado()
                )
        );

        assertNull(
                actualizado.getFechaPago()
        );

        verify(
                anulacionPagoGastoContableService
        ).anularPago(
                existente,
                7L,
                fechaAnulacion
        );

        verify(
                gastoRepository
        ).save(
                existente
        );
    }

    @Test
    void deshaceContabilizacionYDejaGastoPendiente() {

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

        LocalDate fechaAnulacion =
                LocalDate.of(
                        2026,
                        9,
                        21
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

        ContabilidadGasto actualizado =
                service.deshacerContabilizacion(
                        5L,
                        7L,
                        fechaAnulacion
                );

        assertNull(
                actualizado.getNumeroAsiento()
        );

        verify(
                anulacionGastoContableService
        ).anularContabilizacion(
                existente,
                7L,
                fechaAnulacion
        );

        verify(
                gastoRepository
        ).save(
                existente
        );
    }

    @Test
    void eliminaGastoPendienteNoContabilizado() {

        ContabilidadGasto existente =
                new ContabilidadGasto();

        existente.setComunidadId(
                33L
        );

        existente.setPagado(
                false
        );

        existente.setNumeroAsiento(
                null
        );

        existente.setRutaPdf(
                "1776847724992_factura-prueba.pdf"
        );

        when(
                gastoRepository.findByIdForUpdate(
                        5L
                )
        ).thenReturn(
                Optional.of(
                        existente
                )
        );

        service.eliminarPendiente(
                5L
        );

        verify(
                gastoRepository
        ).delete(
                existente
        );
    }

    @Test
    void impideEliminarGastoPagado() {

        ContabilidadGasto existente =
                new ContabilidadGasto();

        existente.setComunidadId(
                33L
        );

        existente.setPagado(
                true
        );

        existente.setNumeroAsiento(
                "GASTO-5-ASIENTO-10"
        );

        when(
                gastoRepository.findByIdForUpdate(
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
                                service.eliminarPendiente(
                                        5L
                                )
                );

        assertEquals(
                "No se puede eliminar un gasto pagado. "
                        + "Primero debe deshacerse el pago.",
                error.getMessage()
        );

        verify(
                gastoRepository,
                never()
        ).delete(
                any()
        );
    }

    @Test
    void impideEliminarGastoContabilizado() {

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
                gastoRepository.findByIdForUpdate(
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
                                service.eliminarPendiente(
                                        5L
                                )
                );

        assertEquals(
                "No se puede eliminar un gasto contabilizado. "
                        + "Primero debe deshacerse su contabilización.",
                error.getMessage()
        );

        verify(
                gastoRepository,
                never()
        ).delete(
                any()
        );
    }

    private GastoGuardarRequest requestValido(
            Long comunidadId
    ) {
        /*
         * Este helper mantiene deliberadamente el constructor
         * histórico de siete parámetros.
         *
         * proveedorComunidadId queda en null.
         */
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