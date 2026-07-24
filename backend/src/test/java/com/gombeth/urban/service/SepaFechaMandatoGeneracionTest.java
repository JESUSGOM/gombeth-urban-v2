package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SepaFechaMandatoGeneracionTest {

    @Test
    void xmlUsaFechaRealDelMandato() {

        RemesaLineaConceptoRepository conceptoRepository =
                mock(RemesaLineaConceptoRepository.class);

        SepaCoreXmlService service =
                new SepaCoreXmlService(
                        conceptoRepository
                );

        FicheroGenerado remesa =
                mock(FicheroGenerado.class);

        when(
                remesa.getIdentificadorFichero()
        ).thenReturn(
                "REMESA-PRUEBA"
        );

        when(
                remesa.getFechaCobro()
        ).thenReturn(
                LocalDate.of(
                        2026,
                        8,
                        5
                )
        );

        Comunidad comunidad =
                mock(Comunidad.class);

        when(
                comunidad.getId()
        ).thenReturn(
                18L
        );

        when(
                comunidad.getNombre()
        ).thenReturn(
                "Comunidad de prueba"
        );

        when(
                comunidad.getIban()
        ).thenReturn(
                "ES9121000418450200051332"
        );

        when(
                comunidad.getIdentificadorAcreedor()
        ).thenReturn(
                "ES12ZZZ12345678"
        );

        RemesaLinea linea =
                mock(RemesaLinea.class);

        when(
                linea.getId()
        ).thenReturn(
                10L
        );

        when(
                linea.getVecinoId()
        ).thenReturn(
                4L
        );

        when(
                linea.getReciboContableId()
        ).thenReturn(
                900L
        );

        when(
                linea.getImporte()
        ).thenReturn(
                new BigDecimal("25.50")
        );

        when(
                linea.getConcepto()
        ).thenReturn(
                "Cuota ordinaria"
        );

        when(
                conceptoRepository
                        .findByRemesaLineaIdOrderByOrdenAsc(
                                10L
                        )
        ).thenReturn(
                List.of()
        );

        Vecino vecino =
                mock(Vecino.class);

        when(
                vecino.getId()
        ).thenReturn(
                4L
        );

        when(
                vecino.getReferenciaMandato()
        ).thenReturn(
                "MANDATO-4-2020"
        );

        lenient().when(
                vecino.getFechaMandato()
        ).thenReturn(
                LocalDate.of(
                        2020,
                        5,
                        15
                )
        );

        when(
                vecino.getNombre()
        ).thenReturn(
                "Propietario de prueba"
        );

        when(
                vecino.getIban()
        ).thenReturn(
                "ES7921000813610123456789"
        );

        String xml =
                service.generarXmlCore(
                        remesa,
                        comunidad,
                        List.of(linea),
                        List.of(vecino)
                );

        assertTrue(
                xml.contains(
                        "<DtOfSgntr>2020-05-15</DtOfSgntr>"
                ),
                () -> "El XML no contiene la fecha real "
                        + "del mandato: "
                        + xml
        );
    }

    @Test
    void c19UsaFechaRealDelMandato() {

        RemesaLineaConceptoRepository conceptoRepository =
                mock(RemesaLineaConceptoRepository.class);

        SepaC19Service service =
                new SepaC19Service(
                        conceptoRepository
                );

        FicheroGenerado remesa =
                mock(FicheroGenerado.class);

        when(
                remesa.getFechaCobro()
        ).thenReturn(
                LocalDate.of(
                        2026,
                        8,
                        5
                )
        );

        Comunidad comunidad =
                mock(Comunidad.class);

        when(
                comunidad.getIdentificadorAcreedor()
        ).thenReturn(
                "ES12ZZZ12345678"
        );

        when(
                comunidad.getIban()
        ).thenReturn(
                "ES9121000418450200051332"
        );

        when(
                comunidad.getNombre()
        ).thenReturn(
                "Comunidad de prueba"
        );

        when(
                comunidad.getDireccion()
        ).thenReturn(
                "Calle Comunidad 1"
        );

        when(
                comunidad.getPoblacion()
        ).thenReturn(
                "Santa Cruz de Tenerife"
        );

        RemesaLinea linea =
                mock(RemesaLinea.class);

        when(
                linea.getId()
        ).thenReturn(
                10L
        );

        when(
                linea.getIncluidoSepa()
        ).thenReturn(
                true
        );

        when(
                linea.getVecinoId()
        ).thenReturn(
                4L
        );

        when(
                linea.getReciboContableId()
        ).thenReturn(
                900L
        );

        when(
                linea.getImporte()
        ).thenReturn(
                new BigDecimal("25.50")
        );

        when(
                linea.getConcepto()
        ).thenReturn(
                "Cuota ordinaria"
        );

        when(
                conceptoRepository
                        .findByRemesaLineaIdOrderByOrdenAsc(
                                10L
                        )
        ).thenReturn(
                List.of()
        );

        Vecino vecino =
                mock(Vecino.class);

        when(
                vecino.getId()
        ).thenReturn(
                4L
        );

        when(
                vecino.isDomiciliado()
        ).thenReturn(
                true
        );

        when(
                vecino.getIban()
        ).thenReturn(
                "ES7921000813610123456789"
        );

        when(
                vecino.getReferenciaMandato()
        ).thenReturn(
                "MANDATO-4-2020"
        );

        lenient().when(
                vecino.getFechaMandato()
        ).thenReturn(
                LocalDate.of(
                        2020,
                        5,
                        15
                )
        );

        when(
                vecino.getBic()
        ).thenReturn(
                "CAIXESBBXXX"
        );

        when(
                vecino.getNombre()
        ).thenReturn(
                "Propietario de prueba"
        );

        when(
                vecino.getDireccion()
        ).thenReturn(
                "Calle Propietario 2"
        );

        when(
                vecino.getPoblacion()
        ).thenReturn(
                "Santa Cruz de Tenerife"
        );

        String contenidoC19 =
                service.generarC19(
                        remesa,
                        comunidad,
                        List.of(linea),
                        List.of(vecino)
                );

        String registro003 =
                Arrays.stream(
                                contenidoC19.split("\\R")
                        )
                        .filter(registro ->
                                registro.startsWith(
                                        "0319154003"
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new AssertionError(
                                        "No se encontró el registro 003."
                                )
                        );

        assertEquals(
                600,
                registro003.length(),
                "El registro 003 debe medir 600 caracteres."
        );

        assertEquals(
                "20200515",
                registro003.substring(
                        99,
                        107
                ),
                "El C19 debe utilizar la fecha real del mandato."
        );
    }
}
