package com.gombeth.urban.service;

import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Norma43ServiceTest {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyMMdd");

    @Mock
    private MovimientoBancarioRepository movimientoBancarioRepository;

    @InjectMocks
    private Norma43Service norma43Service;

    @Test
    void previsualizarProcesaRegistro22SinGuardarEnBaseDeDatos() {
        String registro22 = crearRegistro22(
                LocalDate.of(2026, 5, 19),
                LocalDate.of(2026, 5, 20),
                "2",
                new BigDecimal("120.50"),
                "CUOTA COMUNIDAD MAYO",
                "DOC0000001"
        );

        String contenido =
                registro22
                        + System.lineSeparator()
                        + crearRegistro33();

        List<MovimientoBancario> resultado =
                norma43Service.previsualizarContenido(
                        18L,
                        contenido
                );

        assertEquals(1, resultado.size());

        MovimientoBancario movimiento = resultado.get(0);

        assertEquals(18L, movimiento.getComunidadId());
        assertEquals(LocalDate.of(2026, 5, 19), movimiento.getFechaOperacion());
        assertEquals(LocalDate.of(2026, 5, 20), movimiento.getFechaValor());
        assertEquals("2", movimiento.getSigno());
        assertEquals(new BigDecimal("120.50"), movimiento.getImporte());
        assertEquals("CUOTA COMUNIDAD MAYO", movimiento.getConcepto());
        assertEquals("CUOTA COMUNIDAD MAYO", movimiento.getConceptoCompleto());
        assertEquals("DOC0000001", movimiento.getDocumentoExtra());
        assertFalse(Boolean.TRUE.equals(movimiento.getProcesado()));
        assertFalse(Boolean.TRUE.equals(movimiento.getConciliado()));

        verify(movimientoBancarioRepository, never())
                .saveAll(any());
    }

    @Test
    void previsualizarConcatenaLosRegistros23ConElMovimientoAnterior() {
        String contenido = String.join(
                System.lineSeparator(),
                crearRegistro22(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 1),
                        "2",
                        new BigDecimal("50.00"),
                        "INGRESO RECIBO",
                        "DOC0000002"
                ),
                crearRegistro23(
                        "ANTONIO MARTIN VECINO PISO 1A"
                ),
                crearRegistro23(
                        "REFERENCIA ADICIONAL 2026-06"
                ),
                crearRegistro33()
        );

        List<MovimientoBancario> resultado =
                norma43Service.previsualizarContenido(
                        18L,
                        contenido
                );

        assertEquals(1, resultado.size());

        assertEquals(
                "INGRESO RECIBO ANTONIO MARTIN VECINO PISO 1A "
                        + "REFERENCIA ADICIONAL 2026-06",
                resultado.get(0).getConceptoCompleto()
        );
    }

    @Test
    void registro33FinalizaElUltimoMovimientoDeCadaCuenta() {
        String contenido = String.join(
                System.lineSeparator(),
                crearRegistro22(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 1),
                        "2",
                        new BigDecimal("25.00"),
                        "PRIMER MOVIMIENTO",
                        "DOC0000003"
                ),
                crearRegistro33(),
                crearRegistro22(
                        LocalDate.of(2026, 7, 2),
                        LocalDate.of(2026, 7, 2),
                        "1",
                        new BigDecimal("10.00"),
                        "SEGUNDO MOVIMIENTO",
                        "DOC0000004"
                ),
                crearRegistro33()
        );

        List<MovimientoBancario> resultado =
                norma43Service.previsualizarContenido(
                        18L,
                        contenido
                );

        assertEquals(2, resultado.size());
        assertEquals("PRIMER MOVIMIENTO", resultado.get(0).getConcepto());
        assertEquals("SEGUNDO MOVIMIENTO", resultado.get(1).getConcepto());
        assertEquals("1", resultado.get(1).getSigno());
    }

    @Test
    void fechaInvalidaNoSeSustituyePorLaFechaActual() {
        String registro22Valido = crearRegistro22(
                LocalDate.of(2026, 5, 19),
                LocalDate.of(2026, 5, 20),
                "2",
                new BigDecimal("120.50"),
                "MOVIMIENTO",
                "DOC0000005"
        );

        String registro22Invalido =
                reemplazarRango(
                        registro22Valido,
                        10,
                        16,
                        "261332"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> norma43Service.previsualizarContenido(
                                18L,
                                registro22Invalido
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "fecha de operación"
                )
        );

        verify(movimientoBancarioRepository, never())
                .saveAll(any());
    }

    @Test
    void importarGuardaSoloLosMovimientosQueNoExisten() {
        String contenido = String.join(
                System.lineSeparator(),
                crearRegistro22(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 1),
                        "2",
                        new BigDecimal("75.00"),
                        "MOVIMIENTO NUEVO",
                        "DOC0000006"
                ),
                crearRegistro33()
        );

        when(
                movimientoBancarioRepository
                        .existsByComunidadIdAndFechaOperacionAndFechaValorAndImporteAndSignoAndReferenciaBancaria(
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(false);

        when(movimientoBancarioRepository.saveAll(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        List<MovimientoBancario> resultado =
                norma43Service.importarContenido(
                        18L,
                        contenido
                );

        assertEquals(1, resultado.size());

        verify(movimientoBancarioRepository)
                .saveAll(any());
    }

    private String crearRegistro22(
            LocalDate fechaOperacion,
            LocalDate fechaValor,
            String signo,
            BigDecimal importe,
            String concepto,
            String documento
    ) {
        char[] registro = " ".repeat(80).toCharArray();

        escribir(registro, 0, "22");
        escribir(registro, 2, "2100");
        escribir(registro, 6, "0001");
        escribir(registro, 10, fechaOperacion.format(FORMATO_FECHA));
        escribir(registro, 16, fechaValor.format(FORMATO_FECHA));
        escribir(registro, 22, "01");
        escribir(registro, 24, "001");
        escribir(registro, 27, signo);

        long centimos = importe
                .movePointRight(2)
                .longValueExact();

        escribir(
                registro,
                28,
                String.format("%014d", centimos)
        );

        escribir(
                registro,
                42,
                ajustarLongitud(documento, 10)
        );

        escribir(
                registro,
                52,
                ajustarLongitud(concepto, 28)
        );

        return new String(registro);
    }

    private String crearRegistro23(String concepto) {
        return "2301" + ajustarLongitud(concepto, 76);
    }

    private String crearRegistro33() {
        return "33" + " ".repeat(78);
    }

    private void escribir(
            char[] destino,
            int posicion,
            String valor
    ) {
        for (
                int indice = 0;
                indice < valor.length();
                indice++
        ) {
            destino[posicion + indice] = valor.charAt(indice);
        }
    }

    private String ajustarLongitud(
            String valor,
            int longitud
    ) {
        String seguro = valor == null ? "" : valor;

        if (seguro.length() > longitud) {
            return seguro.substring(0, longitud);
        }

        return seguro + " ".repeat(longitud - seguro.length());
    }

    private String reemplazarRango(
            String origen,
            int desde,
            int hasta,
            String reemplazo
    ) {
        return origen.substring(0, desde)
                + reemplazo
                + origen.substring(hasta);
    }
}