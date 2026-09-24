package com.gombeth.urban.service;

import com.gombeth.urban.dto.FacturaOcrResultado;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Service
public class FacturaOcrService {

    private static final int LONGITUD_MINIMA_TEXTO = 20;
    private static final float OCR_DPI = 300f;

    private final String tessdataPath;
    private final String idioma;

    public FacturaOcrService(
            @Value("${gombeth.ocr.tessdata-path:}") String tessdataPath,
            @Value("${gombeth.ocr.language:spa}") String idioma
    ) {
        this.tessdataPath = tessdataPath != null
                ? tessdataPath.trim()
                : "";

        this.idioma = idioma != null && !idioma.isBlank()
                ? idioma.trim()
                : "spa";
    }

    /**
     * Primera fase del análisis de factura.
     *
     * Por ahora se encarga únicamente de comprobar que podemos
     * extraer texto del PDF, usando OCR cuando sea necesario.
     *
     * La detección de proveedor, fecha, importe y número de factura
     * se incorporará en el siguiente paso.
     */
    public FacturaOcrResultado analizarFactura(
            MultipartFile fichero
    ) {
        validarFichero(fichero);

        List<String> advertencias = new ArrayList<>();

        ExtraccionTexto extraccion =
                extraerTexto(fichero, advertencias);

        LocalDate fechaFactura = extraerFechaFactura(extraccion.texto());

        BigDecimal importeTotal = extraerImporteTotal(extraccion.texto());

        String numeroFactura =
            extraerNumeroFactura(
                    extraccion.texto()
            );

        String proveedor =
            extraerProveedor(
                extraccion.texto()
            );

        if (
                extraccion.texto() == null
                        || extraccion.texto().isBlank()
        ) {
            advertencias.add(
                    "No se ha podido extraer texto de la factura."
            );
        }

        return new FacturaOcrResultado(
                proveedor,
                fechaFactura,
                importeTotal,
                numeroFactura,
                extraccion.ocrAplicado(),
                List.copyOf(advertencias)
        );
    }

    private ExtraccionTexto extraerTexto(
            MultipartFile fichero,
            List<String> advertencias
    ) {
        try (
                PDDocument documento =
                        Loader.loadPDF(
                                fichero.getBytes()
                        )
        ) {
            if (documento.getNumberOfPages() == 0) {
                throw new IllegalArgumentException(
                        "El PDF no contiene páginas."
                );
            }

            String textoPdf =
                    extraerTextoPdf(
                            documento,
                            advertencias
                    );

            if (
                    textoPdf != null
                            && textoPdf.trim().length()
                            >= LONGITUD_MINIMA_TEXTO
            ) {

                if (
                        tessdataPath == null
                                || tessdataPath.isBlank()
                                || !necesitaOcrComplementario(
                                textoPdf
                        )
                ) {
                    return new ExtraccionTexto(
                            textoPdf,
                            false
                    );
                }

                advertencias.add(
                        "El PDF contiene texto, pero no se han detectado suficientes datos; se ha utilizado OCR."
                );

                String textoOcr =
                        aplicarOcr(
                                documento
                        );

                if (
                        puntuarExtraccion(
                                textoOcr
                        )
                                > puntuarExtraccion(
                                textoPdf
                        )
                ) {
                    return new ExtraccionTexto(
                            textoOcr,
                            true
                    );
                }

                return new ExtraccionTexto(
                        textoPdf,
                        false
                );
            }

            advertencias.add(
                    "El PDF no contiene texto suficiente; se ha utilizado OCR."
            );

            String textoOcr =
                    aplicarOcr(
                            documento
                    );

            return new ExtraccionTexto(
                    textoOcr,
                    true
            );

        } catch (IllegalArgumentException error) {
            throw error;

        } catch (Exception error) {
            throw new IllegalArgumentException(
                    "No se ha podido analizar el PDF de la factura.",
                    error
            );
        }
    }

    private String extraerTextoPdf(
            PDDocument documento,
            List<String> advertencias
    ) {
        try {
            PDFTextStripper stripper =
                    new PDFTextStripper();

            stripper.setSortByPosition(true);

            return stripper.getText(
                    documento
            );

        } catch (Exception error) {
            advertencias.add(
                    "La extracción directa de texto del PDF ha fallado."
            );

            return "";
        }
    }

    private String aplicarOcr(
            PDDocument documento
    ) {
        if (
                tessdataPath == null
                        || tessdataPath.isBlank()
        ) {
            throw new IllegalStateException(
                    "No está configurada la ruta tessdata para OCR."
            );
        }

        try {
            ITesseract tesseract =
                    new Tesseract();

            tesseract.setDatapath(
                    tessdataPath
            );

            tesseract.setLanguage(
                    idioma
            );

            tesseract.setVariable(
                    "user_defined_dpi",
                    "300"
            );

            PDFRenderer renderer =
                    new PDFRenderer(
                            documento
                    );

            StringBuilder resultado =
                    new StringBuilder();

            for (
                    int pagina = 0;
                    pagina < documento.getNumberOfPages();
                    pagina++
            ) {
                BufferedImage imagen =
                        renderer.renderImageWithDPI(
                                pagina,
                                OCR_DPI
                        );

                String textoPagina =
                    tesseract.doOCR(
                            imagen
                    );

                if (
                        textoPagina != null
                                && !textoPagina.isBlank()
                ) {
                    resultado
                            .append(textoPagina)
                            .append(System.lineSeparator());
                }
            }

            return resultado.toString();

        } catch (Exception error) {
            throw new IllegalStateException(
                    "No se ha podido ejecutar el OCR de la factura.",
                    error
            );
        }
    }

    private boolean necesitaOcrComplementario(
            String texto
    ) {
        return puntuarExtraccion(
                texto
        ) < 3;
    }

    private int puntuarExtraccion(
            String texto
    ) {
        int puntuacion = 0;

        if (
                extraerProveedor(
                        texto
                ) != null
        ) {
            puntuacion++;
        }

        if (
                extraerFechaFactura(
                        texto
                ) != null
        ) {
            puntuacion++;
        }

        if (
                extraerImporteTotal(
                        texto
                ) != null
        ) {
            puntuacion++;
        }

        if (
                extraerNumeroFactura(
                        texto
                ) != null
        ) {
            puntuacion++;
        }

        return puntuacion;
    }

    private LocalDate extraerFechaFactura(
            String texto
    ) {
        if (
                texto == null
                        || texto.isBlank()
        ) {
            return null;
        }

        Pattern patronNumerico =
                Pattern.compile(
                        "(?i)"
                                + "(?:fecha\\s*(?:de\\s*)?factura"
                                + "|fecha\\s*(?:de\\s*)?emisi[oó]n"
                                + "(?:\\s*de\\s*factura)?)"
                                + "\\s*[:\\-]?\\s*"
                                + "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})"
                );

        Matcher matcherNumerico =
                patronNumerico.matcher(
                        texto
                );

        if (matcherNumerico.find()) {

            String fechaTexto =
                    matcherNumerico.group(1)
                            .replace(
                                    '-',
                                    '/'
                            );

            try {
                DateTimeFormatter formato =
                        DateTimeFormatter.ofPattern(
                                "d/M/uuuu"
                        );

                return LocalDate.parse(
                        fechaTexto,
                        formato
                );

            } catch (
                    DateTimeParseException error
            ) {
                return null;
            }
        }

        Pattern patronTexto =
                Pattern.compile(
                        "(?is)"
                                + "(?:fecha\\s*(?:de\\s*)?factura"
                                + "|fecha\\s*(?:de\\s*)?emisi[oó]n"
                                + "(?:\\s*de\\s*factura)?)"
                                + "\\s*[:\\-]?\\s*"
                                + "(?:[A-Z0-9._/\\-]+\\s+)?"
                                + "(\\d{1,2}\\s+de\\s+"
                                + "[a-záéíóúñ]+"
                                + "\\s+de\\s+\\d{4})"
                );

        Matcher matcherTexto =
                patronTexto.matcher(
                        texto
                );

        if (matcherTexto.find()) {
            try {
                DateTimeFormatter formato =
                        DateTimeFormatter.ofPattern(
                                "d 'de' MMMM 'de' uuuu",
                                Locale.forLanguageTag(
                                        "es-ES"
                                )
                        );

                return LocalDate.parse(
                        matcherTexto.group(1)
                                .trim()
                                .toLowerCase(),
                        formato
                );

            } catch (
                    DateTimeParseException error
            ) {
                // Probamos el formato alternativo siguiente.
            }
        }

        /*
         * Algunos PDF alteran el orden visual y devuelven:
         *
         * ATENCO ENERGIA, SL FAT-2026-054412 7 de abril de 2026
         */
        Pattern patronTrasNumeroFactura =
                Pattern.compile(
                        "(?i)"
                                + "(?:"
                                + "S\\.?\\s*L\\.?\\s*U\\.?"
                                + "|S\\.?\\s*L\\.?\\s*L\\.?"
                                + "|S\\.?\\s*L\\.?"
                                + "|S\\.?\\s*A\\.?"
                                + ")"
                                + "\\s+"
                                + "[A-Z0-9][A-Z0-9._/\\-]{2,}"
                                + "\\s+"
                                + "(\\d{1,2}\\s+de\\s+"
                                + "[a-záéíóúñ]+"
                                + "\\s+de\\s+\\d{4})"
                );

        Matcher matcherTrasNumeroFactura =
                patronTrasNumeroFactura.matcher(
                        texto
                );

        if (!matcherTrasNumeroFactura.find()) {
            return null;
        }

        try {
            DateTimeFormatter formato =
                    DateTimeFormatter.ofPattern(
                            "d 'de' MMMM 'de' uuuu",
                            Locale.forLanguageTag(
                                    "es-ES"
                            )
                    );

            return LocalDate.parse(
                    matcherTrasNumeroFactura.group(1)
                            .trim()
                            .toLowerCase(),
                    formato
            );

        } catch (
                DateTimeParseException error
        ) {
            return null;
        }
    }

    private BigDecimal extraerImporteTotal(
            String texto
    ) {
        if (
                texto == null
                        || texto.isBlank()
        ) {
            return null;
        }

        Pattern patron =
                Pattern.compile(
                        "(?i)"
                                + "(?:importe\\s*(?:total\\s*)?factura"
                                + "|importe\\s*total"
                                + "|total\\s*importe\\s*factura"
                                + "|total\\s*factura)"
                                + "\\s*[:\\-]?\\s*"
                                + "([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2}"
                                + "|[0-9]+,[0-9]{2}"
                                + "|[0-9]+\\.[0-9]{2})"
                                + "\\s*(?:€|EUR)?"
                );

        Matcher matcher =
                patron.matcher(
                        texto
                );

        if (!matcher.find()) {
            return null;
        }

        String importeTexto =
                matcher.group(1)
                        .trim();

        if (
                importeTexto.contains(".")
                        && importeTexto.contains(",")
        ) {
            importeTexto =
                    importeTexto
                            .replace(
                                    ".",
                                    ""
                            )
                            .replace(
                                    ",",
                                    "."
                            );

        } else if (
                importeTexto.contains(",")
        ) {
            importeTexto =
                    importeTexto.replace(
                            ",",
                            "."
                    );
        }

        try {
            return new BigDecimal(
                    importeTexto
            );

        } catch (NumberFormatException error) {
            return null;
        }
    }

    private String extraerNumeroFactura(
            String texto
    ) {
        if (
                texto == null
                        || texto.isBlank()
        ) {
            return null;
        }

        Pattern patron =
                Pattern.compile(
                        "(?i)"
                                + "(?:"
                                + "(?:n[uú]mero|num\\.?|n[º°o]|nro\\.?)"
                                + "\\s*(?:de\\s*)?factura"
                                + "|"
                                + "factura\\s*"
                                + "(?:n[uú]mero|num\\.?|n[º°o]|nro\\.?)"
                                + ")"
                                + "\\s*[:#\\-]?\\s*"
                                + "(?:"
                                + "fecha\\s*(?:de\\s*)?emisi[oó]n"
                                + "\\s*(?:de\\s*)?factura"
                                + "\\s*[:#\\-]?\\s*"
                                + ")?"
                                + "([A-Z0-9][A-Z0-9._/\\-]{1,})"
                );

        Matcher matcher =
                patron.matcher(
                        texto
                );

        if (matcher.find()) {

            String numeroFactura =
                    matcher.group(1)
                            .trim();

            boolean contieneDigito =
                    numeroFactura
                            .chars()
                            .anyMatch(
                                    Character::isDigit
                            );

            if (contieneDigito) {
                return numeroFactura;
            }
        }

        /*
         * Algunos PDF colocan visualmente el número junto a la cabecera,
         * pero PDFTextStripper devuelve primero el proveedor y después
         * número + fecha.
         *
         * Ejemplo real:
         * ATENCO ENERGIA, SL FAT-2026-054412 7 de abril de 2026
         */
        Pattern patronTrasFormaSocial =
                Pattern.compile(
                        "(?i)"
                                + "(?:"
                                + "S\\.?\\s*L\\.?\\s*U\\.?"
                                + "|S\\.?\\s*L\\.?\\s*L\\.?"
                                + "|S\\.?\\s*L\\.?"
                                + "|S\\.?\\s*A\\.?"
                                + ")"
                                + "\\s+"
                                + "([A-Z0-9][A-Z0-9._/\\-]{2,})"
                                + "(?="
                                + "\\s+\\d{1,2}\\s+de\\s+"
                                + "[a-záéíóúñ]+"
                                + "\\s+de\\s+\\d{4}"
                                + "|\\s+(?:CIF|NIF)\\b"
                                + ")"
                );

        Matcher matcherTrasFormaSocial =
                patronTrasFormaSocial.matcher(
                        texto
                );

        if (!matcherTrasFormaSocial.find()) {
            return null;
        }

        String numeroFactura =
                matcherTrasFormaSocial.group(1)
                        .trim();

        boolean contieneDigito =
                numeroFactura
                        .chars()
                        .anyMatch(
                                Character::isDigit
                        );

        return contieneDigito
                ? numeroFactura
                : null;
    }

    private String extraerProveedor(
            String texto
    ) {
        if (
                texto == null
                        || texto.isBlank()
        ) {
            return null;
        }

        Pattern patron =
                Pattern.compile(
                        "(?is)"
                                + "(?:proveedor"
                                + "|raz[oó]n\\s+social"
                                + "|emisor)"
                                + "\\s*[:\\-]?\\s*"
                                + "(.+?)"
                                + "(?="
                                + "\\s+(?:"
                                + "(?:n[uú]mero|num\\.?|n[º°o]|nro\\.?)"
                                + "\\s*(?:de\\s*)?factura"
                                + "|factura\\s*"
                                + "(?:n[uú]mero|num\\.?|n[º°o]|nro\\.?)"
                                + "|fecha\\s*(?:de\\s*)?"
                                + "(?:factura|emisi[oó]n)"
                                + "|importe\\s*total"
                                + "|total\\s*(?:factura)?"
                                + ")"
                                + "\\s*[:#\\-]?"
                                + "|\\R"
                                + "|$"
                                + ")"
                );

        Matcher matcher =
                patron.matcher(
                        texto
                );

        if (matcher.find()) {

            String proveedor =
                    matcher.group(1)
                            .replaceAll(
                                    "\\s+",
                                    " "
                            )
                            .trim();

            if (!proveedor.isBlank()) {
                return proveedor;
            }
        }

        Pattern patronFormaSocial =
                Pattern.compile(
                        "(?im)"
                                + "(?:^|\\R)"
                                + "\\s*"
                                + "([A-ZÁÉÍÓÚÜÑ0-9]"
                                + "[A-ZÁÉÍÓÚÜÑ0-9 .,&'()\\-/]{1,80}?"
                                + "(?:"
                                + "S\\.?\\s*L\\.?\\s*U\\.?"
                                + "|S\\.?\\s*L\\.?\\s*L\\.?"
                                + "|S\\.?\\s*L\\.?"
                                + "|S\\.?\\s*A\\.?"
                                + "))"
                                + "(?=\\s|$)"
                );

        Matcher matcherFormaSocial =
                patronFormaSocial.matcher(
                        texto
                );

        if (matcherFormaSocial.find()) {

            String proveedor =
                    matcherFormaSocial.group(1)
                            .replaceAll(
                                    "\\s+",
                                    " "
                            )
                            .trim();

            if (!proveedor.isBlank()) {
                return proveedor;
            }
        }

        Pattern patronAntesCif =
                Pattern.compile(
                        "(?im)"
                                + "(?:^|\\R)"
                                + "\\s*"
                                + "([A-ZÁÉÍÓÚÜÑ0-9]"
                                + "[A-ZÁÉÍÓÚÜÑ0-9 .,&'()\\-/]{2,100}?)"
                                + "\\s+"
                                + "(?:CIF|NIF)"
                                + "\\s*:\\s*"
                                + "[A-Z0-9-]+"
                );

        Matcher matcherAntesCif =
                patronAntesCif.matcher(
                        texto
                );

        if (!matcherAntesCif.find()) {
            return null;
        }

        String proveedor =
                matcherAntesCif.group(1)
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        return proveedor.isBlank()
                ? null
                : proveedor;
    }

    private void validarFichero(
            MultipartFile fichero
    ) {
        if (
                fichero == null
                        || fichero.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un fichero PDF."
            );
        }

        String nombre =
                fichero.getOriginalFilename();

        if (
                nombre == null
                        || !nombre.toLowerCase().endsWith(".pdf")
        ) {
            throw new IllegalArgumentException(
                    "El fichero debe ser un PDF."
            );
        }

        validarCabeceraPdf(
                fichero
        );
    }

    private void validarCabeceraPdf(
            MultipartFile fichero
    ) {
        try (
                InputStream entrada =
                        fichero.getInputStream()
        ) {
            byte[] cabecera =
                    entrada.readNBytes(
                            5
                    );

            boolean pdf =
                    cabecera.length == 5
                            && cabecera[0] == '%'
                            && cabecera[1] == 'P'
                            && cabecera[2] == 'D'
                            && cabecera[3] == 'F'
                            && cabecera[4] == '-';

            if (!pdf) {
                throw new IllegalArgumentException(
                        "El contenido del fichero no corresponde a un PDF válido."
                );
            }

        } catch (IOException error) {
            throw new IllegalArgumentException(
                    "No se ha podido leer el fichero PDF.",
                    error
            );
        }
    }

    private record ExtraccionTexto(
            String texto,
            boolean ocrAplicado
    ) {
    }
}