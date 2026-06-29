package com.gombeth.urban.dto;

import java.math.BigDecimal;
import java.util.List;

public class LibroMayorDTO {

    private Long comunidadId;
    private Long cuentaId;
    private BigDecimal totalDebe;
    private BigDecimal totalHaber;
    private BigDecimal saldoFinal;
    private List<LibroMayorLineaDTO> lineas;

    public LibroMayorDTO(
            Long comunidadId,
            Long cuentaId,
            BigDecimal totalDebe,
            BigDecimal totalHaber,
            BigDecimal saldoFinal,
            List<LibroMayorLineaDTO> lineas
    ) {
        this.comunidadId = comunidadId;
        this.cuentaId = cuentaId;
        this.totalDebe = totalDebe;
        this.totalHaber = totalHaber;
        this.saldoFinal = saldoFinal;
        this.lineas = lineas;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public BigDecimal getTotalDebe() {
        return totalDebe;
    }

    public BigDecimal getTotalHaber() {
        return totalHaber;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public List<LibroMayorLineaDTO> getLineas() {
        return lineas;
    }
}