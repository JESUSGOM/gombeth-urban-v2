package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Norma43PrevisualizacionResponse(
        Long comunidadId,
        String nombreFichero,
        int numeroMovimientos,
        BigDecimal totalDebe,
        BigDecimal totalHaber,
        LocalDate fechaInicial,
        LocalDate fechaFinal,
        List<Norma43MovimientoPreviewResponse> movimientos
) {
}
