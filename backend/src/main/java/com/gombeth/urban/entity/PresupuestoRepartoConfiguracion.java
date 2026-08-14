package com.gombeth.urban.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "presupuesto_reparto_configuracion",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_presupuesto_reparto_config_presupuesto",
                        columnNames = "presupuesto_id"
                )
        }
)
public class PresupuestoRepartoConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "presupuesto_id", nullable = false)
    private Long presupuestoId;

    @Column(name = "metodo_reparto", nullable = false, length = 30)
    private String metodoReparto;

    @Column(name = "aplica_todos", nullable = false)
    private Boolean aplicaTodos = Boolean.TRUE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPresupuestoId() {
        return presupuestoId;
    }

    public void setPresupuestoId(Long presupuestoId) {
        this.presupuestoId = presupuestoId;
    }

    public String getMetodoReparto() {
        return metodoReparto;
    }

    public void setMetodoReparto(String metodoReparto) {
        this.metodoReparto = metodoReparto;
    }

    public Boolean getAplicaTodos() {
        return aplicaTodos;
    }

    public void setAplicaTodos(Boolean aplicaTodos) {
        this.aplicaTodos = aplicaTodos;
    }
}
