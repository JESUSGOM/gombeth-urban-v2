package com.gombeth.urban.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "conceptos_cobro")
public class ConceptoCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(precision = 10, scale = 2)
    private BigDecimal importe;

    @Column(length = 50)
    private String periodicidad;

    @Column(name = "comunidad_id")
    private Long comunidadId;

    @Column(name = "vecino_id")
    private Long vecinoId;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "mes_inicio")
    private Integer mesInicio;

    @Column(name = "cuenta_contable_id")
    private Long cuentaContableId;

    @Column(name = "movimiento_bancario_id")
    private Long movimientoBancarioId;

    @Column(name = "tipo_impuesto", length = 50)
    private String tipoImpuesto;

    @Column(name = "porcentaje_impuesto", precision = 5, scale = 2)
    private BigDecimal porcentajeImpuesto;


    public ConceptoCobro() {
    }

    public Long getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public String getPeriodicidad() {
        return periodicidad;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public Long getVecinoId() {
        return vecinoId;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Integer getMesInicio() {
        return mesInicio;
    }

    public Long getCuentaContableId() {
        return cuentaContableId;
    }

    public Long getMovimientoBancarioId() {
        return movimientoBancarioId;
    }

    public String getTipoImpuesto() {
        return tipoImpuesto;
    }

    public BigDecimal getPorcentajeImpuesto() {
        return porcentajeImpuesto;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public void setPeriodicidad(String periodicidad) {
        this.periodicidad = periodicidad;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public void setVecinoId(Long vecinoId) {
        this.vecinoId = vecinoId;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setMesInicio(Integer mesInicio) {
        this.mesInicio = mesInicio;
    }

    public void setCuentaContableId(Long cuentaContableId) {
        this.cuentaContableId = cuentaContableId;
    }

    public void setMovimientoBancarioId(Long movimientoBancarioId) {
        this.movimientoBancarioId = movimientoBancarioId;
    }

    public void setTipoImpuesto(String tipoImpuesto) {
        this.tipoImpuesto = tipoImpuesto;
    }

    public void setPorcentajeImpuesto(BigDecimal porcentajeImpuesto) {
        this.porcentajeImpuesto = porcentajeImpuesto;
    }
}