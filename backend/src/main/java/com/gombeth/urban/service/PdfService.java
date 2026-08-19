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

    private static final Color AZUL_PRINCIPAL =
            new Color(20, 64, 108);

    private static final Color AZUL_SECUNDARIO =
            new Color(41, 98, 150);

    private static final Color AZUL_MUY_CLARO =
            new Color(235, 244, 252);

    private static final Color GRIS_FONDO =
            new Color(245, 247, 250);

    // Colores exclusivos del mandato SEPA V3.
    // Se mantienen separados para no alterar el PDF profesional del recibo.
    private static final Color AZUL_MANDATO =
            new Color(31, 78, 121);

    private static final Color GRIS_FONDO_MANDATO =
            new Color(242, 244, 247);

    private static final Color GRIS_BORDE =
            new Color(211, 218, 226);

    private static final Color GRIS_TEXTO =
            new Color(82, 92, 104);

    private static final Color VERDE =
            new Color(31, 122, 78);

    private static final Color VERDE_FONDO =
            new Color(226, 245, 235);

    private static final Color NARANJA =
            new Color(160, 96, 0);

    private static final Color NARANJA_FONDO =
            new Color(255, 243, 214);

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
                34,
                34,
                24,
                22
        );

        try {
            PdfWriter.getInstance(
                    documento,
                    salida
            );

            documento.open();

            Font marca = fuente(
                    FontFactory.HELVETICA_BOLD,
                    16,
                    Color.WHITE
            );

            Font nombreAplicacion = fuente(
                    FontFactory.HELVETICA_BOLD,
                    15,
                    AZUL_PRINCIPAL
            );

            Font textoPequeno = fuente(
                    FontFactory.HELVETICA,
                    8,
                    GRIS_TEXTO
            );

            Font titulo = fuente(
                    FontFactory.HELVETICA_BOLD,
                    22,
                    AZUL_PRINCIPAL
            );

            Font referencia = fuente(
                    FontFactory.HELVETICA,
                    9,
                    GRIS_TEXTO
            );

            Font subtitulo = fuente(
                    FontFactory.HELVETICA_BOLD,
                    10,
                    Color.WHITE
            );

            Font etiqueta = fuente(
                    FontFactory.HELVETICA_BOLD,
                    8.5f,
                    GRIS_TEXTO
            );

            Font texto = fuente(
                    FontFactory.HELVETICA,
                    9,
                    Color.BLACK
            );

            Font textoDestacado = fuente(
                    FontFactory.HELVETICA_BOLD,
                    9,
                    AZUL_PRINCIPAL
            );

            Font importe = fuente(
                    FontFactory.HELVETICA_BOLD,
                    20,
                    AZUL_PRINCIPAL
            );

            agregarCabeceraCorporativa(
                    documento,
                    comunidad,
                    marca,
                    nombreAplicacion,
                    textoPequeno
            );

            agregarTituloRecibo(
                    documento,
                    recibo,
                    titulo,
                    referencia
            );

            agregarResumenSuperior(
                    documento,
                    recibo,
                    vecino,
                    textoPequeno,
                    textoDestacado
            );

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
                    textoDestacado
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
            agregarFila(
                    tablaComunidad,
                    "Cuenta de cargo",
                    enmascararIban(comunidad.getIban()),
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
                    textoDestacado
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
            agregarFila(
                    tablaVecino,
                    "Referencia de mandato",
                    valor(vecino.getReferenciaMandato()),
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
                    textoDestacado
            );
            agregarFila(
                    tablaRecibo,
                    "Tipo",
                    formatearEstado(recibo.getTipoRemesa()),
                    etiqueta,
                    texto
            );
            agregarFila(
                    tablaRecibo,
                    "Estado",
                    formatearEstado(recibo.getEstado()),
                    etiqueta,
                    texto
            );

            if (tieneTexto(recibo.getEtiquetaExtra())) {
                agregarFila(
                        tablaRecibo,
                        "Versión / periodo",
                        valor(recibo.getEtiquetaExtra()),
                        etiqueta,
                        texto
                );
            }

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

            agregarTotal(
                    documento,
                    recibo,
                    importe,
                    textoPequeno
            );

            agregarAviso(
                    documento,
                    recibo,
                    textoPequeno
            );

            agregarPie(
                    documento,
                    comunidad,
                    textoPequeno
            );

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
        if (comunidad == null) {
            throw new IllegalArgumentException(
                    "La comunidad es obligatoria para generar el mandato SEPA."
            );
        }

        if (vecino == null) {
            throw new IllegalArgumentException(
                    "El propietario es obligatorio para generar el mandato SEPA."
            );
        }

        if (!tieneTexto(comunidad.getNombre())) {
            throw new IllegalArgumentException(
                    "La comunidad debe tener nombre."
            );
        }

        if (!tieneTexto(comunidad.getIdentificadorAcreedor())) {
            throw new IllegalArgumentException(
                    "La comunidad debe tener identificador de acreedor SEPA."
            );
        }

        if (!tieneTexto(vecino.getNombre())) {
            throw new IllegalArgumentException(
                    "El propietario debe tener nombre."
            );
        }

        if (!tieneTexto(vecino.getIban())) {
            throw new IllegalArgumentException(
                    "El propietario debe tener IBAN."
            );
        }

        if (!tieneTexto(vecino.getReferenciaMandato())) {
            throw new IllegalArgumentException(
                    "El propietario debe tener referencia de mandato."
            );
        }

        ByteArrayOutputStream salida =
                new ByteArrayOutputStream();

        Document documento = new Document(
                PageSize.A4,
                36,
                36,
                26,
                26
        );

        try {
            PdfWriter.getInstance(
                    documento,
                    salida
            );

            documento.open();

            Font titulo = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    15,
                    Color.BLACK
            );

            Font subtitulo = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    9,
                    Color.WHITE
            );

            Font etiqueta = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    8,
                    Color.DARK_GRAY
            );

            Font texto = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    8,
                    Color.BLACK
            );

            Font textoLegal = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    8,
                    Color.BLACK
            );

            Paragraph encabezado = new Paragraph(
                    "ORDEN DE DOMICILIACIÓN DE ADEUDO DIRECTO SEPA (CORE)",
                    titulo
            );
            encabezado.setAlignment(
                    Element.ALIGN_CENTER
            );
            encabezado.setSpacingAfter(10);
            documento.add(encabezado);

            agregarSeccionMandato(
                    documento,
                    "DATOS DEL ACREEDOR",
                    subtitulo
            );

            PdfPTable tablaAcreedor =
                    crearTablaDatosMandato();

            agregarFilaMandato(
                    tablaAcreedor,
                    "Nombre del acreedor",
                    valor(comunidad.getNombre()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaAcreedor,
                    "Identificador acreedor",
                    valor(comunidad.getIdentificadorAcreedor()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaAcreedor,
                    "Dirección",
                    valor(comunidad.getDireccion()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaAcreedor,
                    "Código postal / Población",
                    unirDireccion(
                            comunidad.getCodigoPostal(),
                            comunidad.getPoblacion(),
                            null,
                            null
                    ),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaAcreedor,
                    "Provincia",
                    valor(comunidad.getProvincia()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaAcreedor,
                    "País",
                    valor(comunidad.getPaiscod()),
                    etiqueta,
                    texto
            );

            documento.add(tablaAcreedor);

            agregarSeccionMandato(
                    documento,
                    "DATOS DEL MANDATO",
                    subtitulo
            );

            PdfPTable tablaMandato =
                    crearTablaDatosMandato();

            agregarFilaMandato(
                    tablaMandato,
                    "Referencia del mandato",
                    valor(vecino.getReferenciaMandato()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaMandato,
                    "Tipo de pago",
                    "RECURRENTE",
                    etiqueta,
                    texto
            );

            documento.add(tablaMandato);

            Paragraph legal = new Paragraph(
                    "Mediante la firma de esta orden de domiciliación, "
                            + "el deudor autoriza (A) al acreedor a enviar "
                            + "instrucciones a la entidad del deudor para "
                            + "adeudar su cuenta y (B) a la entidad para "
                            + "efectuar los adeudos en su cuenta siguiendo "
                            + "las instrucciones del acreedor. Como parte "
                            + "de sus derechos, el deudor está legitimado "
                            + "al reembolso por su entidad en los términos "
                            + "y condiciones del contrato suscrito con la "
                            + "misma. La solicitud de reembolso deberá "
                            + "efectuarse dentro de las ocho semanas que "
                            + "siguen a la fecha de adeudo en cuenta. Puede "
                            + "obtener información adicional sobre sus "
                            + "derechos en su entidad financiera.",
                    textoLegal
            );
            legal.setAlignment(
                    Element.ALIGN_JUSTIFIED
            );
            legal.setLeading(10.5f);
            legal.setSpacingBefore(8);
            legal.setSpacingAfter(6);
            documento.add(legal);

            agregarSeccionMandato(
                    documento,
                    "DATOS DEL DEUDOR",
                    subtitulo
            );

            PdfPTable tablaDeudor =
                    crearTablaDatosMandato();

            agregarFilaMandato(
                    tablaDeudor,
                    "Nombre del deudor",
                    valor(vecino.getNombre()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaDeudor,
                    "NIF / NIE",
                    valor(vecino.getNif()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaDeudor,
                    "Propiedad",
                    valor(vecino.getVivienda()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaDeudor,
                    "Dirección",
                    valor(vecino.getDireccion()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaDeudor,
                    "Código postal / Población",
                    unirDireccion(
                            vecino.getCodigoPostal(),
                            vecino.getPoblacion(),
                            null,
                            null
                    ),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaDeudor,
                    "Provincia",
                    valor(vecino.getProvincia()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaDeudor,
                    "País",
                    valor(vecino.getPaisCod()),
                    etiqueta,
                    texto
            );

            agregarFilaMandato(
                    tablaDeudor,
                    "IBAN",
                    valor(vecino.getIban()),
                    etiqueta,
                    texto
            );

            if (tieneTexto(vecino.getBic())) {
                agregarFilaMandato(
                        tablaDeudor,
                        "BIC",
                        valor(vecino.getBic()),
                        etiqueta,
                        texto
                );
            }

            documento.add(tablaDeudor);

            agregarSeccionMandato(
                    documento,
                    "FIRMA DEL MANDATO",
                    subtitulo
            );

            Paragraph firma = new Paragraph(
                    "Lugar de firma: __________________________________________\n"
                            + "Fecha de firma (dd/mm/aaaa): ________________________\n"
                            + "Firma(s) del deudor(es):\n"
                            + "____________________________________________________",
                    texto
            );
            firma.setLeading(16);
            firma.setSpacingBefore(2);
            documento.add(firma);

            documento.close();
            return salida.toByteArray();

        } catch (Exception excepcion) {
            if (documento.isOpen()) {
                documento.close();
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

    private void agregarCabeceraCorporativa(
            Document documento,
            Comunidad comunidad,
            Font marca,
            Font nombreAplicacion,
            Font textoPequeno
    ) throws Exception {
        PdfPTable cabecera = new PdfPTable(
                new float[]{0.8f, 3.2f, 4.8f}
        );
        cabecera.setWidthPercentage(100);
        cabecera.setSpacingAfter(10);

        PdfPCell insignia = new PdfPCell(
                new Phrase("GU", marca)
        );
        insignia.setHorizontalAlignment(Element.ALIGN_CENTER);
        insignia.setVerticalAlignment(Element.ALIGN_MIDDLE);
        insignia.setBackgroundColor(AZUL_PRINCIPAL);
        insignia.setPaddingTop(12);
        insignia.setPaddingBottom(12);
        insignia.setBorder(Rectangle.NO_BORDER);
        cabecera.addCell(insignia);

        PdfPCell marcaCelda = new PdfPCell();
        marcaCelda.addElement(new Paragraph(
                "Gombeth Urban",
                nombreAplicacion
        ));
        marcaCelda.addElement(new Paragraph(
                "Gestión profesional de comunidades",
                textoPequeno
        ));
        marcaCelda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        marcaCelda.setPaddingLeft(10);
        marcaCelda.setBorder(Rectangle.NO_BORDER);
        cabecera.addCell(marcaCelda);

        PdfPCell comunidadCelda = new PdfPCell();
        Paragraph nombre = new Paragraph(
                valor(comunidad.getNombre()),
                fuente(
                        FontFactory.HELVETICA_BOLD,
                        10,
                        AZUL_PRINCIPAL
                )
        );
        nombre.setAlignment(Element.ALIGN_RIGHT);
        comunidadCelda.addElement(nombre);

        Paragraph direccion = new Paragraph(
                direccionComunidad(comunidad),
                textoPequeno
        );
        direccion.setAlignment(Element.ALIGN_RIGHT);
        comunidadCelda.addElement(direccion);

        comunidadCelda.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );
        comunidadCelda.setBorder(Rectangle.NO_BORDER);
        cabecera.addCell(comunidadCelda);

        documento.add(cabecera);

        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);

        PdfPCell celda = new PdfPCell();
        celda.setFixedHeight(2);
        celda.setBackgroundColor(AZUL_SECUNDARIO);
        celda.setBorder(Rectangle.NO_BORDER);
        linea.addCell(celda);

        documento.add(linea);
    }

    private void agregarTituloRecibo(
            Document documento,
            ContabilidadRecibo recibo,
            Font titulo,
            Font referencia
    ) throws Exception {
        Paragraph encabezado = new Paragraph(
                "RECIBO",
                titulo
        );
        encabezado.setAlignment(Element.ALIGN_CENTER);
        encabezado.setSpacingBefore(8);
        encabezado.setSpacingAfter(2);
        documento.add(encabezado);

        Paragraph numero = new Paragraph(
                "Referencia "
                        + recibo.getId()
                        + " · Emitido "
                        + fecha(recibo.getFechaEmision()),
                referencia
        );
        numero.setAlignment(Element.ALIGN_CENTER);
        numero.setSpacingAfter(8);
        documento.add(numero);
    }

    private void agregarResumenSuperior(
            Document documento,
            ContabilidadRecibo recibo,
            Vecino vecino,
            Font etiqueta,
            Font valor
    ) throws Exception {
        PdfPTable resumen = new PdfPTable(
                new float[]{1.7f, 2.7f, 1.7f, 2.1f}
        );
        resumen.setWidthPercentage(100);
        resumen.setSpacingAfter(5);

        agregarTarjetaResumen(
                resumen,
                "PROPIETARIO",
                valor(vecino.getNombre()),
                etiqueta,
                valor
        );
        agregarTarjetaResumen(
                resumen,
                "VIVIENDA",
                valor(vecino.getVivienda()),
                etiqueta,
                valor
        );
        agregarTarjetaResumen(
                resumen,
                "ESTADO",
                formatearEstado(recibo.getEstado()),
                etiqueta,
                valor
        );
        agregarTarjetaResumen(
                resumen,
                "IMPORTE",
                importe(recibo.getImporte()) + " €",
                etiqueta,
                valor
        );

        documento.add(resumen);
    }

    private void agregarTarjetaResumen(
            PdfPTable tabla,
            String etiqueta,
            String contenido,
            Font fuenteEtiqueta,
            Font fuenteContenido
    ) {
        PdfPCell celda = new PdfPCell();
        celda.addElement(new Paragraph(
                etiqueta,
                fuenteEtiqueta
        ));
        celda.addElement(new Paragraph(
                contenido,
                fuenteContenido
        ));
        celda.setPadding(6);
        celda.setBackgroundColor(AZUL_MUY_CLARO);
        celda.setBorderColor(GRIS_BORDE);
        tabla.addCell(celda);
    }

    private void agregarTotal(
            Document documento,
            ContabilidadRecibo recibo,
            Font fuenteImporte,
            Font fuenteDetalle
    ) throws Exception {
        PdfPTable tablaImporte =
                new PdfPTable(new float[]{4.2f, 2.8f});
        tablaImporte.setWidthPercentage(100);
        tablaImporte.setSpacingBefore(10);

        PdfPCell detalle = new PdfPCell(
                new Phrase(
                        "Importe total del recibo",
                        fuenteDetalle
                )
        );
        detalle.setVerticalAlignment(Element.ALIGN_MIDDLE);
        detalle.setPadding(10);
        detalle.setBackgroundColor(GRIS_FONDO);
        detalle.setBorderColor(AZUL_PRINCIPAL);
        tablaImporte.addCell(detalle);

        PdfPCell celdaImporte = new PdfPCell(
                new Phrase(
                        importe(recibo.getImporte()) + " €",
                        fuenteImporte
                )
        );
        celdaImporte.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );
        celdaImporte.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );
        celdaImporte.setPadding(10);
        celdaImporte.setBackgroundColor(GRIS_FONDO);
        celdaImporte.setBorderColor(AZUL_PRINCIPAL);
        tablaImporte.addCell(celdaImporte);

        documento.add(tablaImporte);
    }

    private void agregarAviso(
            Document documento,
            ContabilidadRecibo recibo,
            Font fuente
    ) throws Exception {
        boolean cobrado = "COBRADO".equalsIgnoreCase(
                valor(recibo.getEstado())
        );

        PdfPTable aviso = new PdfPTable(1);
        aviso.setWidthPercentage(100);
        aviso.setSpacingBefore(8);

        String texto = cobrado
                ? "Este recibo figura como cobrado. "
                + "Conserve el documento como justificante."
                : "Este documento informa del recibo emitido. "
                + "La situación definitiva del cobro dependerá "
                + "de la confirmación bancaria.";

        PdfPCell celda = new PdfPCell(
                new Phrase(texto, fuente)
        );
        celda.setPadding(7);
        celda.setBackgroundColor(
                cobrado
                        ? VERDE_FONDO
                        : NARANJA_FONDO
        );
        celda.setBorderColor(
                cobrado
                        ? VERDE
                        : NARANJA
        );
        aviso.addCell(celda);

        documento.add(aviso);
    }

    private void agregarPie(
            Document documento,
            Comunidad comunidad,
            Font fuente
    ) throws Exception {
        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        linea.setSpacingBefore(8);

        PdfPCell separador = new PdfPCell();
        separador.setFixedHeight(1);
        separador.setBackgroundColor(GRIS_BORDE);
        separador.setBorder(Rectangle.NO_BORDER);
        linea.addCell(separador);
        documento.add(linea);

        Paragraph legal = new Paragraph(
                "Documento generado electrónicamente por "
                        + "Gombeth Urban para "
                        + valor(comunidad.getNombre())
                        + ". Este documento no sustituye al "
                        + "justificante bancario cuando resulte exigible.",
                fuente
        );
        legal.setAlignment(Element.ALIGN_CENTER);
        legal.setSpacingBefore(5);
        documento.add(legal);

        Paragraph fechaGeneracion = new Paragraph(
                "Generado el "
                        + FECHA_ES.format(LocalDate.now())
                        + " · www.jfgb.es",
                fuente
        );
        fechaGeneracion.setAlignment(Element.ALIGN_CENTER);
        fechaGeneracion.setSpacingBefore(2);
        documento.add(fechaGeneracion);
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
        tabla.setSpacingBefore(7);
        tabla.setSpacingAfter(3);

        PdfPCell celda = new PdfPCell(
                new Phrase(titulo, fuente)
        );
        celda.setBackgroundColor(AZUL_PRINCIPAL);
        celda.setPaddingTop(5);
        celda.setPaddingBottom(5);
        celda.setPaddingLeft(9);
        celda.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(celda);

        documento.add(tabla);
    }

    private PdfPTable crearTablaDatos()
            throws Exception {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.55f, 4.45f});
        tabla.setHeaderRows(0);
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
        celdaEtiqueta.setPadding(5f);
        celdaEtiqueta.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );
        celdaEtiqueta.setBorderColor(GRIS_BORDE);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaContenido = new PdfPCell(
                new Phrase(contenido, fuenteContenido)
        );
        celdaContenido.setPadding(5f);
        celdaContenido.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );
        celdaContenido.setBorderColor(GRIS_BORDE);
        tabla.addCell(celdaContenido);
    }

    private void agregarSeccionMandato(
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
        celda.setBackgroundColor(AZUL_MANDATO);
        celda.setPadding(7);
        celda.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(celda);

        documento.add(tabla);
    }

    private PdfPTable crearTablaDatosMandato()
            throws Exception {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 4.5f});
        return tabla;
    }

    private void agregarFilaMandato(
            PdfPTable tabla,
            String etiqueta,
            String contenido,
            Font fuenteEtiqueta,
            Font fuenteContenido
    ) {
        PdfPCell celdaEtiqueta = new PdfPCell(
                new Phrase(etiqueta, fuenteEtiqueta)
        );
        celdaEtiqueta.setBackgroundColor(GRIS_FONDO_MANDATO);
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

    private String enmascararIban(
            String iban
    ) {
        if (!tieneTexto(iban)) {
            return "";
        }

        String limpio = iban
                .replace(" ", "")
                .trim();

        if (limpio.length() <= 8) {
            return limpio;
        }

        return limpio.substring(0, 4)
                + " **** **** **** "
                + limpio.substring(limpio.length() - 4);
    }

    private String formatearEstado(
            String estado
    ) {
        if (!tieneTexto(estado)) {
            return "";
        }

        String limpio = estado
                .trim()
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT);

        return limpio.substring(0, 1)
                .toUpperCase(Locale.ROOT)
                + limpio.substring(1);
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

    private Font fuente(
            String nombre,
            float tamanio,
            Color color
    ) {
        return FontFactory.getFont(
                nombre,
                tamanio,
                color
        );
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