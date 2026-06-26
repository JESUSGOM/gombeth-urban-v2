package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReciboResponse(
        Long id,
        LocalDate fechaEmision,
        Long vecinoId,
        String nombreVecino,
        String vivienda,
        String concepto,
        BigDecimal importe,
        String estado,
        String tipoRemesa,
        String etiquetaExtra
) {
}
