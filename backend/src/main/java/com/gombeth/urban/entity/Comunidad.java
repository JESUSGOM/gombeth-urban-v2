package com.gombeth.urban.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gombeth.urban.util.AesEncryptor;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "comunidades")
public class Comunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 70)
    private String nombre;

    @Column(length = 100)
    private String direccion;

    @Column(length = 50)
    private String poblacion;

    @Column(length = 40)
    private String provincia;

    @Column(length = 10)
    private String codigoPostal;

    @Column(length = 20)
    private String nifCif;

    @Convert(converter = AesEncryptor.class)
    @Column(name = "iban", length = 255)
    private String iban;

    @Column(length = 11)
    private String bic;

    @Column(length = 3)
    private String sufijo;

    @Column(name = "pais_cod", length = 2)
    private String paiscod = "ES";

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "administrador_id")
    private Long administradorId;

    @Convert(converter = AesEncryptor.class)
    @Column(name = "identificador_acreedor", length = 255)
    private String identificadorAcreedor;

    @Column(name = "tipo_reparto", length = 20)
    private String tipoReparto = "PARTES_IGUALES";

    @Column(name = "token_qr", length = 100, unique = true)
    private String tokenQr;

    public Comunidad() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getPoblacion() {
        return poblacion;
    }

    public void setPoblacion(String poblacion) {
        this.poblacion = poblacion;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getNifCif() {
        return nifCif;
    }

    public void setNifCif(String nifCif) {
        this.nifCif = nifCif;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public String getSufijo() {
        return sufijo;
    }

    public void setSufijo(String sufijo) {
        this.sufijo = sufijo;
    }

    public String getPaiscod() {
        return paiscod;
    }

    public void setPaiscod(String paiscod) {
        this.paiscod = paiscod;
    }

    public String getIdentificadorAcreedor() {
        return identificadorAcreedor;
    }

    public void setIdentificadorAcreedor(String identificadorAcreedor) {
        this.identificadorAcreedor = identificadorAcreedor;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getAdministradorId() {
        return administradorId;
    }

    public void setAdministradorId(Long administradorId) {
        this.administradorId = administradorId;
    }

    public String getTipoReparto() {
        return tipoReparto;
    }

    void setTipoReparto(String tipoReparto) {
        this.tipoReparto = tipoReparto;
    }

    @JsonIgnore
    public String getTokenQr() {
        return tokenQr;
    }

    public void setTokenQr(String tokenQr) {
        this.tokenQr = tokenQr;
    }
}