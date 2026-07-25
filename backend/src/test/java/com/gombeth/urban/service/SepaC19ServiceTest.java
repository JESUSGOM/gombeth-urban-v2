package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SepaC19ServiceTest {

    @Mock
    private RemesaLineaConceptoRepository
            remesaLineaConceptoRepository;

    private SepaC19Service service;

    @BeforeEach
    void configurar() {
        service =
                new SepaC19Service(
                        remesaLineaConceptoRepository
                );
    }

    @Test
    void generaRegistrosDeSeiscientosCaracteresEnOrdenCorrecto() {

        Contexto contexto =
                crearContextoValido(
                        new BigDecimal("25.50")
                );

        when(
                remesaLineaConceptoRepository
                        .findByRemesaLineaIdOrderByOrdenAsc(
                                10L
                        )
        ).thenReturn(
                List.of()
        );

        String c19 =
                generar(contexto);

        List<String> registros =
                registros(c19);

        assertEquals(
                6,
                registros.size()
        );

        assertEquals(
                List.of(
                        "01",
                        "02",
                        "03",
                        "04",
                        "05",
                        "99"
                ),
                registros.stream()
                        .map(registro ->
                                registro.substring(0, 2)
                        )
                        .toList()
        );

        assertTrue(
                registros.stream()
                        .allMatch(registro ->
                                registro.length() == 600
                        ),
                () -> "Longitudes encontradas: "
                        + registros.stream()
                        .map(String::length)
                        .toList()
        );
    }

    @Test
    void calculaImportesContadoresYTotalesDelFichero() {

        Contexto contexto =
                crearContextoValido(
                        new BigDecimal("25.50")
                );

        when(
                remesaLineaConceptoRepository
                        .findByRemesaLineaIdOrderByOrdenAsc(
                                10L
                        )
        ).thenReturn(
                List.of()
        );

        List<String> registros =
                registros(
                        generar(contexto)
                );

        String registro03 =
                registros.get(2);

        String registro04 =
                registros.get(3);

        String registro05 =
                registros.get(4);

        String registro99 =
                registros.get(5);

        assertEquals(
                "003",
                registro03.substring(7, 10)
        );

        assertEquals(
                "00000002550",
                registro03.substring(88, 99)
        );

        assertEquals(
                "00000000000002550",
                registro04.substring(45, 62)
        );

        assertEquals(
                "00000001",
                registro04.substring(62, 70)
        );

        assertEquals(
                "0000000003",
                registro04.substring(70, 80)
        );

        assertEquals(
                "00000000000002550",
                registro05.substring(37, 54)
        );

        assertEquals(
                "00000001",
                registro05.substring(54, 62)
        );

        assertEquals(
                "0000000004",
                registro05.substring(62, 72)
        );

        assertEquals(
                "00000000000002550",
                registro99.substring(2, 19)
        );

        assertEquals(
                "00000001",
                registro99.substring(19, 27)
        );

        assertEquals(
                "0000000006",
                registro99.substring(27, 37)
        );
    }

    @Test
    void usaLaFechaRealDelMandatoEnElRegistroDeAdeudo() {

        Contexto contexto =
                crearContextoValido(
                        new BigDecimal("25.50")
                );

        when(
                remesaLineaConceptoRepository
                        .findByRemesaLineaIdOrderByOrdenAsc(
                                10L
                        )
        ).thenReturn(
                List.of()
        );

        String registro03 =
                registros(
                        generar(contexto)
                ).get(2);

        assertEquals(
                "20240115",
                registro03.substring(99, 107)
        );
    }

    @Test
    void generaConceptosAdicionalesSinDuplicarImporteNiAdeudo() {

        Contexto contexto =
                crearContextoValido(
                        new BigDecimal("30.00")
                );

        RemesaLineaConcepto concepto1 =
                mock(RemesaLineaConcepto.class);

        RemesaLineaConcepto concepto2 =
                mock(RemesaLineaConcepto.class);

        when(
                concepto1.getDescripcion()
        ).thenReturn(
                "Cuota ordinaria"
        );

        when(
                concepto1.getImporte()
        ).thenReturn(
                new BigDecimal("20.00")
        );

        when(
                concepto2.getDescripcion()
        ).thenReturn(
                "Fondo de reserva"
        );

        when(
                concepto2.getImporte()
        ).thenReturn(
                new BigDecimal("10.00")
        );

        when(
                remesaLineaConceptoRepository
                        .findByRemesaLineaIdOrderByOrdenAsc(
                                10L
                        )
        ).thenReturn(
                List.of(
                        concepto1,
                        concepto2
                )
        );

        List<String> registros =
                registros(
                        generar(contexto)
                );

        assertEquals(
                7,
                registros.size()
        );

        String primer03 =
                registros.get(2);

        String segundo03 =
                registros.get(3);

        String registro99 =
                registros.get(6);

        assertEquals(
                "003",
                primer03.substring(7, 10)
        );

        assertEquals(
                "004",
                segundo03.substring(7, 10)
        );

        assertEquals(
                "00000003000",
                primer03.substring(88, 99)
        );

        assertEquals(
                "00000000000",
                segundo03.substring(88, 99)
        );

        assertEquals(
                "00000001",
                registro99.substring(19, 27)
        );

        assertEquals(
                "00000000000003000",
                registro99.substring(2, 19)
        );
    }

    @Test
    void excluyeLineasNoIncluidasEnSepa() {

        Contexto contexto =
                crearContextoValido(
                        new BigDecimal("25.50")
                );

        RemesaLinea excluida =
                mock(RemesaLinea.class);

        when(
                excluida.getIncluidoSepa()
        ).thenReturn(
                false
        );

        when(
                remesaLineaConceptoRepository
                        .findByRemesaLineaIdOrderByOrdenAsc(
                                10L
                        )
        ).thenReturn(
                List.of()
        );

        String c19 =
                service.generarC19(
                        contexto.remesa(),
                        contexto.comunidad(),
                        List.of(
                                contexto.linea(),
                                excluida
                        ),
                        List.of(
                                contexto.vecino()
                        )
                );

        List<String> registros =
                registros(c19);

        long registros03 =
                registros.stream()
                        .filter(registro ->
                                registro.startsWith("03")
                        )
                        .count();

        assertEquals(
                1,
                registros03
        );

        assertEquals(
                "00000001",
                registros.get(registros.size() - 1)
                        .substring(19, 27)
        );
    }

    private String generar(
            Contexto contexto
    ) {
        return service.generarC19(
                contexto.remesa(),
                contexto.comunidad(),
                List.of(
                        contexto.linea()
                ),
                List.of(
                        contexto.vecino()
                )
        );
    }

    private List<String> registros(
            String c19
    ) {
        return Arrays.stream(
                        c19.split("\\R")
                )
                .filter(linea ->
                        !linea.isEmpty()
                )
                .toList();
    }

    private Contexto crearContextoValido(
            BigDecimal importe
    ) {

        FicheroGenerado remesa =
                mock(FicheroGenerado.class);

        when(
                remesa.getFechaCobro()
        ).thenReturn(
                LocalDate.of(
                        2026,
                        10,
                        5
                )
        );

        when(
                remesa.getPresentadorIban()
        ).thenReturn(
                "ES9430589999100000000000"
        );

        when(
                remesa.getPresentadorIdentificador()
        ).thenReturn(
                "ES81ZZZB12345675"
        );

        when(
                remesa.getPresentadorAlias()
        ).thenReturn(
                "Presentador de prueba"
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
                "Calle de prueba 1"
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
                linea.getImporte()
        ).thenReturn(
                importe
        );

        when(
                linea.getReciboContableId()
        ).thenReturn(
                100L
        );

        lenient().when(
                linea.getConcepto()
        ).thenReturn(
                "Cuota ordinaria"
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
                "MANDATO-4-2024"
        );

        when(
                vecino.getFechaMandato()
        ).thenReturn(
                LocalDate.of(
                        2024,
                        1,
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
                "Calle del propietario 2"
        );

        when(
                vecino.getPoblacion()
        ).thenReturn(
                "La Laguna"
        );

        return new Contexto(
                remesa,
                comunidad,
                linea,
                vecino
        );
    }

    private record Contexto(
            FicheroGenerado remesa,
            Comunidad comunidad,
            RemesaLinea linea,
            Vecino vecino
    ) {
    }
}