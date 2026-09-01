package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoGuardarRequest(
        Long comunidadId,
        String concepto,
        LocalDate fechaFactura,
        BigDecimal importeTotal,
        String numeroFactura,
        String proveedor,
        Long cuentaGastoId
) {
}