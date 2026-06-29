package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LibroMayorLineaDTO {

    private Long movimientoId;
    private LocalDate fecha;
    private String concepto;
    private String numeroAsiento;
    private BigDecimal debe;
    private BigDecimal haber;
    private BigDecimal saldo;

    public LibroMayorLineaDTO(
            Long movimientoId,
            LocalDate fecha,
            String concepto,
            String numeroAsiento,
            BigDecimal debe,
            BigDecimal haber,
            BigDecimal saldo
    ) {
        this.movimientoId = movimientoId;
        this.fecha = fecha;
        this.concepto = concepto;
        this.numeroAsiento = numeroAsiento;
        this.debe = debe;
        this.haber = haber;
        this.saldo = saldo;
    }

    public Long getMovimientoId() {
        return movimientoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getConcepto() {
        return concepto;
    }

    public String getNumeroAsiento() {
        return numeroAsiento;
    }

    public BigDecimal getDebe() {
        return debe;
    }

    public BigDecimal getHaber() {
        return haber;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}