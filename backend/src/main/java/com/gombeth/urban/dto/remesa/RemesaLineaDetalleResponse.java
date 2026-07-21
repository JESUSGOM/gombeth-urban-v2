package com.gombeth.urban.dto.remesa;

import java.math.BigDecimal;
import java.util.List;

public record RemesaLineaDetalleResponse(
        Long id,
        Long vecinoId,
        String vecino,
        Long reciboContableId,
        BigDecimal importe,
        Boolean domiciliado,
        Boolean incluidoSepa,
        Boolean pdfGenerado,
        Boolean emailEnviado,
        List<RemesaLineaConceptoDetalleResponse> conceptos
) {}