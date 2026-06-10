package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CuotaPresupuestoResponse(
        Long id,
        Long comunidadId,
        Long vecinoId,
        String nombre,
        String vivienda,
        Integer anio,
        Integer mesInicio,
        Integer mesFin,
        Integer version,
        String motivoRevision,
        String descripcion,
        BigDecimal coeficiente,
        BigDecimal importeAnual,
        BigDecimal importeMensual,
        String estado,
        LocalDateTime fechaGeneracion
) {
}