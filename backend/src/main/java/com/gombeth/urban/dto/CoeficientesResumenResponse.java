package com.gombeth.urban.dto;

import java.math.BigDecimal;

public class CoeficientesResumenResponse {

    private Long comunidadId;
    private BigDecimal totalCoeficiente;
    private boolean correcto;
    private long numeroPropietarios;
    private String mensaje;

    public CoeficientesResumenResponse() {
    }

    public CoeficientesResumenResponse(
            Long comunidadId,
            BigDecimal totalCoeficiente,
            boolean correcto,
            long numeroPropietarios,
            String mensaje
    ) {
        this.comunidadId = comunidadId;
        this.totalCoeficiente = totalCoeficiente;
        this.correcto = correcto;
        this.numeroPropietarios = numeroPropietarios;
        this.mensaje = mensaje;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public BigDecimal getTotalCoeficiente() {
        return totalCoeficiente;
    }

    public void setTotalCoeficiente(BigDecimal totalCoeficiente) {
        this.totalCoeficiente = totalCoeficiente;
    }

    public boolean isCorrecto() {
        return correcto;
    }

    public void setCorrecto(boolean correcto) {
        this.correcto = correcto;
    }

    public long getNumeroPropietarios() {
        return numeroPropietarios;
    }

    public void setNumeroPropietarios(long numeroPropietarios) {
        this.numeroPropietarios = numeroPropietarios;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}