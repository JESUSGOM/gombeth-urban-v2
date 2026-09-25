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
        Long cuentaGastoId,
        Long proveedorComunidadId
) {

    /*
     * Compatibilidad con el flujo histórico.
     *
     * Mientras la asociación estructurada con proveedor
     * siga siendo opcional, permitimos construir el request
     * con los siete campos originales.
     */
    public GastoGuardarRequest(
            Long comunidadId,
            String concepto,
            LocalDate fechaFactura,
            BigDecimal importeTotal,
            String numeroFactura,
            String proveedor,
            Long cuentaGastoId
    ) {
        this(
                comunidadId,
                concepto,
                fechaFactura,
                importeTotal,
                numeroFactura,
                proveedor,
                cuentaGastoId,
                null
        );
    }
}