package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SepaCoreXmlService {

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

        String msgId = limpiar(remesa.getIdentificadorFichero());

        String fechaCreacion =
                LocalDateTime.now()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        int numeroOperaciones = lineas.size();

        BigDecimal total = lineas.stream()
                .map(l -> l.getImporte() == null ? BigDecimal.ZERO : l.getImporte())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, Vecino> mapaVecinos =
                vecinos.stream()
                        .collect(Collectors.toMap(
                                Vecino::getId,
                                v -> v
                        ));

        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.008.001.02\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");

        xml.append("  <CstmrDrctDbtInitn>\n");

        xml.append("    <GrpHdr>\n");
        xml.append("      <MsgId>").append(esc(msgId)).append("</MsgId>\n");
        xml.append("      <CreDtTm>").append(fechaCreacion).append("</CreDtTm>\n");
        xml.append("      <NbOfTxs>").append(numeroOperaciones).append("</NbOfTxs>\n");
        xml.append("      <CtrlSum>").append(total).append("</CtrlSum>\n");
        xml.append("      <InitgPty>\n");
        xml.append("        <Nm>").append(esc(comunidad.getNombre())).append("</Nm>\n");
        xml.append("      </InitgPty>\n");
        xml.append("    </GrpHdr>\n");

        xml.append("    <PmtInf>\n");
        xml.append("      <PmtInfId>").append(esc(msgId)).append("-PMT</PmtInfId>\n");
        xml.append("      <PmtMtd>DD</PmtMtd>\n");
        xml.append("      <BtchBookg>true</BtchBookg>\n");
        xml.append("      <NbOfTxs>").append(numeroOperaciones).append("</NbOfTxs>\n");
        xml.append("      <CtrlSum>").append(total).append("</CtrlSum>\n");

        xml.append("      <PmtTpInf>\n");
        xml.append("        <SvcLvl><Cd>SEPA</Cd></SvcLvl>\n");
        xml.append("        <LclInstrm><Cd>CORE</Cd></LclInstrm>\n");
        xml.append("        <SeqTp>RCUR</SeqTp>\n");
        xml.append("      </PmtTpInf>\n");

        xml.append("      <ReqdColltnDt>")
                .append(remesa.getFechaCobro())
                .append("</ReqdColltnDt>\n");

        xml.append("      <Cdtr>\n");
        xml.append("        <Nm>").append(esc(comunidad.getNombre())).append("</Nm>\n");
        xml.append("      </Cdtr>\n");

        xml.append("      <CdtrAcct>\n");
        xml.append("        <Id><IBAN>").append(esc(comunidad.getIban())).append("</IBAN></Id>\n");
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
        xml.append("              <Id>").append(esc(comunidad.getIdentificadorAcreedor())).append("</Id>\n");
        xml.append("              <SchmeNm><Prtry>SEPA</Prtry></SchmeNm>\n");
        xml.append("            </Othr>\n");
        xml.append("          </PrvtId>\n");
        xml.append("        </Id>\n");
        xml.append("      </CdtrSchmeId>\n");

        for (RemesaLinea linea : lineas) {

            Vecino vecino =
                    mapaVecinos.get(linea.getVecinoId());

            if (vecino == null) {
                continue;
            }

            String endToEndId =
                    "CP"
                            + comunidad.getId()
                            + "-"
                            + remesa.getFechaCobro().getYear()
                            + String.format("%02d", remesa.getFechaCobro().getMonthValue())
                            + "-REC"
                            + linea.getReciboContableId();

            xml.append("      <DrctDbtTxInf>\n");

            xml.append("        <PmtId>\n");
            xml.append("          <EndToEndId>")
                    .append(esc(endToEndId))
                    .append("</EndToEndId>\n");
            xml.append("        </PmtId>\n");

            xml.append("        <InstdAmt Ccy=\"EUR\">")
                    .append(linea.getImporte() == null ? BigDecimal.ZERO : linea.getImporte())
                    .append("</InstdAmt>\n");

            xml.append("        <DrctDbtTx>\n");
            xml.append("          <MndtRltdInf>\n");
            xml.append("            <MndtId>")
                    .append(esc(vecino.getReferenciaMandato()))
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
            xml.append("          <Nm>").append(esc(vecino.getNombre())).append("</Nm>\n");
            xml.append("        </Dbtr>\n");

            xml.append("        <DbtrAcct>\n");
            xml.append("          <Id><IBAN>")
                    .append(esc(vecino.getIban()))
                    .append("</IBAN></Id>\n");
            xml.append("        </DbtrAcct>\n");

            xml.append("        <RmtInf>\n");

            List<RemesaLineaConcepto> conceptos =
                    remesaLineaConceptoRepository
                            .findByRemesaLineaIdOrderByOrdenAsc(
                                    linea.getId()
                            );

            if (conceptos.isEmpty()) {
                xml.append("          <Ustrd>")
                        .append(esc(linea.getConcepto()))
                        .append("</Ustrd>\n");
            } else {
                for (RemesaLineaConcepto concepto : conceptos) {
                    xml.append("          <Ustrd>")
                            .append(esc(concepto.getDescripcion()))
                            .append("</Ustrd>\n");
                }
            }

            xml.append("        </RmtInf>\n");
            xml.append("      </DrctDbtTxInf>\n");
        }

        xml.append("    </PmtInf>\n");
        xml.append("  </CstmrDrctDbtInitn>\n");
        xml.append("</Document>\n");

        return xml.toString();
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

        return value
                .replaceAll("[^A-Za-z0-9\\-]", "")
                .trim();
    }
}
