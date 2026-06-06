package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Vecino;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public byte[] generarMandatoSepa(
            Comunidad comunidad,
            Vecino vecino
    ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

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

            document.add(new Paragraph("DATOS DEL ACREEDOR:", subtitulo));
            document.add(new Paragraph("Nombre: " + valor(comunidad.getNombre()), normal));
            document.add(new Paragraph("ID Acreedor: " + valor(comunidad.getIdentificadorAcreedor()), normal));
            document.add(new Paragraph("Dirección: " + valor(comunidad.getDireccion()), normal));
            document.add(new Paragraph("Población: " + valor(comunidad.getPoblacion()), normal));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("DATOS DEL DEUDOR (PAGADOR):", subtitulo));
            document.add(new Paragraph("Nombre: " + valor(vecino.getNombre()), normal));
            document.add(new Paragraph("NIF: " + valor(vecino.getNif()), normal));
            document.add(new Paragraph("Propiedad: " + valor(vecino.getVivienda()), normal));
            document.add(new Paragraph("IBAN: " + valor(vecino.getIban()), normal));
            document.add(new Paragraph("Referencia mandato: " + valor(vecino.getReferenciaMandato()), normal));
            document.add(new Paragraph(" "));

            Paragraph textoLegal = new Paragraph(
                    "Mediante la firma de esta orden de domiciliación, " +
                            "el deudor autoriza al acreedor a enviar instrucciones " +
                            "a su entidad para adeudar su cuenta y a la entidad " +
                            "para adeudar los importes correspondientes de acuerdo " +
                            "con las instrucciones del acreedor.",
                    normal
            );

            textoLegal.setSpacingBefore(10);
            document.add(textoLegal);

            document.add(new Paragraph(
                    "\n\nFecha: _________________  Firma: ___________________________",
                    normal
            ));

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error generando mandato SEPA",
                    e
            );
        }
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }
}