package com.gombeth.urban.dto;

import java.math.BigDecimal;

public record CandidatoConciliacionResponse(
        Long reciboId,
        Long vecinoId,
        String concepto,
        BigDecimal importe,
        String estado
) {
}