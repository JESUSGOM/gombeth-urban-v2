package com.gombeth.urban.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "comunidad_configuracion_reparto")
public class ComunidadConfiguracionReparto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comunidad_id", nullable = false, unique = true)
    private Long comunidadId;

    @Column(name = "metodo_reparto", nullable = false)
    private String metodoReparto;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public String getMetodoReparto() {
        return metodoReparto;
    }

    public void setMetodoReparto(String metodoReparto) {
        this.metodoReparto = metodoReparto;
    }
}