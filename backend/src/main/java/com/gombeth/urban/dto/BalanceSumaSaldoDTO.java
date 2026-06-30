package com.gombeth.urban.dto;

import java.math.BigDecimal;

public class BalanceSumaSaldoDTO {

    private Long cuentaId;
    private String codigoCuenta;
    private String nombreCuenta;

    private BigDecimal debe;
    private BigDecimal haber;
    private BigDecimal saldo;

    public BalanceSumaSaldoDTO(
            Long cuentaId,
            String codigoCuenta,
            String nombreCuenta,
            BigDecimal debe,
            BigDecimal haber,
            BigDecimal saldo
    ) {
        this.cuentaId = cuentaId;
        this.codigoCuenta = codigoCuenta;
        this.nombreCuenta = nombreCuenta;
        this.debe = debe;
        this.haber = haber;
        this.saldo = saldo;
    }

    public Long getCuentaId() { return cuentaId; }
    public String getCodigoCuenta() { return codigoCuenta; }
    public String getNombreCuenta() { return nombreCuenta; }
    public BigDecimal getDebe() { return debe; }
    public BigDecimal getHaber() { return haber; }
    public BigDecimal getSaldo() { return saldo; }
}