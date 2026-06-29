package com.gombeth.urban.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contabilidad_gastos")
public class ContabilidadGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String concepto;

    @Column(name = "fecha_factura")
    private LocalDate fechaFactura;

    @Column(name = "importe_total", precision = 19, scale = 2)
    private BigDecimal importeTotal;

    @Column(name = "numero_factura", length = 255)
    private String numeroFactura;

    @Column(length = 255)
    private String proveedor;

    @Column(name = "comunidad_id", nullable = false)
    private Long comunidadId;

    @Column(name = "cuenta_gasto_id")
    private Long cuentaGastoId;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    private Boolean pagado = false;

    @Column(name = "numero_asiento", length = 255)
    private String numeroAsiento;

    @Column(name = "ruta_pdf", length = 500)
    private String rutaPdf;

    public Long getId() {
        return id;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public LocalDate getFechaFactura() {
        return fechaFactura;
    }

    public void setFechaFactura(LocalDate fechaFactura) {
        this.fechaFactura = fechaFactura;
    }

    public BigDecimal getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public Long getCuentaGastoId() {
        return cuentaGastoId;
    }

    public void setCuentaGastoId(Long cuentaGastoId) {
        this.cuentaGastoId = cuentaGastoId;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Boolean getPagado() {
        return pagado;
    }

    public void setPagado(Boolean pagado) {
        this.pagado = pagado;
    }

    public String getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(String numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }

    public String getRutaPdf() {
        return rutaPdf;
    }

    public void setRutaPdf(String rutaPdf) {
        this.rutaPdf = rutaPdf;
    }
}