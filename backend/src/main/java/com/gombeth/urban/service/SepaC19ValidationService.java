package com.gombeth.urban.service;

import com.gombeth.urban.dto.SepaValidacionResultado;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Service
public class SepaC19ValidationService {

    private static final int LONGITUD_REGISTRO = 600;

    public SepaValidacionResultado validar(
            String contenido
    ) {
        SepaValidacionResultado resultado =
                new SepaValidacionResultado();

        if (
                contenido == null
                        || contenido.isBlank()
        ) {
            resultado.addError(
                    "El fichero C19 está vacío."
            );

            return resultado;
        }

        List<String> registros =
                Arrays.stream(
                                contenido.split("\\R")
                        )
                        .filter(linea ->
                                !linea.isEmpty()
                        )
                        .toList();

        if (registros.isEmpty()) {
            resultado.addError(
                    "El fichero C19 está vacío."
            );

            return resultado;
        }

        validarLongitudes(
                registros,
                resultado
        );

        validarOrdenBasico(
                registros,
                resultado
        );

        if (!resultado.isValida()) {
            return resultado;
        }

        validarEstructuraYTotales(
                registros,
                resultado
        );

        return resultado;
    }

    private void validarLongitudes(
            List<String> registros,
            SepaValidacionResultado resultado
    ) {
        for (int i = 0; i < registros.size(); i++) {
            String registro =
                    registros.get(i);

            if (
                    registro.length()
                            != LONGITUD_REGISTRO
            ) {
                resultado.addError(
                        "El registro "
                                + (i + 1)
                                + " no tiene 600 caracteres."
                );
            }
        }
    }

    private void validarOrdenBasico(
            List<String> registros,
            SepaValidacionResultado resultado
    ) {
        if (
                registros.size() < 5
        ) {
            resultado.addError(
                    "El fichero C19 no contiene la estructura mínima obligatoria."
            );

            return;
        }

        if (
                !registros.get(0)
                        .startsWith("01")
        ) {
            resultado.addError(
                    "El primer registro debe ser el registro 01."
            );
        }

        if (
                !registros.get(1)
                        .startsWith("02")
        ) {
            resultado.addError(
                    "El segundo registro debe ser el registro 02."
            );
        }

        int indice04 =
                buscarUnicoRegistro(
                        registros,
                        "04",
                        resultado
                );

        int indice05 =
                buscarUnicoRegistro(
                        registros,
                        "05",
                        resultado
                );

        int indice99 =
                buscarUnicoRegistro(
                        registros,
                        "99",
                        resultado
                );

        if (
                indice04 >= 0
                        && indice05 >= 0
                        && indice99 >= 0
        ) {
            if (
                    indice04 >= indice05
                            || indice05 >= indice99
            ) {
                resultado.addError(
                        "Los registros 04, 05 y 99 no están en el orden correcto."
                );
            }

            if (
                    indice99
                            != registros.size() - 1
            ) {
                resultado.addError(
                        "El último registro debe ser el registro 99."
                );
            }

            for (
                    int i = 2;
                    i < indice04;
                    i++
            ) {
                if (
                        !registros.get(i)
                                .startsWith("03")
                ) {
                    resultado.addError(
                            "Entre los registros 02 y 04 solo puede haber registros 03."
                    );

                    break;
                }
            }
        }
    }

    private int buscarUnicoRegistro(
            List<String> registros,
            String tipo,
            SepaValidacionResultado resultado
    ) {
        int indice =
                -1;

        int encontrados =
                0;

        for (int i = 0; i < registros.size(); i++) {
            if (
                    registros.get(i)
                            .startsWith(tipo)
            ) {
                encontrados++;
                indice = i;
            }
        }

        if (encontrados == 0) {
            resultado.addError(
                    "No existe el registro "
                            + tipo
                            + "."
            );

            return -1;
        }

        if (encontrados > 1) {
            resultado.addError(
                    "Existe más de un registro "
                            + tipo
                            + "."
            );

            return -1;
        }

        return indice;
    }

    private void validarEstructuraYTotales(
            List<String> registros,
            SepaValidacionResultado resultado
    ) {
        int indice04 =
                buscarIndice(
                        registros,
                        "04"
                );

        int indice05 =
                buscarIndice(
                        registros,
                        "05"
                );

        int indice99 =
                buscarIndice(
                        registros,
                        "99"
                );

        if (
                indice04 < 0
                        || indice05 < 0
                        || indice99 < 0
        ) {
            return;
        }

        BigInteger totalCalculado =
                BigInteger.ZERO;

        int numeroAdeudos =
                0;

        boolean adeudoAbierto =
                false;

        for (
                int i = 2;
                i < indice04;
                i++
        ) {
            String registro =
                    registros.get(i);

            if (
                    !registro.startsWith("03")
            ) {
                continue;
            }

            String numeroDato =
                    registro.substring(
                            7,
                            10
                    );

            if ("003".equals(numeroDato)) {
                numeroAdeudos++;
                adeudoAbierto = true;

                BigInteger importe =
                        leerNumero(
                                registro,
                                88,
                                99,
                                "importe del registro 03",
                                resultado
                        );

                totalCalculado =
                        totalCalculado.add(
                                importe
                        );

            } else if (
                    "004".equals(numeroDato)
                            || "005".equals(numeroDato)
                            || "006".equals(numeroDato)
                            || "007".equals(numeroDato)
            ) {
                if (!adeudoAbierto) {
                    resultado.addError(
                            "Existe un registro 03 adicional sin un adeudo 003 anterior."
                    );
                }

                BigInteger importeAdicional =
                        leerNumero(
                                registro,
                                88,
                                99,
                                "importe del registro 03 adicional",
                                resultado
                        );

                if (
                        importeAdicional.signum()
                                != 0
                ) {
                    resultado.addError(
                            "Los registros 03 adicionales no pueden incrementar el importe total."
                    );
                }

            } else {
                resultado.addError(
                        "El registro 03 contiene un número de dato no válido: "
                                + numeroDato
                                + "."
                );
            }
        }

        if (numeroAdeudos == 0) {
            resultado.addError(
                    "El fichero C19 no contiene adeudos."
            );
        }

        BigInteger total04 =
                leerNumero(
                        registros.get(indice04),
                        45,
                        62,
                        "importe total del registro 04",
                        resultado
                );

        int adeudos04 =
                leerEntero(
                        registros.get(indice04),
                        62,
                        70,
                        "número de adeudos del registro 04",
                        resultado
                );

        int registros04 =
                leerEntero(
                        registros.get(indice04),
                        70,
                        80,
                        "número de registros del registro 04",
                        resultado
                );

        BigInteger total05 =
                leerNumero(
                        registros.get(indice05),
                        37,
                        54,
                        "importe total del registro 05",
                        resultado
                );

        int adeudos05 =
                leerEntero(
                        registros.get(indice05),
                        54,
                        62,
                        "número de adeudos del registro 05",
                        resultado
                );

        int registros05 =
                leerEntero(
                        registros.get(indice05),
                        62,
                        72,
                        "número de registros del registro 05",
                        resultado
                );

        BigInteger total99 =
                leerNumero(
                        registros.get(indice99),
                        2,
                        19,
                        "importe total del registro 99",
                        resultado
                );

        int adeudos99 =
                leerEntero(
                        registros.get(indice99),
                        19,
                        27,
                        "número de adeudos del registro 99",
                        resultado
                );

        int registros99 =
                leerEntero(
                        registros.get(indice99),
                        27,
                        37,
                        "número total de registros del registro 99",
                        resultado
                );

        if (
                !totalCalculado.equals(total04)
                        || !totalCalculado.equals(total05)
                        || !totalCalculado.equals(total99)
        ) {
            resultado.addError(
                    "El importe total del fichero C19 no coincide con la suma de los adeudos."
            );
        }

        if (
                numeroAdeudos != adeudos04
                        || numeroAdeudos != adeudos05
                        || numeroAdeudos != adeudos99
        ) {
            resultado.addError(
                    "El número de adeudos del fichero C19 no coincide con los registros de control."
            );
        }

        int registrosEsperados04 =
                indice04;

        int registrosEsperados05 =
                indice05;

        int registrosEsperados99 =
                registros.size();

        if (
                registros04 != registrosEsperados04
        ) {
            resultado.addError(
                    "El contador de registros del registro 04 no coincide con el fichero."
            );
        }

        if (
                registros05 != registrosEsperados05
        ) {
            resultado.addError(
                    "El contador de registros del registro 05 no coincide con el fichero."
            );
        }

        if (
                registros99 != registrosEsperados99
        ) {
            resultado.addError(
                    "El número total de registros del registro 99 no coincide con el fichero."
            );
        }
    }

    private int buscarIndice(
            List<String> registros,
            String tipo
    ) {
        for (int i = 0; i < registros.size(); i++) {
            if (
                    registros.get(i)
                            .startsWith(tipo)
            ) {
                return i;
            }
        }

        return -1;
    }

    private BigInteger leerNumero(
            String registro,
            int inicio,
            int fin,
            String campo,
            SepaValidacionResultado resultado
    ) {
        String valor =
                registro.substring(
                                inicio,
                                fin
                        )
                        .trim();

        if (
                valor.isEmpty()
                        || !valor.matches("[0-9]+")
        ) {
            resultado.addError(
                    "El "
                            + campo
                            + " no es numérico."
            );

            return BigInteger.ZERO;
        }

        return new BigInteger(
                valor
        );
    }

    private int leerEntero(
            String registro,
            int inicio,
            int fin,
            String campo,
            SepaValidacionResultado resultado
    ) {
        BigInteger valor =
                leerNumero(
                        registro,
                        inicio,
                        fin,
                        campo,
                        resultado
                );

        try {
            return valor.intValueExact();

        } catch (ArithmeticException error) {
            resultado.addError(
                    "El "
                            + campo
                            + " excede el valor permitido."
            );

            return 0;
        }
    }
}