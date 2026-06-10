package com.gombeth.urban.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "propiedades")
public class Propiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comunidad_id", nullable = false)
    private Long comunidadId;

    @Column(name = "vecino_id", nullable = false)
    private Long vecinoId;

    @Column(nullable = false, length = 50)
    private String referencia;

    @Column(length = 30)
    private String tipo;

    @Column(length = 200)
    private String direccion;

    @Column(precision = 10, scale = 4)
    private BigDecimal coeficiente;

    @Column(nullable = false)
    private Boolean activo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notas;

    public Long getId() {
        return id;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public Long getVecinoId() {
        return vecinoId;
    }

    public void setVecinoId(Long vecinoId) {
        this.vecinoId = vecinoId;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public BigDecimal getCoeficiente() {
        return coeficiente;
    }

    public void setCoeficiente(BigDecimal coeficiente) {
        this.coeficiente = coeficiente;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}