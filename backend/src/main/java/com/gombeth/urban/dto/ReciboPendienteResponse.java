package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReciboPendienteResponse(
        Long id,
        Long vecinoId,
        String nombreVecino,
        LocalDate fechaEmision,
        String periodo,
        String concepto,
        BigDecimal importe,
        String estado
) {
}