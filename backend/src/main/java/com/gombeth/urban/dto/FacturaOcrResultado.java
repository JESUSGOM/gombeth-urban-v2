package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos propuestos al analizar una factura.
 *
 * El OCR nunca modifica directamente el gasto:
 * estos valores deben poder ser revisados y corregidos
 * por el usuario antes de guardarlos.
 */
public record FacturaOcrResultado(
        String proveedor,
        LocalDate fechaFactura,
        BigDecimal importeTotal,
        String numeroFactura,
        boolean ocrAplicado,
        List<String> advertencias
) {
}