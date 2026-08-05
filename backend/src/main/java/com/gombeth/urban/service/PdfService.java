package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.Vecino;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfService {

    private static final DateTimeFormatter FECHA_ES =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Color AZUL_CABECERA =
            new Color(31, 78, 121);

    private static final Color GRIS_FONDO =
            new Color(242, 244, 247);

    public byte[] generarReciboPdf(
            ContabilidadRecibo recibo,
            Comunidad comunidad,
            Vecino vecino
    ) {
        validarDatosRecibo(recibo, comunidad, vecino);

        ByteArrayOutputStream salida =
                new ByteArrayOutputStream();

        Document documento = new Document(
                PageSize.A4,
                42,
                42,
                36,
                36
        );

        try {
            PdfWriter.getInstance(
                    documento,
                    salida
            );

            documento.open();

            Font titulo = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    18,
                    Color.BLACK
            );

            Font subtitulo = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    11,
                    Color.WHITE
            );

            Font etiqueta = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    9,
                    Color.DARK_GRAY
            );

            Font texto = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    9,
                    Color.BLACK
            );

            Font importe = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    16,
                    AZUL_CABECERA
            );

            Paragraph encabezado = new Paragraph(
                    "RECIBO",
                    titulo
            );
            encabezado.setAlignment(
                    Element.ALIGN_CENTER
            );
            encabezado.setSpacingAfter(4);
            documento.add(encabezado);

            Paragraph referencia = new Paragraph(
                    "Referencia: " + recibo.getId(),
                    texto
            );
            referencia.setAlignment(
                    Element.ALIGN_CENTER
            );
            referencia.setSpacingAfter(18);
            documento.add(referencia);

            agregarSeccion(
                    documento,
                    "DATOS DE LA COMUNIDAD",
                    subtitulo
            );

            PdfPTable tablaComunidad =
                    crearTablaDatos();

            agregarFila(
                    tablaComunidad,
                    "Comunidad",
                    valor(comunidad.getNombre()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaComunidad,
                    "NIF/CIF",
                    valor(comunidad.getNifCif()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaComunidad,
                    "Dirección",
                    direccionComunidad(comunidad),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaComunidad,
                    "Identificador acreedor",
                    valor(comunidad.getIdentificadorAcreedor()),
                    etiqueta,
                    texto
            );

            documento.add(tablaComunidad);

            agregarSeccion(
                    documento,
                    "DATOS DEL PROPIETARIO",
                    subtitulo
            );

            PdfPTable tablaVecino =
                    crearTablaDatos();

            agregarFila(
                    tablaVecino,
                    "Propietario",
                    valor(vecino.getNombre()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaVecino,
                    "NIF",
                    valor(vecino.getNif()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaVecino,
                    "Vivienda",
                    valor(vecino.getVivienda()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaVecino,
                    "Dirección de notificación",
                    direccionVecino(vecino),
                    etiqueta,
                    texto
            );

            documento.add(tablaVecino);

            agregarSeccion(
                    documento,
                    "DETALLE DEL RECIBO",
                    subtitulo
            );

            PdfPTable tablaRecibo =
                    crearTablaDatos();

            agregarFila(
                    tablaRecibo,
                    "Fecha de emisión",
                    fecha(recibo.getFechaEmision()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaRecibo,
                    "Concepto",
                    valor(recibo.getConcepto()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaRecibo,
                    "Tipo",
                    valor(recibo.getTipoRemesa()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaRecibo,
                    "Estado",
                    valor(recibo.getEstado()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaRecibo,
                    "Versión / periodo",
                    valor(recibo.getEtiquetaExtra()),
                    etiqueta,
                    texto
            );

            if (recibo.getFechaCobroBanco() != null) {
                agregarFila(
                        tablaRecibo,
                        "Fecha de cobro",
                        fecha(recibo.getFechaCobroBanco()),
                        etiqueta,
                        texto
                );
            }

            documento.add(tablaRecibo);

            PdfPTable tablaImporte =
                    new PdfPTable(1);
            tablaImporte.setWidthPercentage(100);
            tablaImporte.setSpacingBefore(18);

            PdfPCell celdaImporte = new PdfPCell(
                    new Phrase(
                            "TOTAL: "
                                    + importe(recibo.getImporte())
                                    + " €",
                            importe
                    )
            );
            celdaImporte.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );
            celdaImporte.setPadding(12);
            celdaImporte.setBackgroundColor(
                    GRIS_FONDO
            );
            celdaImporte.setBorderColor(
                    AZUL_CABECERA
            );
            tablaImporte.addCell(celdaImporte);

            documento.add(tablaImporte);

            Paragraph pie = new Paragraph(
                    "Documento generado por Gombeth Urban.",
                    FontFactory.getFont(
                            FontFactory.HELVETICA_OBLIQUE,
                            8,
                            Color.GRAY
                    )
            );
            pie.setAlignment(Element.ALIGN_CENTER);
            pie.setSpacingBefore(24);
            documento.add(pie);

            documento.close();
            return salida.toByteArray();

        } catch (Exception excepcion) {
            if (documento.isOpen()) {
                documento.close();
            }

            throw new IllegalStateException(
                    "No se pudo generar el PDF del recibo "
                            + recibo.getId()
                            + ".",
                    excepcion
            );
        }
    }

    public byte[] generarMandatoSepa(
            Comunidad comunidad,
            Vecino vecino
    ) {
        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        Document document = new Document(
                PageSize.A4,
                50,
                50,
                50,
                50
        );

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titulo = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    14
            );
            Font subtitulo = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    10
            );
            Font normal = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    10
            );

            Paragraph pTitulo = new Paragraph(
                    "ORDEN DE DOMICILIACIÓN SEPA (CORE)",
                    titulo
            );
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            pTitulo.setSpacingAfter(20);
            document.add(pTitulo);

            document.add(new Paragraph(
                    "DATOS DEL ACREEDOR:",
                    subtitulo
            ));
            document.add(new Paragraph(
                    "Nombre: " + valor(comunidad.getNombre()),
                    normal
            ));
            document.add(new Paragraph(
                    "ID Acreedor: "
                            + valor(comunidad.getIdentificadorAcreedor()),
                    normal
            ));
            document.add(new Paragraph(
                    "Dirección: "
                            + valor(comunidad.getDireccion()),
                    normal
            ));
            document.add(new Paragraph(
                    "Población: "
                            + valor(comunidad.getPoblacion()),
                    normal
            ));
            document.add(new Paragraph(" "));

            document.add(new Paragraph(
                    "DATOS DEL DEUDOR (PAGADOR):",
                    subtitulo
            ));
            document.add(new Paragraph(
                    "Nombre: " + valor(vecino.getNombre()),
                    normal
            ));
            document.add(new Paragraph(
                    "NIF: " + valor(vecino.getNif()),
                    normal
            ));
            document.add(new Paragraph(
                    "Propiedad: " + valor(vecino.getVivienda()),
                    normal
            ));
            document.add(new Paragraph(
                    "IBAN: " + valor(vecino.getIban()),
                    normal
            ));
            document.add(new Paragraph(
                    "Referencia mandato: "
                            + valor(vecino.getReferenciaMandato()),
                    normal
            ));
            document.add(new Paragraph(" "));

            Paragraph textoLegal = new Paragraph(
                    "Mediante la firma de esta orden de domiciliación, "
                            + "el deudor autoriza al acreedor a enviar "
                            + "instrucciones a su entidad para adeudar "
                            + "su cuenta y a la entidad para adeudar los "
                            + "importes correspondientes de acuerdo con "
                            + "las instrucciones del acreedor.",
                    normal
            );
            textoLegal.setSpacingBefore(10);
            document.add(textoLegal);

            document.add(new Paragraph(
                    "\n\nFecha: _________________  "
                            + "Firma: ___________________________",
                    normal
            ));

            document.close();
            return out.toByteArray();

        } catch (Exception excepcion) {
            if (document.isOpen()) {
                document.close();
            }

            throw new IllegalStateException(
                    "Error generando mandato SEPA.",
                    excepcion
            );
        }
    }

    public String nombreArchivoRecibo(
            ContabilidadRecibo recibo,
            Vecino vecino
    ) {
        return "RECIBO_"
                + recibo.getId()
                + "_"
                + normalizarNombreArchivo(
                vecino != null
                        ? vecino.getVivienda()
                        : "VIVIENDA"
        )
                + ".pdf";
    }

    private void validarDatosRecibo(
            ContabilidadRecibo recibo,
            Comunidad comunidad,
            Vecino vecino
    ) {
        if (recibo == null || recibo.getId() == null) {
            throw new IllegalArgumentException(
                    "El recibo es obligatorio."
            );
        }

        if (comunidad == null || comunidad.getId() == null) {
            throw new IllegalArgumentException(
                    "La comunidad del recibo es obligatoria."
            );
        }

        if (vecino == null || vecino.getId() == null) {
            throw new IllegalArgumentException(
                    "El propietario del recibo es obligatorio."
            );
        }
    }

    private void agregarSeccion(
            Document documento,
            String titulo,
            Font fuente
    ) throws Exception {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(12);
        tabla.setSpacingAfter(6);

        PdfPCell celda = new PdfPCell(
                new Phrase(titulo, fuente)
        );
        celda.setBackgroundColor(AZUL_CABECERA);
        celda.setPadding(7);
        celda.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(celda);

        documento.add(tabla);
    }

    private PdfPTable crearTablaDatos()
            throws Exception {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 4.5f});
        return tabla;
    }

    private void agregarFila(
            PdfPTable tabla,
            String etiqueta,
            String contenido,
            Font fuenteEtiqueta,
            Font fuenteContenido
    ) {
        PdfPCell celdaEtiqueta = new PdfPCell(
                new Phrase(etiqueta, fuenteEtiqueta)
        );
        celdaEtiqueta.setBackgroundColor(GRIS_FONDO);
        celdaEtiqueta.setPadding(6);
        celdaEtiqueta.setBorderColor(Color.LIGHT_GRAY);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaContenido = new PdfPCell(
                new Phrase(contenido, fuenteContenido)
        );
        celdaContenido.setPadding(6);
        celdaContenido.setBorderColor(Color.LIGHT_GRAY);
        tabla.addCell(celdaContenido);
    }

    private String direccionComunidad(
            Comunidad comunidad
    ) {
        return unirDireccion(
                comunidad.getDireccion(),
                comunidad.getCodigoPostal(),
                comunidad.getPoblacion(),
                comunidad.getProvincia()
        );
    }

    private String direccionVecino(
            Vecino vecino
    ) {
        if (tieneTexto(vecino.getDireccionNotificacion())) {
            return vecino.getDireccionNotificacion().trim();
        }

        return unirDireccion(
                vecino.getDireccion(),
                vecino.getCodigoPostal(),
                vecino.getPoblacion(),
                vecino.getProvincia()
        );
    }

    private String unirDireccion(
            String direccion,
            String codigoPostal,
            String poblacion,
            String provincia
    ) {
        StringBuilder resultado = new StringBuilder();

        agregarParte(resultado, direccion);
        agregarParte(resultado, codigoPostal);
        agregarParte(resultado, poblacion);
        agregarParte(resultado, provincia);

        return resultado.length() > 0
                ? resultado.toString()
                : "";
    }

    private void agregarParte(
            StringBuilder resultado,
            String parte
    ) {
        if (!tieneTexto(parte)) {
            return;
        }

        if (resultado.length() > 0) {
            resultado.append(", ");
        }

        resultado.append(parte.trim());
    }

    private String fecha(LocalDate fecha) {
        return fecha != null
                ? FECHA_ES.format(fecha)
                : "";
    }

    private String importe(BigDecimal valor) {
        BigDecimal importeSeguro = valor != null
                ? valor
                : BigDecimal.ZERO;

        return importeSeguro
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',');
    }

    private String valor(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private String normalizarNombreArchivo(
            String texto
    ) {
        if (!tieneTexto(texto)) {
            return "VIVIENDA";
        }

        String normalizado = java.text.Normalizer
                .normalize(
                        texto,
                        java.text.Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace('Ñ', 'N')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        return normalizado.isBlank()
                ? "VIVIENDA"
                : normalizado;
    }
}