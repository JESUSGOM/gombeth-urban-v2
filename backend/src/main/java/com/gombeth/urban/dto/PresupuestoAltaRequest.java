package com.gombeth.urban.dto;

import java.math.BigDecimal;

public record PresupuestoAltaRequest(
        Long cuentaId,
        Integer anio,
        BigDecimal importe
) {
}