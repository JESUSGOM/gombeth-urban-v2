package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RemesaResumenResponse(
        Long id,
        Long comunidadId,
        String identificadorFichero,
        LocalDate fechaCreacion,
        BigDecimal totalImporte,
        Integer numeroRecibos,
        String nombreArchivo,
        String estado,
        String tipoRemesa,
        LocalDate fechaCobro,
        String esquemaSepa,
        BigDecimal totalDomiciliado,
        BigDecimal totalNoDomiciliado,
        String observaciones
) {
}