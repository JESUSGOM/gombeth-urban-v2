package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PresupuestoRevisionResponse(
        Long id,
        Long comunidadId,
        Integer anio,
        Integer version,
        Integer mesInicio,
        Integer mesFin,
        BigDecimal importeRevision,
        String estado,
        String motivoRevision,
        LocalDateTime fechaGeneracion
) {
}