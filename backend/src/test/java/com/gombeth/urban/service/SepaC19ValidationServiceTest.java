package com.gombeth.urban.service;

import com.gombeth.urban.dto.SepaValidacionResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SepaC19ValidationServiceTest {

    private SepaC19ValidationService service;

    @BeforeEach
    void configurar() {
        service = new SepaC19ValidationService();
    }

    @Test
    void aceptaFicheroC19EstructuralmenteValido() {
        SepaValidacionResultado resultado = service.validar(crearFicheroValido());

        assertTrue(
                resultado.isValida(),
                () -> String.join(" | ", resultado.getErrores())
        );
    }

    @Test
    void rechazaFicheroVacio() {
        SepaValidacionResultado resultado = service.validar("");
        assertContieneError(resultado, "vacío");
    }

    @Test
    void rechazaRegistroConLongitudDistintaDeSeiscientos() {
        List<String> registros = new ArrayList<>(crearRegistrosValidos());
        registros.set(2, registros.get(2).substring(0, 599));

        SepaValidacionResultado resultado = service.validar(String.join("\n", registros));
        assertContieneError(resultado, "600");
    }

    @Test
    void rechazaOrdenDeRegistrosIncorrecto() {
        List<String> registros = new ArrayList<>(crearRegistrosValidos());
        String temporal = registros.get(0);
        registros.set(0, registros.get(1));
        registros.set(1, temporal);

        SepaValidacionResultado resultado = service.validar(String.join("\n", registros));
        assertContieneError(resultado, "registro 01");
    }

    @Test
    void rechazaTotalDeImporteIncorrecto() {
        List<String> registros = new ArrayList<>(crearRegistrosValidos());
        registros.set(5, reemplazar(registros.get(5), 2, 19, "00000000000003000"));

        SepaValidacionResultado resultado = service.validar(String.join("\n", registros));
        assertContieneError(resultado, "importe total");
    }

    @Test
    void rechazaContadorDeAdeudosIncorrecto() {
        List<String> registros = new ArrayList<>(crearRegistrosValidos());
        registros.set(5, reemplazar(registros.get(5), 19, 27, "00000002"));

        SepaValidacionResultado resultado = service.validar(String.join("\n", registros));
        assertContieneError(resultado, "número de adeudos");
    }

    @Test
    void rechazaContadorFinalDeRegistrosIncorrecto() {
        List<String> registros = new ArrayList<>(crearRegistrosValidos());
        registros.set(5, reemplazar(registros.get(5), 27, 37, "0000000005"));

        SepaValidacionResultado resultado = service.validar(String.join("\n", registros));
        assertContieneError(resultado, "número total de registros");
    }

    @Test
    void rechazaFicheroSinAdeudos() {
        SepaValidacionResultado resultado = service.validar(
                String.join("\n", crearRegistrosSinAdeudos())
        );

        assertFalse(resultado.isValida());
        assertContieneError(resultado, "no contiene adeudos");
    }

    private String crearFicheroValido() {
        return String.join("\n", crearRegistrosValidos()) + "\n";
    }

    private List<String> crearRegistrosValidos() {
        String registro01 = completarRegistro("01");
        String registro02 = completarRegistro("02");

        String registro03 = completarRegistro(
                reemplazar(
                        reemplazar(completarRegistro("0319154003"), 88, 99, "00000002550"),
                        99,
                        107,
                        "20240115"
                )
        );

        String registro04 = completarRegistro(
                reemplazar(
                        reemplazar(
                                reemplazar(completarRegistro("04"), 45, 62, "00000000000002550"),
                                62,
                                70,
                                "00000001"
                        ),
                        70,
                        80,
                        "0000000003"
                )
        );

        String registro05 = completarRegistro(
                reemplazar(
                        reemplazar(
                                reemplazar(completarRegistro("05"), 37, 54, "00000000000002550"),
                                54,
                                62,
                                "00000001"
                        ),
                        62,
                        72,
                        "0000000004"
                )
        );

        String registro99 = completarRegistro(
                reemplazar(
                        reemplazar(
                                reemplazar(completarRegistro("99"), 2, 19, "00000000000002550"),
                                19,
                                27,
                                "00000001"
                        ),
                        27,
                        37,
                        "0000000006"
                )
        );

        return List.of(registro01, registro02, registro03, registro04, registro05, registro99);
    }

    private List<String> crearRegistrosSinAdeudos() {
        String registro01 = completarRegistro("01");
        String registro02 = completarRegistro("02");

        String registro04 = completarRegistro(
                reemplazar(
                        reemplazar(
                                reemplazar(completarRegistro("04"), 45, 62, "00000000000000000"),
                                62,
                                70,
                                "00000000"
                        ),
                        70,
                        80,
                        "0000000002"
                )
        );

        String registro05 = completarRegistro(
                reemplazar(
                        reemplazar(
                                reemplazar(completarRegistro("05"), 37, 54, "00000000000000000"),
                                54,
                                62,
                                "00000000"
                        ),
                        62,
                        72,
                        "0000000003"
                )
        );

        String registro99 = completarRegistro(
                reemplazar(
                        reemplazar(
                                reemplazar(completarRegistro("99"), 2, 19, "00000000000000000"),
                                19,
                                27,
                                "00000000"
                        ),
                        27,
                        37,
                        "0000000005"
                )
        );

        return List.of(registro01, registro02, registro04, registro05, registro99);
    }

    private String completarRegistro(String contenido) {
        StringBuilder resultado = new StringBuilder(contenido);
        while (resultado.length() < 600) {
            resultado.append(' ');
        }
        return resultado.substring(0, 600);
    }

    private String reemplazar(String texto, int inicio, int fin, String valor) {
        return texto.substring(0, inicio) + valor + texto.substring(fin);
    }

    private void assertContieneError(
            SepaValidacionResultado resultado,
            String fragmento
    ) {
        assertTrue(
                resultado.getErrores()
                        .stream()
                        .anyMatch(error ->
                                error.toLowerCase().contains(fragmento.toLowerCase())
                        ),
                () -> "No se encontró el texto '"
                        + fragmento
                        + "' en los errores: "
                        + resultado.getErrores()
        );
    }
}