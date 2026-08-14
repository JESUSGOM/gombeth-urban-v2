package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.util.List;

public record PresupuestoResponse(
        Long id,
        Long cuentaId,
        String cuentaCodigo,
        String cuentaDescripcion,
        Integer anio,
        BigDecimal importe,
        String metodoReparto,
        Boolean aplicaTodos,
        List<Long> vecinoIds,
        Integer numeroPropietariosAfectados
) {
}
