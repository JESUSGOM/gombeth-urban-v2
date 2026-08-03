package com.gombeth.urban.entity;

import java.util.Locale;

public enum RemesaEstado {

    GENERADA,
    VALIDADA,
    FICHERO_GENERADO,
    PRESENTADA,
    ANULADA;

    public static RemesaEstado desde(
            String valor
    ) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "El estado de la remesa es obligatorio."
            );
        }

        try {
            return RemesaEstado.valueOf(
                    valor.trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException excepcion) {
            throw new IllegalArgumentException(
                    "Estado de remesa no reconocido: "
                            + valor,
                    excepcion
            );
        }
    }

    public boolean puedeCambiarA(
            RemesaEstado destino
    ) {
        if (destino == null || this == destino) {
            return false;
        }

        return switch (this) {
            case GENERADA ->
                    destino == VALIDADA
                            || destino == ANULADA;

            case VALIDADA ->
                    destino == FICHERO_GENERADO
                            || destino == ANULADA;

            case FICHERO_GENERADO ->
                    destino == PRESENTADA
                            || destino == ANULADA;

            case PRESENTADA, ANULADA -> false;
        };
    }
}