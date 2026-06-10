package com.gombeth.urban.dto;

public record GeneracionCuotasResponse(
        Long comunidadId,
        Integer anio,
        Integer cuotasGeneradas,
        String mensaje
) {
}