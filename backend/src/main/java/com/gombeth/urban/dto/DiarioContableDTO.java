package com.gombeth.urban.dto;

import com.gombeth.urban.entity.ContabilidadAsiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DiarioContableDTO {

    private final ContabilidadAsiento asiento;

    private final List<MovimientoDiarioDTO> movimientos;

    private final BigDecimal totalDebe;

    private final BigDecimal totalHaber;

    private final boolean cuadrado;

    public DiarioContableDTO(
            ContabilidadAsiento asiento,
            List<MovimientoDiarioDTO> movimientos,
            BigDecimal totalDebe,
            BigDecimal totalHaber
    ) {
        this.asiento = asiento;
        this.movimientos = movimientos;
        this.totalDebe = totalDebe;
        this.totalHaber = totalHaber;

        this.cuadrado =
                totalDebe != null
                        && totalHaber != null
                        && totalDebe.compareTo(totalHaber) == 0;
    }

    public ContabilidadAsiento getAsiento() {
        return asiento;
    }

    public List<MovimientoDiarioDTO> getMovimientos() {
        return movimientos;
    }

    public BigDecimal getTotalDebe() {
        return totalDebe;
    }

    public BigDecimal getTotalHaber() {
        return totalHaber;
    }

    public boolean isCuadrado() {
        return cuadrado;
    }

    public static class MovimientoDiarioDTO {

        private final Long id;

        private final String concepto;

        private final BigDecimal debe;

        private final BigDecimal haber;

        private final LocalDate fecha;

        private final String numeroAsiento;

        private final Long comunidadId;

        private final Long cuentaId;

        private final String codigoCuenta;

        private final String nombreCuenta;

        public MovimientoDiarioDTO(
                Long id,
                String concepto,
                BigDecimal debe,
                BigDecimal haber,
                LocalDate fecha,
                String numeroAsiento,
                Long comunidadId,
                Long cuentaId,
                String codigoCuenta,
                String nombreCuenta
        ) {
            this.id = id;
            this.concepto = concepto;
            this.debe = debe;
            this.haber = haber;
            this.fecha = fecha;
            this.numeroAsiento = numeroAsiento;
            this.comunidadId = comunidadId;
            this.cuentaId = cuentaId;
            this.codigoCuenta = codigoCuenta;
            this.nombreCuenta = nombreCuenta;
        }

        public Long getId() {
            return id;
        }

        public String getConcepto() {
            return concepto;
        }

        public BigDecimal getDebe() {
            return debe;
        }

        public BigDecimal getHaber() {
            return haber;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public String getNumeroAsiento() {
            return numeroAsiento;
        }

        public Long getComunidadId() {
            return comunidadId;
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
    }
}