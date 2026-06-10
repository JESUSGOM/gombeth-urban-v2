package com.gombeth.urban.dto;

import java.math.BigDecimal;

public record PresupuestoResponse(
        Long id,
        Long cuentaId,
        String cuentaCodigo,
        String cuentaDescripcion,
        Integer anio,
        BigDecimal importe
) {
}