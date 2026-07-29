package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Norma43MovimientoPreviewResponse(
        LocalDate fechaOperacion,
        LocalDate fechaValor,
        String signo,
        String tipo,
        BigDecimal importe,
        String concepto,
        String conceptoCompleto,
        String referenciaBancaria,
        String documentoExtra
) {
}
