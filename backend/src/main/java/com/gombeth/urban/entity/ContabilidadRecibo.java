package com.gombeth.urban.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contabilidad_recibos")
public class ContabilidadRecibo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_cobro_banco")
    private LocalDate fechaCobroBanco;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal importe;

    @Column(name = "comunidad_id", nullable = false)
    private Long comunidadId;

    @Column(name = "movimiento_bancario_id")
    private Long movimientoBancarioId;

    @Column(name = "vecino_id", nullable = false)
    private Long vecinoId;

    @Column(name = "cuota_presupuesto_id")
    private Long cuotaPresupuestoId;

    @Column(name = "pagado_acumulado", nullable = false, precision = 19, scale = 2)
    private BigDecimal pagadoAcumulado = BigDecimal.ZERO;

    @Column(length = 255)
    private String concepto;

    @Column(name = "tipo_remesa", length = 20)
    private String tipoRemesa = "ORDINARIA";

    @Column(name = "etiqueta_extra", length = 100)
    private String etiquetaExtra;

    public Long getId() {
        return id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaCobroBanco() {
        return fechaCobroBanco;
    }

    public void setFechaCobroBanco(LocalDate fechaCobroBanco) {
        this.fechaCobroBanco = fechaCobroBanco;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public Long getMovimientoBancarioId() {
        return movimientoBancarioId;
    }

    public void setMovimientoBancarioId(Long movimientoBancarioId) {
        this.movimientoBancarioId = movimientoBancarioId;
    }

    public Long getVecinoId() {
        return vecinoId;
    }

    public void setVecinoId(Long vecinoId) {
        this.vecinoId = vecinoId;
    }

    public Long getCuotaPresupuestoId() {
        return cuotaPresupuestoId;
    }

    public void setCuotaPresupuestoId(Long cuotaPresupuestoId) {
        this.cuotaPresupuestoId = cuotaPresupuestoId;
    }

    public BigDecimal getPagadoAcumulado() {
        return pagadoAcumulado;
    }

    public void setPagadoAcumulado(BigDecimal pagadoAcumulado) {
        this.pagadoAcumulado = pagadoAcumulado;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getTipoRemesa() {
        return tipoRemesa;
    }

    public void setTipoRemesa(String tipoRemesa) {
        this.tipoRemesa = tipoRemesa;
    }

    public String getEtiquetaExtra() {
        return etiquetaExtra;
    }

    public void setEtiquetaExtra(String etiquetaExtra) {
        this.etiquetaExtra = etiquetaExtra;
    }
}