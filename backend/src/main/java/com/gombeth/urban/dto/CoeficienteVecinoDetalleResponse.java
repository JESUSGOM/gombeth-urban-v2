package com.gombeth.urban.dto;

import java.math.BigDecimal;

public class CoeficienteVecinoDetalleResponse {

    private Long vecinoId;
    private String nombre;
    private String vivienda;
    private BigDecimal coeficiente;
    private Boolean activo;

    public CoeficienteVecinoDetalleResponse(
            Long vecinoId,
            String nombre,
            String vivienda,
            BigDecimal coeficiente,
            Boolean activo
    ) {
        this.vecinoId = vecinoId;
        this.nombre = nombre;
        this.vivienda = vivienda;
        this.coeficiente = coeficiente;
        this.activo = activo;
    }

    public Long getVecinoId() {
        return vecinoId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getVivienda() {
        return vivienda;
    }

    public BigDecimal getCoeficiente() {
        return coeficiente;
    }

    public Boolean getActivo() {
        return activo;
    }
}