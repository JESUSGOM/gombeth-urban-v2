package com.gombeth.urban.dto;

import java.time.LocalDate;

public record ActaResumenResponse(
        Long id,
        Long comunidadId,
        String titulo,
        LocalDate fechaReunion,
        String estado
) {
}