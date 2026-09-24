package com.gombeth.urban.controller;

import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ContabilidadGastoPdfService;
import com.gombeth.urban.service.ContabilidadGastoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContabilidadGastoPdfControllerTest {

    private ContabilidadGastoService
            gastoService;

    private ContabilidadGastoPdfService
            gastoPdfService;

    private AccesoComunidadService
            accesoComunidadService;

    private Authentication
            authentication;

    private ContabilidadGastoPdfController
            controller;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {

        gastoService =
                mock(
                        ContabilidadGastoService.class
                );

        gastoPdfService =
                mock(
                        ContabilidadGastoPdfService.class
                );

        accesoComunidadService =
                mock(
                        AccesoComunidadService.class
                );

        authentication =
                mock(
                        Authentication.class
                );

        controller =
                new ContabilidadGastoPdfController(
                        gastoService,
                        gastoPdfService,
                        accesoComunidadService
                );
    }

    @Test
    void subePdfTrasValidarAccesoAComunidad() {

        ContabilidadGasto gasto =
                crearGasto(
                        18L
                );

        ContabilidadGasto gastoActualizado =
                crearGasto(
                        18L
                );

        gastoActualizado.setRutaPdf(
                "1776847724991_factura.pdf"
        );

        MockMultipartFile archivo =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "%PDF-1.7 prueba"
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        when(
                gastoService.findById(
                        25L
                )
        ).thenReturn(
                gasto
        );

        when(
                gastoPdfService.subirPdf(
                        25L,
                        archivo
                )
        ).thenReturn(
                gastoActualizado
        );

        ContabilidadGasto resultado =
                controller.subirPdf(
                        25L,
                        archivo,
                        authentication
                );

        assertSame(
                gastoActualizado,
                resultado
        );

        assertEquals(
                "1776847724991_factura.pdf",
                resultado.getRutaPdf()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                18L
        );

        verify(
                gastoPdfService
        ).subirPdf(
                25L,
                archivo
        );
    }

    @Test
    void devuelveConflictSiElGastoYaTienePdf() {

        ContabilidadGasto gasto =
                crearGasto(
                        18L
                );

        MockMultipartFile archivo =
                new MockMultipartFile(
                        "file",
                        "nuevo.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "%PDF-1.7 nuevo"
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        when(
                gastoService.findById(
                        23L
                )
        ).thenReturn(
                gasto
        );

        when(
                gastoPdfService.subirPdf(
                        23L,
                        archivo
                )
        ).thenThrow(
                new IllegalStateException(
                        "El gasto ya tiene un PDF asociado."
                )
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.subirPdf(
                                        23L,
                                        archivo,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        assertEquals(
                "El gasto ya tiene un PDF asociado.",
                error.getReason()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                18L
        );
    }

    @Test
    void visualizaPdfHistoricoInline()
            throws Exception {

        ContabilidadGasto gasto =
                crearGasto(
                        18L
                );

        String nombre =
                "1776847724991_FAT-2026-054412.pdf";

        Path pdf =
                tempDir.resolve(
                        nombre
                );

        byte[] contenido =
                "%PDF-1.4\nPDF HISTORICO"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        Files.write(
                pdf,
                contenido
        );

        when(
                gastoService.findById(
                        23L
                )
        ).thenReturn(
                gasto
        );

        when(
                gastoPdfService.obtenerPdf(
                        23L
                )
        ).thenReturn(
                pdf
        );

        ResponseEntity<Resource> respuesta =
                controller.visualizarPdf(
                        23L,
                        authentication
                );

        assertEquals(
                HttpStatus.OK,
                respuesta.getStatusCode()
        );

        assertEquals(
                MediaType.APPLICATION_PDF,
                respuesta.getHeaders()
                        .getContentType()
        );

        assertEquals(
                contenido.length,
                respuesta.getHeaders()
                        .getContentLength()
        );

        String contentDisposition =
                respuesta.getHeaders()
                        .getFirst(
                                "Content-Disposition"
                        );

        assertNotNull(
                contentDisposition
        );

        assertTrue(
                contentDisposition.startsWith(
                        "inline"
                )
        );

        assertNotNull(
                respuesta.getBody()
        );

        assertEquals(
                nombre,
                respuesta.getBody()
                        .getFilename()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                18L
        );

        verify(
                gastoPdfService
        ).obtenerPdf(
                23L
        );
    }

    @Test
    void devuelveNotFoundSiElGastoNoExiste() {

        when(
                gastoService.findById(
                        999L
                )
        ).thenThrow(
                new IllegalStateException(
                        "No existe el gasto 999"
                )
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.visualizarPdf(
                                        999L,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        assertEquals(
                "No existe el gasto 999",
                error.getReason()
        );

        verify(
                accesoComunidadService,
                never()
        ).validarAcceso(
                any(),
                any()
        );

        verify(
                gastoPdfService,
                never()
        ).obtenerPdf(
                anyLong()
        );
    }

    private ContabilidadGasto crearGasto(
            Long comunidadId
    ) {
        ContabilidadGasto gasto =
                new ContabilidadGasto();

        gasto.setComunidadId(
                comunidadId
        );

        return gasto;
    }
}