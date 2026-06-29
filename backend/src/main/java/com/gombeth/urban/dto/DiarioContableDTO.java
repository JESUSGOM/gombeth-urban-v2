package com.gombeth.urban.dto;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadMovimiento;

import java.math.BigDecimal;
import java.util.List;

public class DiarioContableDTO {

    private ContabilidadAsiento asiento;
    private List<ContabilidadMovimiento> movimientos;
    private BigDecimal totalDebe;
    private BigDecimal totalHaber;
    private boolean cuadrado;

    public DiarioContableDTO(
            ContabilidadAsiento asiento,
            List<ContabilidadMovimiento> movimientos,
            BigDecimal totalDebe,
            BigDecimal totalHaber
    ) {
        this.asiento = asiento;
        this.movimientos = movimientos;
        this.totalDebe = totalDebe;
        this.totalHaber = totalHaber;
        this.cuadrado = totalDebe.compareTo(totalHaber) == 0;
    }

    public ContabilidadAsiento getAsiento() {
        return asiento;
    }

    public List<ContabilidadMovimiento> getMovimientos() {
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
}