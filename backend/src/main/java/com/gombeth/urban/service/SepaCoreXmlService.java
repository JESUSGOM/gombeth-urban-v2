package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SepaCoreXmlService {

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RemesaLineaConceptoRepository remesaLineaConceptoRepository;

    public SepaCoreXmlService(
            RemesaLineaConceptoRepository remesaLineaConceptoRepository
    ) {
        this.remesaLineaConceptoRepository = remesaLineaConceptoRepository;
    }

    public String generarXmlCore(
            FicheroGenerado remesa,
            Comunidad comunidad,
            List<RemesaLinea> lineas,
            List<Vecino> vecinos
    ) {

        Map<Long, Vecino> mapaVecinos =
                vecinos.stream()
                        .collect(Collectors.toMap(
                                Vecino::getId,
                                vecino -> vecino,
                                (primero, segundo) -> primero
                        ));

        List<RemesaLinea> lineasValidas =
                lineas.stream()
                        .filter(linea -> mapaVecinos.containsKey(linea.getVecinoId()))
                        .toList();

        String msgId =
                validarIdentificador(
                        "Identificación del mensaje",
                        limpiar(remesa.getIdentificadorFichero()),
                        35
                );

        String pmtInfId = construirPmtInfId(msgId);

        String fechaCreacion =
                LocalDateTime.now().format(FORMATO_FECHA_HORA);

        int numeroOperaciones = lineasValidas.size();

        BigDecimal total =
                lineasValidas.stream()
                        .map(linea -> importe(linea.getImporte()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

        String nombreIniciador =
                tieneTexto(remesa.getPresentadorAlias())
                        ? remesa.getPresentadorAlias()
                        : comunidad.getNombre();

        String identificadorIniciador =
                tieneTexto(remesa.getPresentadorIdentificador())
                        ? remesa.getPresentadorIdentificador()
                        : comunidad.getIdentificadorAcreedor();

        identificadorIniciador =
                validarIdentificador(
                        "Identificador del presentador",
                        identificadorIniciador,
                        35
                );

        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.008.001.08\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");

        xml.append("  <CstmrDrctDbtInitn>\n");

        xml.append("    <GrpHdr>\n");
        xml.append("      <MsgId>")
                .append(esc(msgId))
                .append("</MsgId>\n");
        xml.append("      <CreDtTm>")
                .append(fechaCreacion)
                .append("</CreDtTm>\n");
        xml.append("      <NbOfTxs>")
                .append(numeroOperaciones)
                .append("</NbOfTxs>\n");
        xml.append("      <CtrlSum>")
                .append(formatearImporte(total))
                .append("</CtrlSum>\n");

        xml.append("      <InitgPty>\n");
        xml.append("        <Nm>")
                .append(esc(textoMaximo(nombreIniciador, 70)))
                .append("</Nm>\n");
        xml.append("        <Id>\n");

        if (esPersonaFisica(remesa.getPresentadorNifCif())) {
            xml.append("          <PrvtId>\n");
            xml.append("            <Othr>\n");
            xml.append("              <Id>")
                    .append(esc(identificadorIniciador))
                    .append("</Id>\n");
            xml.append("            </Othr>\n");
            xml.append("          </PrvtId>\n");
        } else {
            xml.append("          <OrgId>\n");
            xml.append("            <Othr>\n");
            xml.append("              <Id>")
                    .append(esc(identificadorIniciador))
                    .append("</Id>\n");
            xml.append("            </Othr>\n");
            xml.append("          </OrgId>\n");
        }

        xml.append("        </Id>\n");
        xml.append("      </InitgPty>\n");
        xml.append("    </GrpHdr>\n");

        xml.append("    <PmtInf>\n");
        xml.append("      <PmtInfId>")
                .append(esc(pmtInfId))
                .append("</PmtInfId>\n");
        xml.append("      <PmtMtd>DD</PmtMtd>\n");
        xml.append("      <BtchBookg>true</BtchBookg>\n");
        xml.append("      <NbOfTxs>")
                .append(numeroOperaciones)
                .append("</NbOfTxs>\n");
        xml.append("      <CtrlSum>")
                .append(formatearImporte(total))
                .append("</CtrlSum>\n");

        xml.append("      <PmtTpInf>\n");
        xml.append("        <SvcLvl><Cd>SEPA</Cd></SvcLvl>\n");
        xml.append("        <LclInstrm><Cd>CORE</Cd></LclInstrm>\n");
        xml.append("        <SeqTp>RCUR</SeqTp>\n");
        xml.append("      </PmtTpInf>\n");

        xml.append("      <ReqdColltnDt>")
                .append(remesa.getFechaCobro())
                .append("</ReqdColltnDt>\n");

        xml.append("      <Cdtr>\n");
        xml.append("        <Nm>")
                .append(esc(textoMaximo(comunidad.getNombre(), 70)))
                .append("</Nm>\n");
        xml.append("      </Cdtr>\n");

        xml.append("      <CdtrAcct>\n");
        xml.append("        <Id><IBAN>")
                .append(esc(normalizarIban(comunidad.getIban())))
                .append("</IBAN></Id>\n");
        xml.append("      </CdtrAcct>\n");

        xml.append("      <CdtrAgt>\n");
        xml.append("        <FinInstnId>\n");
        xml.append("          <Othr><Id>NOTPROVIDED</Id></Othr>\n");
        xml.append("        </FinInstnId>\n");
        xml.append("      </CdtrAgt>\n");

        xml.append("      <ChrgBr>SLEV</ChrgBr>\n");

        xml.append("      <CdtrSchmeId>\n");
        xml.append("        <Id>\n");
        xml.append("          <PrvtId>\n");
        xml.append("            <Othr>\n");
        xml.append("              <Id>")
                .append(esc(validarIdentificador(
                        "Identificador del acreedor",
                        comunidad.getIdentificadorAcreedor(),
                        35
                )))
                .append("</Id>\n");
        xml.append("              <SchmeNm><Prtry>SEPA</Prtry></SchmeNm>\n");
        xml.append("            </Othr>\n");
        xml.append("          </PrvtId>\n");
        xml.append("        </Id>\n");
        xml.append("      </CdtrSchmeId>\n");

        for (RemesaLinea linea : lineasValidas) {

            Vecino vecino = mapaVecinos.get(linea.getVecinoId());

            String endToEndId =
                    validarIdentificador(
                            "Identificación de extremo a extremo",
                            "CP"
                                    + comunidad.getId()
                                    + "-"
                                    + remesa.getFechaCobro().getYear()
                                    + String.format("%02d", remesa.getFechaCobro().getMonthValue())
                                    + "-REC"
                                    + linea.getReciboContableId(),
                            35
                    );

            xml.append("      <DrctDbtTxInf>\n");

            xml.append("        <PmtId>\n");
            xml.append("          <EndToEndId>")
                    .append(esc(endToEndId))
                    .append("</EndToEndId>\n");
            xml.append("        </PmtId>\n");

            xml.append("        <InstdAmt Ccy=\"EUR\">")
                    .append(formatearImporte(linea.getImporte()))
                    .append("</InstdAmt>\n");

            xml.append("        <DrctDbtTx>\n");
            xml.append("          <MndtRltdInf>\n");
            xml.append("            <MndtId>")
                    .append(esc(validarIdentificador(
                            "Referencia del mandato",
                            vecino.getReferenciaMandato(),
                            35
                    )))
                    .append("</MndtId>\n");
            xml.append("            <DtOfSgntr>")
                    .append(vecino.getFechaMandato())
                    .append("</DtOfSgntr>\n");
            xml.append("          </MndtRltdInf>\n");
            xml.append("        </DrctDbtTx>\n");

            xml.append("        <DbtrAgt>\n");
            xml.append("          <FinInstnId>\n");
            xml.append("            <Othr><Id>NOTPROVIDED</Id></Othr>\n");
            xml.append("          </FinInstnId>\n");
            xml.append("        </DbtrAgt>\n");

            xml.append("        <Dbtr>\n");
            xml.append("          <Nm>")
                    .append(esc(textoMaximo(vecino.getNombre(), 70)))
                    .append("</Nm>\n");
            xml.append("        </Dbtr>\n");

            xml.append("        <DbtrAcct>\n");
            xml.append("          <Id><IBAN>")
                    .append(esc(normalizarIban(vecino.getIban())))
                    .append("</IBAN></Id>\n");
            xml.append("        </DbtrAcct>\n");

            List<RemesaLineaConcepto> conceptos =
                    remesaLineaConceptoRepository
                            .findByRemesaLineaIdOrderByOrdenAsc(linea.getId());

            String informacionRemesa =
                    construirInformacionRemesa(linea, conceptos);

            if (tieneTexto(informacionRemesa)) {
                xml.append("        <RmtInf>\n");
                xml.append("          <Ustrd>")
                        .append(esc(informacionRemesa))
                        .append("</Ustrd>\n");
                xml.append("        </RmtInf>\n");
            }

            xml.append("      </DrctDbtTxInf>\n");
        }

        xml.append("    </PmtInf>\n");
        xml.append("  </CstmrDrctDbtInitn>\n");
        xml.append("</Document>\n");

        return xml.toString();
    }

    private String construirPmtInfId(String msgId) {
        String sufijo = "-PMT";
        int longitudBase = 35 - sufijo.length();
        String base = msgId.length() <= longitudBase
                ? msgId
                : msgId.substring(0, longitudBase);
        return base + sufijo;
    }

    private String construirInformacionRemesa(
            RemesaLinea linea,
            List<RemesaLineaConcepto> conceptos
    ) {
        String texto;

        if (conceptos == null || conceptos.isEmpty()) {
            texto = linea.getConcepto();
        } else {
            texto = conceptos.stream()
                    .map(RemesaLineaConcepto::getDescripcion)
                    .filter(this::tieneTexto)
                    .map(String::trim)
                    .collect(Collectors.joining(" - "));
        }

        return textoMaximo(texto, 140);
    }

    private BigDecimal importe(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatearImporte(BigDecimal value) {
        return importe(value).toPlainString();
    }

    private String validarIdentificador(
            String nombreCampo,
            String value,
            int longitudMaxima
    ) {
        if (!tieneTexto(value)) {
            throw new IllegalArgumentException(
                    nombreCampo + " es obligatorio para generar el XML SEPA."
            );
        }

        String normalizado = value.replaceAll("\\s+", "").trim();

        if (normalizado.length() > longitudMaxima) {
            throw new IllegalArgumentException(
                    nombreCampo
                            + " supera la longitud máxima de "
                            + longitudMaxima
                            + " caracteres."
            );
        }

        return normalizado;
    }

    private String normalizarIban(String value) {
        if (!tieneTexto(value)) {
            throw new IllegalArgumentException(
                    "El IBAN es obligatorio para generar el XML SEPA."
            );
        }

        String iban = value
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        if (iban.length() > 34) {
            throw new IllegalArgumentException(
                    "El IBAN supera la longitud máxima de 34 caracteres."
            );
        }

        return iban;
    }

    private boolean esPersonaFisica(String nifCif) {
        if (!tieneTexto(nifCif)) {
            return false;
        }

        String identificador = nifCif
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        return identificador.matches("[0-9]{8}[A-Z]")
                || identificador.matches("[XYZ][0-9]{7}[A-Z]");
    }

    private String textoMaximo(String value, int longitudMaxima) {
        if (!tieneTexto(value)) {
            return "";
        }

        String texto = value
                .replaceAll("\\s+", " ")
                .trim();

        return texto.length() <= longitudMaxima
                ? texto
                : texto.substring(0, longitudMaxima);
    }

    private boolean tieneTexto(String value) {
        return value != null && !value.isBlank();
    }

    private String esc(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String limpiar(String value) {
        if (value == null) {
            return "REMESA";
        }

        String limpio = value
                .replaceAll("[^A-Za-z0-9\\-]", "")
                .trim();

        return limpio.isBlank() ? "REMESA" : limpio;
    }
}