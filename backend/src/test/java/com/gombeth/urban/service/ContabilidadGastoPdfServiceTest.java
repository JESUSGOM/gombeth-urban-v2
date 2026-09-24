package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.repository.ContabilidadGastoRepository;
import com.gombeth.urban.service.storage.GastoPdfStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContabilidadGastoPdfServiceTest {

    private ContabilidadGastoRepository
            gastoRepository;

    private GastoPdfStorageService
            gastoPdfStorageService;

    private ContabilidadGastoPdfService
            service;

    @BeforeEach
    void setUp() {

        gastoRepository =
                mock(
                        ContabilidadGastoRepository.class
                );

        gastoPdfStorageService =
                mock(
                        GastoPdfStorageService.class
                );

        service =
                new ContabilidadGastoPdfService(
                        gastoRepository,
                        gastoPdfStorageService
                );
    }

    @AfterEach
    void tearDown() {

        if (
                TransactionSynchronizationManager
                        .isSynchronizationActive()
        ) {
            TransactionSynchronizationManager
                    .clearSynchronization();
        }
    }

    @Test
    void subePdfYGuardaSoloElNombreEnRutaPdf() {

        TransactionSynchronizationManager
                .initSynchronization();

        ContabilidadGasto gasto =
                crearGasto(
                        25L,
                        null
                );

        MockMultipartFile archivo =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        "%PDF-1.7 prueba".getBytes()
                );

        when(
                gastoRepository.findByIdForUpdate(
                        25L
                )
        ).thenReturn(
                Optional.of(
                        gasto
                )
        );

        when(
                gastoPdfStorageService.guardarPdf(
                        archivo
                )
        ).thenReturn(
                "1776847724991_factura.pdf"
        );

        when(
                gastoRepository.save(
                        gasto
                )
        ).thenReturn(
                gasto
        );

        ContabilidadGasto resultado =
                service.subirPdf(
                        25L,
                        archivo
                );

        assertEquals(
                "1776847724991_factura.pdf",
                resultado.getRutaPdf()
        );

        verify(
                gastoPdfStorageService
        ).guardarPdf(
                archivo
        );

        verify(
                gastoRepository
        ).save(
                gasto
        );
    }

    @Test
    void noPermiteReemplazarUnPdfYaAsociado() {

        ContabilidadGasto gasto =
                crearGasto(
                        23L,
                        "1776847724991_FAT-2026-054412.pdf"
                );

        MockMultipartFile archivo =
                new MockMultipartFile(
                        "file",
                        "nuevo.pdf",
                        "application/pdf",
                        "%PDF-1.7 nuevo".getBytes()
                );

        when(
                gastoRepository.findByIdForUpdate(
                        23L
                )
        ).thenReturn(
                Optional.of(
                        gasto
                )
        );

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.subirPdf(
                                        23L,
                                        archivo
                                )
                );

        assertEquals(
                "El gasto ya tiene un PDF asociado.",
                error.getMessage()
        );

        verify(
                gastoPdfStorageService,
                never()
        ).guardarPdf(
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
    void recuperaElPdfHistoricoUsandoRutaPdf() {

        String nombreHistorico =
                "1776847724991_FAT-2026-054412.pdf";

        ContabilidadGasto gasto =
                crearGasto(
                        23L,
                        nombreHistorico
                );

        Path ruta =
                Path.of(
                        "C:/temporal/facturas",
                        nombreHistorico
                );

        when(
                gastoRepository.findById(
                        23L
                )
        ).thenReturn(
                Optional.of(
                        gasto
                )
        );

        when(
                gastoPdfStorageService.obtenerPdf(
                        nombreHistorico
                )
        ).thenReturn(
                ruta
        );

        Path resultado =
                service.obtenerPdf(
                        23L
                );

        assertEquals(
                ruta,
                resultado
        );

        verify(
                gastoPdfStorageService
        ).obtenerPdf(
                nombreHistorico
        );
    }

    @Test
    void indicaSiElGastoTienePdfFisicoDisponible() {

        String nombre =
                "1776847724991_FAT-2026-054412.pdf";

        ContabilidadGasto gasto =
                crearGasto(
                        23L,
                        nombre
                );

        when(
                gastoRepository.findById(
                        23L
                )
        ).thenReturn(
                Optional.of(
                        gasto
                )
        );

        when(
                gastoPdfStorageService.existePdf(
                        nombre
                )
        ).thenReturn(
                true
        );

        boolean resultado =
                service.tienePdf(
                        23L
                );

        assertTrue(
                resultado
        );

        verify(
                gastoPdfStorageService
        ).existePdf(
                nombre
        );
    }

    @Test
    void eliminaPdfRecienCreadoSiLaTransaccionHaceRollback() {

        TransactionSynchronizationManager
                .initSynchronization();

        ContabilidadGasto gasto =
                crearGasto(
                        25L,
                        null
                );

        MockMultipartFile archivo =
                new MockMultipartFile(
                        "file",
                        "factura-rollback.pdf",
                        "application/pdf",
                        "%PDF-1.7 rollback".getBytes()
                );

        String nombreArchivo =
                "1776847724992_factura-rollback.pdf";

        when(
                gastoRepository.findByIdForUpdate(
                        25L
                )
        ).thenReturn(
                Optional.of(
                        gasto
                )
        );

        when(
                gastoPdfStorageService.guardarPdf(
                        archivo
                )
        ).thenReturn(
                nombreArchivo
        );

        when(
                gastoRepository.save(
                        gasto
                )
        ).thenReturn(
                gasto
        );

        service.subirPdf(
                25L,
                archivo
        );

        assertEquals(
                1,
                TransactionSynchronizationManager
                        .getSynchronizations()
                        .size()
        );

        for (
                TransactionSynchronization synchronization
                : TransactionSynchronizationManager
                .getSynchronizations()
        ) {
            synchronization.afterCompletion(
                    TransactionSynchronization
                            .STATUS_ROLLED_BACK
            );
        }

        verify(
                gastoPdfStorageService
        ).eliminarPdfRecienCreado(
                nombreArchivo
        );
    }

    private ContabilidadGasto crearGasto(
            Long id,
            String rutaPdf
    ) {
        ContabilidadGasto gasto =
                new ContabilidadGasto();

        /*
         * La entidad no tiene setId(), así que para estos
         * tests no necesitamos asignarlo: el repositorio
         * simulado ya devuelve el gasto para el id pedido.
         */

        gasto.setRutaPdf(
                rutaPdf
        );

        return gasto;
    }
}