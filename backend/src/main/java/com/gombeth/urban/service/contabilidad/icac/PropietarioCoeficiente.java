package com.gombeth.urban.service.contabilidad.icac;

import java.math.BigDecimal;

public class PropietarioCoeficiente {

    private Long propietarioId;
    private BigDecimal coeficiente;

    public PropietarioCoeficiente(Long propietarioId, BigDecimal coeficiente) {
        this.propietarioId = propietarioId;
        this.coeficiente = coeficiente;
    }

    public Long getPropietarioId() {
        return propietarioId;
    }

    public BigDecimal getCoeficiente() {
        return coeficiente;
    }
}