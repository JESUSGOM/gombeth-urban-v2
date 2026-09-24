package com.gombeth.urban.service;

import com.gombeth.urban.entity.Acta;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.EstadoActa;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfActaServiceFirmaRealTest {

    @TempDir
    Path tempDir;

    @Test
    void firmaActaCerradaConCertificadoReal()
            throws Exception {

        String password =
                System.getenv(
                        "GOMBETH_ACTAS_CERT_PASSWORD"
                );

        assumeTrue(
                password != null
                        && !password.isBlank(),
                "Prueba de firma real omitida: "
                        + "no está configurada "
                        + "GOMBETH_ACTAS_CERT_PASSWORD."
        );

        Path certificado =
                Path.of(
                        "C:/sepa1914/certificados/"
                                + "CertificadoJesus.p12"
                );

        assumeTrue(
                Files.exists(certificado),
                "Prueba de firma real omitida: "
                        + "no existe el certificado."
        );

        PdfActaService servicio =
                new PdfActaService(
                        tempDir.toString(),
                        certificado.toString(),
                        password
                );

        Comunidad comunidad =
                new Comunidad();

        comunidad.setId(18L);
        comunidad.setNombre(
                "Comunidad de Propietarios Test"
        );

        Acta acta =
                new Acta();

        acta.setId(999999L);
        acta.setComunidad(comunidad);
        acta.setTitulo(
                "Prueba técnica de firma"
        );
        acta.setFechaReunion(
                LocalDate.of(2026, 9, 23)
        );
        acta.setContenido(
                "<p>Documento temporal "
                        + "para validar la firma digital.</p>"
        );
        acta.setEstado(
                EstadoActa.CERRADA
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

        PdfReader reader =
                new PdfReader(
                        Files.readAllBytes(pdf)
                );

        try {

            List<String> firmas =
                    reader
                            .getAcroFields()
                            .getSignedFieldNames();

            assertFalse(
                    firmas.isEmpty(),
                    "El PDF debe contener "
                            + "una firma digital."
            );

            assertTrue(
                    firmas.contains("Firma GTI"),
                    "Debe existir la firma "
                            + "histórica 'Firma GTI'."
            );

        } finally {
            reader.close();
        }
    }


}