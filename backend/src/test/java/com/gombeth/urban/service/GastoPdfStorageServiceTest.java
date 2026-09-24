package com.gombeth.urban.service.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GastoPdfStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void guardaPdfYDevuelveSoloElNombreDelFichero()
            throws Exception {

        GastoPdfStorageService service =
                new GastoPdfStorageService(
                        tempDir.toString()
                );

        byte[] contenido =
                "%PDF-1.7\nPDF DE PRUEBA"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        MockMultipartFile archivo =
                new MockMultipartFile(
                        "file",
                        "factura-prueba.pdf",
                        "application/pdf",
                        contenido
                );

        String nombre =
                service.guardarPdf(
                        archivo
                );

        assertNotNull(
                nombre
        );

        assertTrue(
                nombre.endsWith(
                        "_factura-prueba.pdf"
                )
        );

        assertFalse(
                nombre.contains("/")
        );

        assertFalse(
                nombre.contains("\\")
        );

        Path guardado =
                tempDir.resolve(
                        nombre
                );

        assertTrue(
                Files.isRegularFile(
                        guardado
                )
        );

        assertArrayEquals(
                contenido,
                Files.readAllBytes(
                        guardado
                )
        );
    }

    @Test
    void recuperaUnPdfHistoricoPorSuNombre()
            throws Exception {

        GastoPdfStorageService service =
                new GastoPdfStorageService(
                        tempDir.toString()
                );

        String nombreHistorico =
                "1776847724991_FAT-2026-054412.pdf";

        Path historico =
                tempDir.resolve(
                        nombreHistorico
                );

        Files.write(
                historico,
                "%PDF-1.4\nHISTORICO"
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
        );

        Path recuperado =
                service.obtenerPdf(
                        nombreHistorico
                );

        assertEquals(
                historico.toAbsolutePath()
                        .normalize(),
                recuperado
        );

        assertTrue(
                service.existePdf(
                        nombreHistorico
                )
        );
    }

    @Test
    void rechazaUnArchivoQueNoEsRealmentePdf() {

        GastoPdfStorageService service =
                new GastoPdfStorageService(
                        tempDir.toString()
                );

        MockMultipartFile archivo =
                new MockMultipartFile(
                        "file",
                        "falso.pdf",
                        "application/pdf",
                        "ESTO NO ES UN PDF"
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.guardarPdf(
                                        archivo
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        assertEquals(
                "El contenido del archivo "
                        + "no corresponde a un PDF válido.",
                error.getReason()
        );
    }

    @Test
    void impideSalirDeLaCarpetaDeFacturas() {

        GastoPdfStorageService service =
                new GastoPdfStorageService(
                        tempDir.toString()
                );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.obtenerPdf(
                                        "../secreto.pdf"
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        assertEquals(
                "El nombre del PDF no es válido.",
                error.getReason()
        );

        assertFalse(
                service.existePdf(
                        "../secreto.pdf"
                )
        );
    }
}