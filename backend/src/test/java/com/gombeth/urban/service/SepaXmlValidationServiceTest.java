package com.gombeth.urban.service;

import com.gombeth.urban.dto.SepaValidacionResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SepaXmlValidationServiceTest {

    private SepaXmlValidationService service;

    @BeforeEach
    void configurar() {
        service = new SepaXmlValidationService();
    }

    @Test
    void aceptaXmlSepaValidoSegunXsdOficial() {
        SepaValidacionResultado resultado =
                service.validar(
                        crearXmlValido()
                );

        assertTrue(
                resultado.isValida(),
                () -> String.join(
                        " | ",
                        resultado.getErrores()
                )
        );
    }

    @Test
    void rechazaXmlVacio() {
        SepaValidacionResultado resultado =
                service.validar(
                        ""
                );

        assertFalse(
                resultado.isValida()
        );

        assertContieneError(
                resultado,
                "vacío"
        );
    }

    @Test
    void rechazaXmlMalFormado() {
        SepaValidacionResultado resultado =
                service.validar(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Document>
                            <CstmrDrctDbtInitn>
                        </Document>
                        """
                );

        assertFalse(
                resultado.isValida()
        );

        assertContieneError(
                resultado,
                "no cumple"
        );
    }

    @Test
    void rechazaXmlConNamespaceIncorrecto() {
        String xml =
                crearXmlValido()
                        .replace(
                                "urn:iso:std:iso:20022:tech:xsd:pain.008.001.08",
                                "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02"
                        );

        SepaValidacionResultado resultado =
                service.validar(
                        xml
                );

        assertFalse(
                resultado.isValida()
        );

        assertContieneError(
                resultado,
                "no cumple"
        );
    }

    @Test
    void rechazaXmlSinContenidoObligatorio() {
        SepaValidacionResultado resultado =
                service.validar(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.008.001.08">
                            <CstmrDrctDbtInitn/>
                        </Document>
                        """
                );

        assertFalse(
                resultado.isValida()
        );

        assertContieneError(
                resultado,
                "no cumple"
        );
    }

    private void assertContieneError(
            SepaValidacionResultado resultado,
            String textoEsperado
    ) {
        assertTrue(
                resultado.getErrores()
                        .stream()
                        .anyMatch(error ->
                                error.toLowerCase()
                                        .contains(
                                                textoEsperado.toLowerCase()
                                        )
                        ),
                () -> "Errores obtenidos: "
                        + String.join(
                        " | ",
                        resultado.getErrores()
                )
        );
    }

    private String crearXmlValido() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.008.001.08"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                    <CstmrDrctDbtInitn>
                        <GrpHdr>
                            <MsgId>REMESA-PRUEBA-1</MsgId>
                            <CreDtTm>2026-07-25T16:00:00</CreDtTm>
                            <NbOfTxs>1</NbOfTxs>
                            <CtrlSum>25.50</CtrlSum>
                            <InitgPty>
                                <Nm>Presentador de pruebas</Nm>
                                <Id>
                                    <OrgId>
                                        <Othr>
                                            <Id>ES81ZZZB12345675</Id>
                                        </Othr>
                                    </OrgId>
                                </Id>
                            </InitgPty>
                        </GrpHdr>

                        <PmtInf>
                            <PmtInfId>REMESA-PRUEBA-1-PMT</PmtInfId>
                            <PmtMtd>DD</PmtMtd>
                            <BtchBookg>true</BtchBookg>
                            <NbOfTxs>1</NbOfTxs>
                            <CtrlSum>25.50</CtrlSum>

                            <PmtTpInf>
                                <SvcLvl>
                                    <Cd>SEPA</Cd>
                                </SvcLvl>
                                <LclInstrm>
                                    <Cd>CORE</Cd>
                                </LclInstrm>
                                <SeqTp>RCUR</SeqTp>
                            </PmtTpInf>

                            <ReqdColltnDt>2026-10-05</ReqdColltnDt>

                            <Cdtr>
                                <Nm>Comunidad de prueba</Nm>
                            </Cdtr>

                            <CdtrAcct>
                                <Id>
                                    <IBAN>ES9121000418450200051332</IBAN>
                                </Id>
                            </CdtrAcct>

                            <CdtrAgt>
                                <FinInstnId>
                                    <Othr>
                                        <Id>NOTPROVIDED</Id>
                                    </Othr>
                                </FinInstnId>
                            </CdtrAgt>

                            <ChrgBr>SLEV</ChrgBr>

                            <CdtrSchmeId>
                                <Id>
                                    <PrvtId>
                                        <Othr>
                                            <Id>ES12ZZZ12345678</Id>
                                            <SchmeNm>
                                                <Prtry>SEPA</Prtry>
                                            </SchmeNm>
                                        </Othr>
                                    </PrvtId>
                                </Id>
                            </CdtrSchmeId>

                            <DrctDbtTxInf>
                                <PmtId>
                                    <EndToEndId>RECIBO-PRUEBA-1</EndToEndId>
                                </PmtId>

                                <InstdAmt Ccy="EUR">25.50</InstdAmt>

                                <DrctDbtTx>
                                    <MndtRltdInf>
                                        <MndtId>MANDATO-PRUEBA-1</MndtId>
                                        <DtOfSgntr>2020-05-15</DtOfSgntr>
                                    </MndtRltdInf>
                                </DrctDbtTx>

                                <DbtrAgt>
                                    <FinInstnId>
                                        <Othr>
                                            <Id>NOTPROVIDED</Id>
                                        </Othr>
                                    </FinInstnId>
                                </DbtrAgt>

                                <Dbtr>
                                    <Nm>Propietario de prueba</Nm>
                                </Dbtr>

                                <DbtrAcct>
                                    <Id>
                                        <IBAN>ES7921000813610123456789</IBAN>
                                    </Id>
                                </DbtrAcct>

                                <RmtInf>
                                    <Ustrd>Cuota ordinaria</Ustrd>
                                </RmtInf>
                            </DrctDbtTxInf>
                        </PmtInf>
                    </CstmrDrctDbtInitn>
                </Document>
                """;
    }
}