package com.gombeth.urban.service;

import com.gombeth.urban.entity.Acta;
import com.gombeth.urban.entity.EstadoActa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Enumeration;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfActaService {

    private final Path actasPath;
    private final String certificadoPath;
    private final String certificadoPassword;

    public PdfActaService(
            @Value("${app.storage.actas-path}")
            String actasPath,

            @Value("${gombeth.actas.certificado-path:}")
            String certificadoPath,

            @Value("${gombeth.actas.certificado-password:}")
            String certificadoPassword
    ) {
        this.actasPath = Path.of(actasPath);
        this.certificadoPath =
                certificadoPath != null
                        ? certificadoPath
                        : "";
        this.certificadoPassword =
                certificadoPassword != null
                        ? certificadoPassword
                        : "";
    }

    public String generarPdfActa(Acta acta)
            throws Exception {

        validarActa(acta);

        Path carpetaComunidad =
                actasPath.resolve(
                        "comunidad_"
                                + acta.getComunidad().getId()
                );

        Files.createDirectories(
                carpetaComunidad
        );

        String nombreArchivo;

        boolean requiereFirma =
                acta.getEstado()
                        != EstadoActa.BORRADOR;

        if (!requiereFirma) {

            nombreArchivo =
                    "Acta_"
                            + acta.getId()
                            + "_Preview.pdf";

        } else {

            nombreArchivo =
                    "Acta_"
                            + acta.getId()
                            + "_FIRMADA.pdf";
        }

        Path rutaPdf =
                carpetaComunidad.resolve(
                        nombreArchivo
                );

        String html =
                prepararHtml(acta);

        byte[] pdf =
                generarPdfDesdeHtml(html);

        if (requiereFirma) {
            pdf =
                    firmarBytes(pdf);
        }

        Files.write(
                rutaPdf,
                pdf
        );

        return rutaPdf.toString();
    }

    public Path obtenerPdf(
            Acta acta
    ) throws Exception {

        validarActa(acta);

        Path carpetaComunidad =
                actasPath.resolve(
                        "comunidad_"
                                + acta.getComunidad().getId()
                );

        String nombreArchivo;

        if (acta.getEstado() == EstadoActa.BORRADOR) {

            nombreArchivo =
                    "Acta_"
                            + acta.getId()
                            + "_Preview.pdf";

        } else {

            nombreArchivo =
                    "Acta_"
                            + acta.getId()
                            + "_FIRMADA.pdf";
        }

        Path rutaPdf =
                carpetaComunidad.resolve(
                        nombreArchivo
                );

        if (Files.exists(rutaPdf)) {
            return rutaPdf;
        }

        if (acta.getEstado() == EstadoActa.BORRADOR) {

            return Path.of(
                    generarPdfActa(acta)
            );
        }

        throw new IllegalStateException(
                "No existe el PDF firmado del acta "
                        + acta.getId()
                        + "."
        );
    }

    public void eliminarPdfFirmadoGenerado(
            Acta acta
    ) throws Exception {

        validarActa(acta);

        Path carpetaComunidad =
                actasPath.resolve(
                        "comunidad_"
                                + acta.getComunidad().getId()
                );

        Path rutaPdfFirmado =
                carpetaComunidad.resolve(
                        "Acta_"
                                + acta.getId()
                                + "_FIRMADA.pdf"
                );

        Files.deleteIfExists(
                rutaPdfFirmado
        );
    }

    private byte[] generarPdfDesdeHtml(
            String html
    ) throws Exception {

        ByteArrayOutputStream salida =
                new ByteArrayOutputStream();

        ITextRenderer renderer =
                new ITextRenderer();

        renderer.setDocumentFromString(
                html
        );

        renderer.layout();

        renderer.createPDF(
                salida
        );

        return salida.toByteArray();
    }

    private String prepararHtml(
            Acta acta
    ) {

        String contenido =
                acta.getContenido() != null
                        ? acta.getContenido()
                        : "";

        String marcaBorrador =
                acta.getEstado() == EstadoActa.BORRADOR
                        ? "<div class=\"watermark\">BORRADOR</div>"
                        : "";

        String html =
                """
                        <html>
                        <head>
                            <style>
                                body {
                                    font-family: Helvetica, sans-serif;
                                    margin: 50px;
                                    color: #333333;
                                }
                        
                                .header {
                                    text-align: center;
                                    border-bottom: 2px solid #00458b;
                                    margin-bottom: 20px;
                                    padding-bottom: 10px;
                                }
                        
                                .comunidad {
                                    font-size: 18px;
                                    font-weight: bold;
                                    color: #00458b;
                                    text-transform: uppercase;
                                }
                        
                                .titulo {
                                    font-size: 22px;
                                    margin-top: 10px;
                                    font-weight: bold;
                                }
                        
                                .fecha {
                                    text-align: right;
                                    font-style: italic;
                                    margin-bottom: 30px;
                                    font-size: 14px;
                                }
                        
                                .contenido {
                                    line-height: 1.6;
                                    text-align: justify;
                                }
                        
                                .watermark {
                                    position: absolute;
                                    top: 40%;
                                    left: 10%;
                                    font-size: 80px;
                                    color: #eeeeee;
                                    z-index: -1;
                                }
                            </style>
                        </head>
                        
                        <body>
                        """
                + marcaBorrador
                + """
                
                    <div class="header">
                
                        <div class="comunidad">
                """
                + escaparHtml(
                acta.getComunidad().getNombre()
        )
                + """
                        </div>
                
                        <div class="titulo">
                """
                + escaparHtml(
                acta.getTitulo()
        )
                + """
                        </div>
                
                    </div>
                
                    <div class="fecha">
                        En fecha:
                """
                + acta.getFechaReunion()
                + """
                    </div>
                
                    <div class="contenido">
                """
                + contenido
                + """
                        </div>
                
                    </body>
                    </html>
                """;

        return normalizarEntidadesHtmlHistoricas(
                html
        );
    }

    private String escaparHtml(
            String valor
    ) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

        private String normalizarEntidadesHtmlHistoricas(
                String html
) {

            if (html == null) {
                return null;
            }

            return html
                    .replace("&nbsp;", "&#160;")
                    .replace("&aacute;", "&#225;")
                    .replace("&eacute;", "&#233;")
                    .replace("&iacute;", "&#237;")
                    .replace("&oacute;", "&#243;")
                    .replace("&uacute;", "&#250;")
                    .replace("&ntilde;", "&#241;")
                    .replace("&euro;", "&#8364;");
        }

    protected byte[] firmarBytes(
            byte[] inputPdf
    ) throws Exception {

        if (
                certificadoPath == null
                        || certificadoPath.isBlank()
        ) {
            throw new IllegalStateException(
                    "No está configurada la ruta "
                            + "del certificado de actas."
            );
        }

        if (
                certificadoPassword == null
                        || certificadoPassword.isBlank()
        ) {
            throw new IllegalStateException(
                    "No está configurada la contraseña "
                            + "del certificado de actas."
            );
        }

        Path rutaCertificado =
                Path.of(certificadoPath);

        if (!Files.exists(rutaCertificado)) {
            throw new IllegalStateException(
                    "No existe el certificado de actas en: "
                            + rutaCertificado
            );
        }

        char[] password =
                certificadoPassword.toCharArray();

        KeyStore keyStore =
                KeyStore.getInstance("PKCS12");

        try (
                FileInputStream input =
                        new FileInputStream(
                                rutaCertificado.toFile()
                        )
        ) {
            keyStore.load(
                    input,
                    password
            );
        }

        Enumeration<String> aliases =
                keyStore.aliases();

        if (!aliases.hasMoreElements()) {
            throw new IllegalStateException(
                    "El certificado PKCS12 "
                            + "no contiene ningún alias."
            );
        }

        String alias =
                aliases.nextElement();

        Key key =
                keyStore.getKey(
                        alias,
                        password
                );

        if (!(key instanceof PrivateKey privateKey)) {
            throw new IllegalStateException(
                    "El certificado no contiene "
                            + "una clave privada válida."
            );
        }

        Certificate[] certificateChain =
                keyStore.getCertificateChain(
                        alias
                );

        if (
                certificateChain == null
                        || certificateChain.length == 0
        ) {
            throw new IllegalStateException(
                    "El certificado no contiene "
                            + "una cadena de certificados válida."
            );
        }

        PdfReader reader =
                new PdfReader(
                        inputPdf
                );

        ByteArrayOutputStream salidaFirmada =
                new ByteArrayOutputStream();

        PdfStamper stamper = null;

        try {

            stamper =
                    PdfStamper.createSignature(
                            reader,
                            salidaFirmada,
                            '\0'
                    );

            PdfSignatureAppearance appearance =
                    stamper.getSignatureAppearance();

            appearance.setVisibleSignature(
                    new Rectangle(
                            70.0f,
                            50.0f,
                            350.0f,
                            150.0f
                    ),
                    reader.getNumberOfPages(),
                    "Firma GTI"
            );

            appearance.setCrypto(
                    privateKey,
                    certificateChain,
                    null,
                    PdfSignatureAppearance.SELF_SIGNED
            );

            appearance.setReason(
                    "Certificación Oficial "
                            + "de la Administración"
            );

            appearance.setLocation(
                    "España"
            );

            appearance.setCertificationLevel(
                    -1
            );

            stamper.close();
            stamper = null;

            return salidaFirmada.toByteArray();

        } finally {

            if (stamper != null) {
                try {
                    stamper.close();
                } catch (Exception ignored) {
                    // Se preserva la excepción original.
                }
            }

            reader.close();
        }
    }

    private void validarActa(
            Acta acta
    ) {

        if (acta == null) {
            throw new IllegalArgumentException(
                    "El acta es obligatoria."
            );
        }

        if (acta.getId() == null) {
            throw new IllegalArgumentException(
                    "El acta debe tener identificador."
            );
        }

        if (
                acta.getComunidad() == null
                        || acta.getComunidad().getId() == null
        ) {
            throw new IllegalArgumentException(
                    "El acta debe pertenecer "
                            + "a una comunidad."
            );
        }

        if (acta.getEstado() == null) {
            throw new IllegalArgumentException(
                    "El acta debe tener estado."
            );
        }
    }
}