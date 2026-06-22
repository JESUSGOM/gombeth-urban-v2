package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoContextoResponse(
        Long movimientoId,
        Long comunidadId,
        String nombreComunidad,
        LocalDate fechaOperacion,
        LocalDate fechaValor,
        BigDecimal importe,
        String concepto
) {
}