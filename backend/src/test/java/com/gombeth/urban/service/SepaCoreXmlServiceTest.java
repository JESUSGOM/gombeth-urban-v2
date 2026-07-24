package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SepaCoreXmlServiceTest {

    @Test
    void generaPain00800108ConPresentadorAcreedorTotalesYUnUstrdPorAdeudo() {

        RemesaLineaConceptoRepository conceptoRepository =
                mock(RemesaLineaConceptoRepository.class);

        SepaCoreXmlService service =
                new SepaCoreXmlService(conceptoRepository);

        FicheroGenerado remesa = crearRemesa(
                "REM-18-20261005",
                LocalDate.of(2026, 10, 5),
                "Cajamar -- Presentador de pruebas",
                "ES81ZZZB12345675",
                "B12345675"
        );

        Comunidad comunidad = crearComunidad();

        RemesaLinea lineaPedro = crearLinea(
                10L,
                196L,
                1990L,
                "26.67",
                "Cuota ordinaria Pedro"
        );

        RemesaLinea lineaJesus = crearLinea(
                11L,
                198L,
                1992L,
                "20.01",
                "Cuota ordinaria Jesús"
        );

        Vecino pedro = crearVecino(
                196L,
                "Pedro Hernández Pérez",
                "ES5321007772457367365553",
                "MANDATO-PEDRO-2024"
        );

        Vecino jesus = crearVecino(
                198L,
                "Jesús Hernández Ossorio",
                "ES7400750144400600349113",
                "MANDATO-JESUS-2024"
        );

        List<RemesaLineaConcepto> conceptosPedro = List.of(
                crearConcepto("Cuota Comunidad Vivienda 1A"),
                crearConcepto("Cuota Garaje 8"),
                crearConcepto("Trastero 12")
        );

        List<RemesaLineaConcepto> conceptosJesus = List.of(
                crearConcepto("Cuota Comunidad Vivienda 3A"),
                crearConcepto("Cuota Garaje 37"),
                crearConcepto("Trastero 85")
        );

        when(
                conceptoRepository.findByRemesaLineaIdOrderByOrdenAsc(10L)
        ).thenReturn(conceptosPedro);

        when(
                conceptoRepository.findByRemesaLineaIdOrderByOrdenAsc(11L)
        ).thenReturn(conceptosJesus);

        String xml = service.generarXmlCore(
                remesa,
                comunidad,
                List.of(lineaPedro, lineaJesus),
                List.of(pedro, jesus)
        );

        assertTrue(
                xml.contains(
                        "xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.008.001.08\""
                )
        );

        assertFalse(
                xml.contains("pain.008.001.02")
        );

        assertTrue(
                xml.contains(
                        "<Nm>Cajamar -- Presentador de pruebas</Nm>"
                )
        );

        assertTrue(
                xml.contains(
                        "<OrgId>\n"
                                + "            <Othr>\n"
                                + "              <Id>ES81ZZZB12345675</Id>"
                )
        );

        assertTrue(
                xml.contains(
                        "<Nm>Comunidad de Propietarios Test</Nm>"
                )
        );

        assertTrue(
                xml.contains(
                        "<IBAN>ES3931908991912652753236</IBAN>"
                )
        );

        assertTrue(
                xml.contains(
                        "<Id>H12345674</Id>"
                )
        );

        assertEquals(
                2,
                contarApariciones(xml, "<NbOfTxs>2</NbOfTxs>")
        );

        assertEquals(
                2,
                contarApariciones(xml, "<CtrlSum>46.68</CtrlSum>")
        );

        assertEquals(
                2,
                contarApariciones(xml, "<DrctDbtTxInf>")
        );

        assertEquals(
                2,
                contarApariciones(xml, "<Ustrd>")
        );

        assertTrue(
                xml.contains(
                        "<Ustrd>Cuota Comunidad Vivienda 1A"
                                + " - Cuota Garaje 8"
                                + " - Trastero 12</Ustrd>"
                )
        );

        assertTrue(
                xml.contains(
                        "<Ustrd>Cuota Comunidad Vivienda 3A"
                                + " - Cuota Garaje 37"
                                + " - Trastero 85</Ustrd>"
                )
        );
    }

    @Test
    void usaPrvtIdCuandoElPresentadorEsPersonaFisica() {

        RemesaLineaConceptoRepository conceptoRepository =
                mock(RemesaLineaConceptoRepository.class);

        SepaCoreXmlService service =
                new SepaCoreXmlService(conceptoRepository);

        FicheroGenerado remesa = crearRemesa(
                "REM-PERSONA-FISICA",
                LocalDate.of(2026, 10, 5),
                "Presentador persona física",
                "PRESENTADOR-12345678Z",
                "12345678Z"
        );

        Comunidad comunidad = crearComunidad();

        RemesaLinea linea = crearLinea(
                20L,
                196L,
                3000L,
                "10.00",
                "Cuota ordinaria"
        );

        Vecino vecino = crearVecino(
                196L,
                "Pedro Hernández Pérez",
                "ES5321007772457367365553",
                "MANDATO-PEDRO-2024"
        );

        when(
                conceptoRepository.findByRemesaLineaIdOrderByOrdenAsc(20L)
        ).thenReturn(List.of());

        String xml = service.generarXmlCore(
                remesa,
                comunidad,
                List.of(linea),
                List.of(vecino)
        );

        assertTrue(
                xml.contains(
                        "<PrvtId>\n"
                                + "            <Othr>\n"
                                + "              <Id>PRESENTADOR-12345678Z</Id>"
                )
        );

        assertFalse(
                xml.contains("<OrgId>")
        );
    }

    @Test
    void usaLaComunidadComoIniciadoraEnRemesasHistoricas() {

        RemesaLineaConceptoRepository conceptoRepository =
                mock(RemesaLineaConceptoRepository.class);

        SepaCoreXmlService service =
                new SepaCoreXmlService(conceptoRepository);

        FicheroGenerado remesa = crearRemesa(
                "REM-HISTORICA",
                LocalDate.of(2026, 10, 5),
                null,
                null,
                null
        );

        Comunidad comunidad = crearComunidad();

        RemesaLinea linea = crearLinea(
                30L,
                196L,
                4000L,
                "15.00",
                "Cuota histórica"
        );

        Vecino vecino = crearVecino(
                196L,
                "Pedro Hernández Pérez",
                "ES5321007772457367365553",
                "MANDATO-PEDRO-2024"
        );

        when(
                conceptoRepository.findByRemesaLineaIdOrderByOrdenAsc(30L)
        ).thenReturn(List.of());

        String xml = service.generarXmlCore(
                remesa,
                comunidad,
                List.of(linea),
                List.of(vecino)
        );

        assertTrue(
                xml.contains(
                        "<InitgPty>\n"
                                + "        <Nm>Comunidad de Propietarios Test</Nm>"
                )
        );

        assertTrue(
                xml.contains(
                        "<OrgId>\n"
                                + "            <Othr>\n"
                                + "              <Id>H12345674</Id>"
                )
        );
    }

    @Test
    void limitaLaInformacionNoEstructuradaA140Caracteres() {

        RemesaLineaConceptoRepository conceptoRepository =
                mock(RemesaLineaConceptoRepository.class);

        SepaCoreXmlService service =
                new SepaCoreXmlService(conceptoRepository);

        FicheroGenerado remesa = crearRemesa(
                "REM-USTRD-140",
                LocalDate.of(2026, 10, 5),
                "Presentador de pruebas",
                "ES81ZZZB12345675",
                "B12345675"
        );

        Comunidad comunidad = crearComunidad();

        RemesaLinea linea = crearLinea(
                40L,
                196L,
                5000L,
                "12.00",
                "Concepto alternativo"
        );

        Vecino vecino = crearVecino(
                196L,
                "Pedro Hernández Pérez",
                "ES5321007772457367365553",
                "MANDATO-PEDRO-2024"
        );

        RemesaLineaConcepto conceptoLargo =
                crearConcepto("A".repeat(200));

        when(
                conceptoRepository.findByRemesaLineaIdOrderByOrdenAsc(40L)
        ).thenReturn(List.of(conceptoLargo));

        String xml = service.generarXmlCore(
                remesa,
                comunidad,
                List.of(linea),
                List.of(vecino)
        );

        String contenidoUstrd = extraerContenido(
                xml,
                "<Ustrd>",
                "</Ustrd>"
        );

        assertEquals(
                140,
                contenidoUstrd.length()
        );

        assertEquals(
                "A".repeat(140),
                contenidoUstrd
        );
    }

    private FicheroGenerado crearRemesa(
            String identificador,
            LocalDate fechaCobro,
            String presentadorAlias,
            String presentadorIdentificador,
            String presentadorNifCif
    ) {
        FicheroGenerado remesa = mock(FicheroGenerado.class);

        when(remesa.getIdentificadorFichero()).thenReturn(identificador);
        when(remesa.getFechaCobro()).thenReturn(fechaCobro);
        when(remesa.getPresentadorAlias()).thenReturn(presentadorAlias);
        when(remesa.getPresentadorIdentificador())
                .thenReturn(presentadorIdentificador);
        when(remesa.getPresentadorNifCif()).thenReturn(presentadorNifCif);

        return remesa;
    }

    private Comunidad crearComunidad() {
        Comunidad comunidad = mock(Comunidad.class);

        when(comunidad.getId()).thenReturn(18L);
        when(comunidad.getNombre())
                .thenReturn("Comunidad de Propietarios Test");
        when(comunidad.getIban())
                .thenReturn("ES39 3190 8991 9126 5275 3236");
        when(comunidad.getIdentificadorAcreedor())
                .thenReturn("H12345674");

        return comunidad;
    }

    private RemesaLinea crearLinea(
            Long id,
            Long vecinoId,
            Long reciboContableId,
            String importe,
            String concepto
    ) {
        RemesaLinea linea = mock(RemesaLinea.class);

        when(linea.getId()).thenReturn(id);
        when(linea.getVecinoId()).thenReturn(vecinoId);
        when(linea.getReciboContableId()).thenReturn(reciboContableId);
        when(linea.getImporte()).thenReturn(new BigDecimal(importe));
        when(linea.getConcepto()).thenReturn(concepto);

        return linea;
    }

    private Vecino crearVecino(
            Long id,
            String nombre,
            String iban,
            String referenciaMandato
    ) {
        Vecino vecino = mock(Vecino.class);

        when(vecino.getId()).thenReturn(id);
        when(vecino.getNombre()).thenReturn(nombre);
        when(vecino.getIban()).thenReturn(iban);
        when(vecino.getReferenciaMandato())
                .thenReturn(referenciaMandato);
        when(vecino.getFechaMandato())
                .thenReturn(LocalDate.of(2024, 1, 1));

        return vecino;
    }

    private RemesaLineaConcepto crearConcepto(String descripcion) {
        RemesaLineaConcepto concepto = mock(RemesaLineaConcepto.class);
        when(concepto.getDescripcion()).thenReturn(descripcion);
        return concepto;
    }

    private int contarApariciones(String texto, String fragmento) {
        int contador = 0;
        int posicion = 0;

        while ((posicion = texto.indexOf(fragmento, posicion)) >= 0) {
            contador++;
            posicion += fragmento.length();
        }

        return contador;
    }

    private String extraerContenido(
            String texto,
            String etiquetaInicial,
            String etiquetaFinal
    ) {
        int inicio = texto.indexOf(etiquetaInicial);

        if (inicio < 0) {
            throw new AssertionError(
                    "No se encontró la etiqueta inicial " + etiquetaInicial
            );
        }

        inicio += etiquetaInicial.length();

        int fin = texto.indexOf(etiquetaFinal, inicio);

        if (fin < 0) {
            throw new AssertionError(
                    "No se encontró la etiqueta final " + etiquetaFinal
            );
        }

        return texto.substring(inicio, fin);
    }
}