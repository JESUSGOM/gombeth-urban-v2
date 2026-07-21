package com.gombeth.urban.dto.remesa;

import java.math.BigDecimal;

public record RemesaLineaConceptoDetalleResponse(
        Long id,
        String descripcion,
        BigDecimal importe,
        Integer orden,
        Boolean agrupadoEnUltimaLinea
) {}