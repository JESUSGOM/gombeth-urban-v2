package com.gombeth.urban.dto;

import java.time.LocalDate;
import java.util.List;

public record GenerarRemesaSeleccionRequest(
        Long comunidadId,
        LocalDate fechaCobro,
        List<Long> reciboIds
) {
}