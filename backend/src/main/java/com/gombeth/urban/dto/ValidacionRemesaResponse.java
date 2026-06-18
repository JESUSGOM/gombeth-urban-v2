package com.gombeth.urban.dto;

import java.util.List;

public record ValidacionRemesaResponse(
        Long remesaId,
        boolean valida,
        int errores,
        List<String> mensajes
) {
}