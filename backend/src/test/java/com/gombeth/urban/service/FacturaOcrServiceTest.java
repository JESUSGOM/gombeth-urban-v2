package com.gombeth.urban.service;

import com.gombeth.urban.dto.FacturaOcrResultado;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Assumptions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FacturaOcrServiceTest {

    @Test
    void usaTextoDelPdfSinEjecutarOcrCuandoHayTextoSuficiente()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "FACTURA DE PRUEBA F-12345 IMPORTE 25,00 EUR"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura-prueba.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertNotNull(resultado);

        assertFalse(
                resultado.ocrAplicado()
        );

        assertNull(
                resultado.proveedor()
        );

        assertNull(
                resultado.fechaFactura()
        );

        assertNull(
                resultado.importeTotal()
        );

        assertNull(
                resultado.numeroFactura()
        );

        assertTrue(
                resultado.advertencias().isEmpty()
        );
    }

    @Test
    void extraeNumeroDeFactura()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "Proveedor de prueba "
                                + "Numero factura: FAT-2026-054412 "
                                + "Fecha factura: 15/09/2026 "
                                + "Importe total: 217,71 EUR"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura-numero.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertFalse(
                resultado.ocrAplicado()
        );

        assertEquals(
                "FAT-2026-054412",
                resultado.numeroFactura()
        );
    }

    @Test
    void extraeProveedorDeUnaFactura()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "Proveedor: ATENCO ENERGIA SL "
                                + "Numero factura: FAT-2026-054412 "
                                + "Fecha factura: 15/09/2026 "
                                + "Importe total: 217,71 EUR"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura-proveedor.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertFalse(
                resultado.ocrAplicado()
        );

        assertEquals(
                "ATENCO ENERGIA SL",
                resultado.proveedor()
        );
    }

    @Test
    void aplicaOcrAUnPdfEscaneado()
            throws Exception {

        String tessdata =
                "C:/Program Files/Tesseract-OCR/tessdata";

        Assumptions.assumeTrue(
                Files.exists(
                        Path.of(
                                tessdata,
                                "spa.traineddata"
                        )
                ),
                "Tesseract español no está instalado."
        );

        FacturaOcrService service =
                new FacturaOcrService(
                        tessdata,
                        "spa"
                );

        byte[] pdf =
                crearPdfEscaneado();

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura-escaneada.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertTrue(
                resultado.ocrAplicado()
        );

        assertEquals(
                "ATENCO ENERGIA SL",
                resultado.proveedor()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        9,
                        15
                ),
                resultado.fechaFactura()
        );

        assertEquals(
                new BigDecimal("217.71"),
                resultado.importeTotal()
        );

        assertEquals(
                "FAT-2026-054412",
                resultado.numeroFactura()
        );
    }

    @Test
    void rechazaUnFicheroQueNoSeaPdf() {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.txt",
                        "text/plain",
                        "esto no es un pdf".getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.analizarFactura(
                                fichero
                        )
                );

        assertEquals(
                "El fichero debe ser un PDF.",
                error.getMessage()
        );
    }

    private byte[] crearPdfConTexto(
            String texto
    ) throws Exception {

        try (
                PDDocument documento =
                        new PDDocument();

                ByteArrayOutputStream salida =
                        new ByteArrayOutputStream()
        ) {
            PDPage pagina =
                    new PDPage();

            documento.addPage(
                    pagina
            );

            try (
                    PDPageContentStream contenido =
                            new PDPageContentStream(
                                    documento,
                                    pagina
                            )
            ) {
                contenido.beginText();

                contenido.setFont(
                        new PDType1Font(
                                Standard14Fonts.FontName.HELVETICA
                        ),
                        12
                );

                contenido.newLineAtOffset(
                        50,
                        700
                );

                contenido.showText(
                        texto
                );

                contenido.endText();
            }

            documento.save(
                    salida
            );

            return salida.toByteArray();
        }
    }

    private byte[] crearPdfEscaneado()
            throws Exception {

        BufferedImage imagen =
                new BufferedImage(
                        1800,
                        1000,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                imagen.createGraphics();

        try {
            graphics.setColor(
                    Color.WHITE
            );

            graphics.fillRect(
                    0,
                    0,
                    imagen.getWidth(),
                    imagen.getHeight()
            );

            graphics.setColor(
                    Color.BLACK
            );

            graphics.setFont(
                    new Font(
                            "Arial",
                            Font.PLAIN,
                            52
                    )
            );

            graphics.drawString(
                    "Proveedor: ATENCO ENERGIA SL",
                    100,
                    220
            );

            graphics.drawString(
                    "Numero factura: FAT-2026-054412",
                    100,
                    360
            );

            graphics.drawString(
                    "Fecha factura: 15/09/2026",
                    100,
                    500
            );

            graphics.drawString(
                    "Importe total: 217,71 EUR",
                    100,
                    640
            );

        } finally {
            graphics.dispose();
        }

        try (
                PDDocument documento =
                        new PDDocument();

                ByteArrayOutputStream salida =
                        new ByteArrayOutputStream()
        ) {
            PDPage pagina =
                new PDPage(
                    new PDRectangle(
                    720,
                    400
                    )
                );

            documento.addPage(
                pagina
            );

            PDImageXObject imagenPdf =
                LosslessFactory.createFromImage(
                    documento,
                    imagen
                );

            try (
                    PDPageContentStream contenido =
                            new PDPageContentStream(
                                    documento,
                                    pagina
                            )
            ) {
                contenido.drawImage(
                        imagenPdf,
                        0,
                        0,
                        pagina.getMediaBox().getWidth(),
                        pagina.getMediaBox().getHeight()
                );
            }

            documento.save(
                    salida
            );

            return salida.toByteArray();
        }
    }

    @Test
    void extraeFechaEImporteDeUnaFacturaConTexto()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "FACTURA F-2026-001 "
                                + "Fecha factura: 15/09/2026 "
                                + "Importe total: 1.234,56 EUR"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura-con-datos.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertFalse(
                resultado.ocrAplicado()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        9,
                        15
                ),
                resultado.fechaFactura()
        );

        assertEquals(
                new BigDecimal("1234.56"),
                resultado.importeTotal()
        );
    }
    @Test
    void rechazaUnFicheroConExtensionPdfPeroContenidoNoPdf() {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        "esto realmente no es un PDF".getBytes()
                );

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.analizarFactura(
                                        fichero
                                )
                );

        assertEquals(
                "El contenido del fichero no corresponde a un PDF válido.",
                error.getMessage()
        );
    }

    @Test
    void noAceptaUnaPalabraSinDigitosComoNumeroDeFactura()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "Numero factura: FECHA"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertNull(
                resultado.numeroFactura()
        );
    }

    @Test
    void extraeImporteConFormatoImporteFactura()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "IMPORTE FACTURA: 217,71 €"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertEquals(
                new BigDecimal("217.71"),
                resultado.importeTotal()
        );
    }

    @Test
    void extraeNumeroFacturaCuandoComparteCabeceraConFecha()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "Numero factura: "
                                + "Fecha de emision de factura: "
                                + "FAT-2026-054412"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertEquals(
                "FAT-2026-054412",
                resultado.numeroFactura()
        );
    }

    @Test
    void extraeFechaEspañolaDeEmision()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "Fecha de emision de factura: "
                                + "FAT-2026-054412 "
                                + "7 de abril de 2026"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertEquals(
                LocalDate.of(
                        2026,
                        4,
                        7
                ),
                resultado.fechaFactura()
        );
    }

    @Test
    void extraeProveedorSituadoAntesDelCif()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "ATENCO ENERGIA, SL CIF: B76366723"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertEquals(
                "ATENCO ENERGIA, SL",
                resultado.proveedor()
        );
    }

    @Test
    void noIncluyeNumeroNiFechaEnElProveedor()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "ATENCO ENERGIA, SL "
                                + "FAT-2026-054412 "
                                + "7 de abril de 2026 "
                                + "CIF: B76366723"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertEquals(
                "ATENCO ENERGIA, SL",
                resultado.proveedor()
        );
    }

    @Test
    void extraeNumeroFacturaDespuesDeLaFormaSocial()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "ATENCO ENERGIA, SL "
                                + "FAT-2026-054412 "
                                + "7 de abril de 2026 "
                                + "CIF: B76366723"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertEquals(
                "FAT-2026-054412",
                resultado.numeroFactura()
        );
    }

    @Test
    void extraeFechaDespuesDelNumeroFactura()
            throws Exception {

        FacturaOcrService service =
                new FacturaOcrService(
                        "",
                        "spa"
                );

        byte[] pdf =
                crearPdfConTexto(
                        "ATENCO ENERGIA, SL "
                                + "FAT-2026-054412 "
                                + "7 de abril de 2026 "
                                + "CIF: B76366723"
                );

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        pdf
                );

        FacturaOcrResultado resultado =
                service.analizarFactura(
                        fichero
                );

        assertEquals(
                LocalDate.of(
                        2026,
                        4,
                        7
                ),
                resultado.fechaFactura()
        );
    }
}