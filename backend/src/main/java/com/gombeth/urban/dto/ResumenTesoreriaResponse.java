package com.gombeth.urban.dto;

import java.math.BigDecimal;

public record ResumenTesoreriaResponse(
        Long comunidadId,
        String nombreComunidad,
        long recibosPendientes,
        BigDecimal importePendiente,
        long movimientosBanco,
        long movimientosSinConciliar,
        BigDecimal importeSinConciliar
) {
}