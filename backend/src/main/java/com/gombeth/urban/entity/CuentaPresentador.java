package com.gombeth.urban.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuentas_presentador")
public class CuentaPresentador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "administrador_id",
            nullable = false
    )
    private Long administradorId;

    @Column(
            name = "alias",
            nullable = false,
            length = 100
    )
    private String alias;

    @Column(
            name = "banco",
            length = 100
    )
    private String banco;

    @Column(
            name = "identificador_presentador",
            nullable = false,
            length = 35
    )
    private String identificadorPresentador;

    @Column(
            name = "nif_cif",
            length = 20
    )
    private String nifCif;

    @Column(
            name = "sufijo",
            length = 3
    )
    private String sufijo;

    @Column(
            name = "iban",
            length = 34
    )
    private String iban;

    @Column(
            name = "bic",
            length = 11
    )
    private String bic;

    @Column(
            name = "activa",
            nullable = false
    )
    private boolean activa = true;

    @Lob
    @Column(
            name = "observaciones",
            columnDefinition = "TEXT"
    )
    private String observaciones;

    public CuentaPresentador() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Long getAdministradorId() {
        return administradorId;
    }

    public void setAdministradorId(
            Long administradorId
    ) {
        this.administradorId = administradorId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(
            String alias
    ) {
        this.alias = alias;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(
            String banco
    ) {
        this.banco = banco;
    }

    public String getIdentificadorPresentador() {
        return identificadorPresentador;
    }

    public void setIdentificadorPresentador(
            String identificadorPresentador
    ) {
        this.identificadorPresentador =
                identificadorPresentador;
    }

    public String getNifCif() {
        return nifCif;
    }

    public void setNifCif(
            String nifCif
    ) {
        this.nifCif = nifCif;
    }

    public String getSufijo() {
        return sufijo;
    }

    public void setSufijo(
            String sufijo
    ) {
        this.sufijo = sufijo;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(
            String iban
    ) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(
            String bic
    ) {
        this.bic = bic;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(
            boolean activa
    ) {
        this.activa = activa;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(
            String observaciones
    ) {
        this.observaciones = observaciones;
    }
}