package com.gombeth.urban.dto.remesa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RemesaDetalleCompletoResponse(
        Long id,
        Long comunidadId,
        String comunidad,
        LocalDate fechaCreacion,
        LocalDate fechaCobro,
        String estado,
        String esquemaSepa,
        BigDecimal totalImporte,
        BigDecimal totalDomiciliado,
        BigDecimal totalNoDomiciliado,
        Integer numeroRecibos,
        String nombreArchivo,
        Boolean detalleReconstruido,
        String avisoDetalle,
        List<RemesaLineaDetalleResponse> lineas
) {
}