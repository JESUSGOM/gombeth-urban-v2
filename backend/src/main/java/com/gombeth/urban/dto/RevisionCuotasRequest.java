package com.gombeth.urban.dto;

import java.math.BigDecimal;

public record RevisionCuotasRequest(
        Integer anio,
        Integer mesInicio,
        Integer mesFin,
        BigDecimal importeRevision,
        String motivoRevision
) {
}