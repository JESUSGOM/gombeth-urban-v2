package com.gombeth.urban.dto;

import java.math.BigDecimal;

public class RepartoPresupuestoResponse {

    private Long vecinoId;
    private String nombre;
    private String vivienda;
    private BigDecimal coeficiente;
    private BigDecimal importeAnual;
    private BigDecimal importeMensual;

    public RepartoPresupuestoResponse(
            Long vecinoId,
            String nombre,
            String vivienda,
            BigDecimal coeficiente,
            BigDecimal importeAnual,
            BigDecimal importeMensual
    ) {
        this.vecinoId = vecinoId;
        this.nombre = nombre;
        this.vivienda = vivienda;
        this.coeficiente = coeficiente;
        this.importeAnual = importeAnual;
        this.importeMensual = importeMensual;
    }

    public Long getVecinoId() { return vecinoId; }
    public String getNombre() { return nombre; }
    public String getVivienda() { return vivienda; }
    public BigDecimal getCoeficiente() { return coeficiente; }
    public BigDecimal getImporteAnual() { return importeAnual; }
    public BigDecimal getImporteMensual() { return importeMensual; }
}