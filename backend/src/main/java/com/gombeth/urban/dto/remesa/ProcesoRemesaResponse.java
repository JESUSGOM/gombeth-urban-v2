package com.gombeth.urban.dto.remesa;

public class ProcesoRemesaResponse {

    private boolean correcto;

    private String mensaje;

    private Long remesaId;

    private int recibos;

    private String ficheroC19;

    private String ficheroXml;

    public boolean isCorrecto() {
        return correcto;
    }

    public void setCorrecto(boolean correcto) {
        this.correcto = correcto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Long getRemesaId() {
        return remesaId;
    }

    public void setRemesaId(Long remesaId) {
        this.remesaId = remesaId;
    }

    public int getRecibos() {
        return recibos;
    }

    public void setRecibos(int recibos) {
        this.recibos = recibos;
    }

    public String getFicheroC19() {
        return ficheroC19;
    }

    public void setFicheroC19(String ficheroC19) {
        this.ficheroC19 = ficheroC19;
    }

    public String getFicheroXml() {
        return ficheroXml;
    }

    public void setFicheroXml(String ficheroXml) {
        this.ficheroXml = ficheroXml;
    }
}