package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.util.List;

public record PresupuestoAltaRequest(
        Long cuentaId,
        Integer anio,
        BigDecimal importe,
        String metodoReparto,
        Boolean aplicaTodos,
        List<Long> vecinoIds
) {
    /**
     * Constructor conservado para compatibilidad con llamadas y pruebas
     * anteriores a la configuracion de reparto por partida.
     */
    public PresupuestoAltaRequest(
            Long cuentaId,
            Integer anio,
            BigDecimal importe
    ) {
        this(
                cuentaId,
                anio,
                importe,
                null,
                null,
                List.of()
        );
    }
}
