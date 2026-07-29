package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LibroMayorLineaDTO {

    private final Long movimientoId;

    private final LocalDate fecha;

    private final String concepto;

    private final String numeroAsiento;

    private final BigDecimal debe;

    private final BigDecimal haber;

    private final BigDecimal saldo;

    private final List<ContrapartidaDTO> contrapartidas;

    public LibroMayorLineaDTO(
            Long movimientoId,
            LocalDate fecha,
            String concepto,
            String numeroAsiento,
            BigDecimal debe,
            BigDecimal haber,
            BigDecimal saldo
    ) {
        this(
                movimientoId,
                fecha,
                concepto,
                numeroAsiento,
                debe,
                haber,
                saldo,
                List.of()
        );
    }

    public LibroMayorLineaDTO(
            Long movimientoId,
            LocalDate fecha,
            String concepto,
            String numeroAsiento,
            BigDecimal debe,
            BigDecimal haber,
            BigDecimal saldo,
            List<ContrapartidaDTO> contrapartidas
    ) {
        this.movimientoId = movimientoId;
        this.fecha = fecha;
        this.concepto = concepto;
        this.numeroAsiento = numeroAsiento;
        this.debe = debe;
        this.haber = haber;
        this.saldo = saldo;

        this.contrapartidas =
                contrapartidas != null
                        ? List.copyOf(contrapartidas)
                        : List.of();
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

    public List<ContrapartidaDTO> getContrapartidas() {
        return contrapartidas;
    }

    public static class ContrapartidaDTO {

        private final Long cuentaId;

        private final String codigoCuenta;

        private final String nombreCuenta;

        private final BigDecimal debe;

        private final BigDecimal haber;

        public ContrapartidaDTO(
                Long cuentaId,
                String codigoCuenta,
                String nombreCuenta,
                BigDecimal debe,
                BigDecimal haber
        ) {
            this.cuentaId = cuentaId;
            this.codigoCuenta = codigoCuenta;
            this.nombreCuenta = nombreCuenta;
            this.debe = debe;
            this.haber = haber;
        }

        public Long getCuentaId() {
            return cuentaId;
        }

        public String getCodigoCuenta() {
            return codigoCuenta;
        }

        public String getNombreCuenta() {
            return nombreCuenta;
        }

        public BigDecimal getDebe() {
            return debe;
        }

        public BigDecimal getHaber() {
            return haber;
        }
    }
}