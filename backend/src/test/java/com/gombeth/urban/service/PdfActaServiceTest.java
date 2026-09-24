package com.gombeth.urban.service;

import com.gombeth.urban.entity.Acta;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.EstadoActa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfActaServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generaBorradorConRutaHistorica() throws Exception {

        PdfActaService servicio =
                new PdfActaService(
                        tempDir.toString(),
                        "",
                        ""
                );

        Acta acta =
                crearActa(
                        EstadoActa.BORRADOR
                );

        String rutaGenerada =
                servicio.generarPdfActa(acta);

        Path rutaEsperada =
                tempDir
                        .resolve("comunidad_18")
                        .resolve("Acta_7_Preview.pdf");

        assertEquals(
                rutaEsperada.toString(),
                rutaGenerada
        );

        assertTrue(
                Files.exists(rutaEsperada)
        );

        assertTrue(
                Files.size(rutaEsperada) > 0
        );
    }

    @Test
    void generaActaCerradaConNombreHistoricoFirmado()
            throws Exception {

        PdfActaService servicio =
                new PdfActaService(
                        tempDir.toString(),
                        "",
                        ""
                ) {
                    @Override
                    protected byte[] firmarBytes(
                            byte[] inputPdf
                    ) {
                        return inputPdf;
                    }
                };

        Acta acta =
                crearActa(
                        EstadoActa.CERRADA
                );

        String rutaGenerada =
                servicio.generarPdfActa(acta);

        Path rutaEsperada =
                tempDir
                        .resolve("comunidad_18")
                        .resolve("Acta_7_FIRMADA.pdf");

        assertEquals(
                rutaEsperada.toString(),
                rutaGenerada
        );

        assertTrue(
                Files.exists(rutaEsperada)
        );

        assertTrue(
                Files.size(rutaEsperada) > 0
        );

        try (
                PDDocument documento =
                        Loader.loadPDF(
                                Files.readAllBytes(
                                        rutaEsperada
                                )
                        )
        ) {

            String texto =
                    new PDFTextStripper()
                            .getText(documento);

            assertFalse(
                    texto.contains("BORRADOR"),
                    "Un acta cerrada no debe contener "
                            + "la marca BORRADOR."
            );
        }
    }

    private Acta crearActa(
            EstadoActa estado
    ) {

        Comunidad comunidad =
                new Comunidad();

        comunidad.setId(18L);
        comunidad.setNombre(
                "Comunidad de Propietarios Test"
        );

        Acta acta =
                new Acta();

        acta.setId(7L);
        acta.setComunidad(comunidad);
        acta.setTitulo(
                "Junta General Ordinaria"
        );
        acta.setFechaReunion(
                LocalDate.of(2026, 9, 23)
        );
        acta.setContenido(
                "<p>Contenido de prueba del acta.</p>"
        );
        acta.setEstado(estado);

        return acta;
    }

    @Test
    void generaPdfConEntidadesHtmlHistoricas()
            throws Exception {

        PdfActaService servicio =
                new PdfActaService(
                        tempDir.toString(),
                        "",
                        ""
                );

        Acta acta =
                crearActa(
                        EstadoActa.BORRADOR
                );

        acta.setContenido(
                "<p>"
                        + "Reuni&oacute;n&nbsp;"
                        + "de la comunidad: "
                        + "&aacute; &eacute; "
                        + "&iacute; &oacute; "
                        + "&uacute; &ntilde; "
                        + "&euro;"
                        + "</p>"
        );

        String ruta =
                servicio.generarPdfActa(
                        acta
                );

        Path pdf =
                Path.of(ruta);

        assertTrue(
                Files.exists(pdf)
        );

        assertTrue(
                Files.size(pdf) > 0
        );
    }

    @Test
    void obtienePdfFirmadoExistenteSinRegenerarlo()
            throws Exception {

        PdfActaService servicio =
                new PdfActaService(
                        tempDir.toString(),
                        "",
                        ""
                ) {
                    @Override
                    protected byte[] firmarBytes(
                            byte[] inputPdf
                    ) {
                        throw new AssertionError(
                                "No debe volver a firmarse "
                                        + "un PDF cerrado ya existente."
                        );
                    }
                };

        Acta acta =
                crearActa(
                        EstadoActa.CERRADA
                );

        Path carpeta =
                tempDir.resolve(
                        "comunidad_18"
                );

        Files.createDirectories(
                carpeta
        );

        Path pdfExistente =
                carpeta.resolve(
                        "Acta_7_FIRMADA.pdf"
                );

        byte[] contenidoOriginal =
                "PDF YA FIRMADO".getBytes();

        Files.write(
                pdfExistente,
                contenidoOriginal
        );

        Path obtenido =
                servicio.obtenerPdf(
                        acta
                );

        assertEquals(
                pdfExistente,
                obtenido
        );

        assertTrue(
                Files.exists(obtenido)
        );

        assertEquals(
                "PDF YA FIRMADO",
                Files.readString(obtenido)
        );
    }

    @Test
    void eliminaPdfFirmadoGenerado()
            throws Exception {

        PdfActaService servicio =
                new PdfActaService(
                        tempDir.toString(),
                        "",
                        ""
                ) {
                    @Override
                    protected byte[] firmarBytes(
                            byte[] inputPdf
                    ) {
                        return inputPdf;
                    }
                };

        Acta acta =
                crearActa(
                        EstadoActa.CERRADA
                );

        Path ruta =
                Path.of(
                        servicio.generarPdfActa(
                                acta
                        )
                );

        assertTrue(
                Files.exists(
                        ruta
                )
        );

        servicio.eliminarPdfFirmadoGenerado(
                acta
        );

        assertFalse(
                Files.exists(
                        ruta
                )
        );
    }
}