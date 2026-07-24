package com.gombeth.urban.dto.presentador;

public class CuentaPresentadorRequest {

    private String alias;

    private String banco;

    private String identificadorPresentador;

    private String nifCif;

    private String sufijo;

    private String iban;

    private String bic;

    private Boolean activa;

    private String observaciones;

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

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(
            Boolean activa
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