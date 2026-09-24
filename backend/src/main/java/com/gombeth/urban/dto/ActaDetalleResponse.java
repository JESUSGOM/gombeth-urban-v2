package com.gombeth.urban.dto;

import java.time.LocalDate;

public record ActaDetalleResponse(
        Long id,
        Long comunidadId,
        String titulo,
        LocalDate fechaReunion,
        String contenido,
        String estado
) {
}