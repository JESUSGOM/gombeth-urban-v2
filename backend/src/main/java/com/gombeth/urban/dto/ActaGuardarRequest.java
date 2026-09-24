package com.gombeth.urban.dto;

import java.time.LocalDate;

public record ActaGuardarRequest(
        Long comunidadId,
        String titulo,
        LocalDate fechaReunion,
        String contenido
) {
}