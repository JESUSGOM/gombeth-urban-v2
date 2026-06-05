package com.gombeth.urban.entity;

import com.gombeth.urban.util.AesEncryptor;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vecinos")
public class Vecino {

    @Id
    private Long id;

    @Column(name = "comunidad_id", nullable = false)
    private Long comunidadId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "telefono_1", length = 20)
    private String telefono1;

    @Column(name = "telefono_2", length = 20)
    private String telefono2;

    @Column(name = "telefono_3", length = 20)
    private String telefono3;

    @Convert(converter = AesEncryptor.class)
    @Column(length = 255)
    private String nif;

    @Convert(converter = AesEncryptor.class)
    @Column(length = 255)
    private String iban;

    @Convert(converter = AesEncryptor.class)
    @Column(length = 255)
    private String bic;

    @Column(length = 50)
    private String direccion;

    @Column(length = 50)
    private String poblacion;

    @Column(name = "codigopostal", length = 5)
    private String codigoPostal;

    @Column(length = 40)
    private String provincia;

    @Column(name = "pais_cod", length = 2)
    private String paisCod;

    @Convert(converter = AesEncryptor.class)
    @Column(length = 150)
    private String email;

    @Column(name = "referencia_mandato", length = 35)
    private String referenciaMandato;

    @Column(name = "piso_porton", length = 50)
    private String pisoPorton;

    @Column(name = "direccion_notificacion", length = 200)
    private String direccionNotificacion;

    @Column(name = "ruta_mandato_firmado", length = 255)
    private String rutaMandatoFirmado;

    @Column(name = "cuenta_contable_id")
    private Long cuentaContableId;

    @Column(name = "cuenta_contable", length = 15)
    private String cuentaContable;

    @Column(nullable = false, length = 50)
    private String vivienda;

    @Column(precision = 10, scale = 4)
    private BigDecimal coeficiente;

    @Column(nullable = false)
    private Boolean domiciliado;

    @Column(name = "envio_digital")
    private Boolean envioDigital;

    @Column(nullable = false)
    private Boolean activo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notas;

    public Long getId() {return id;}
    public Long getComunidadId() {return comunidadId;}
    public void setComunidadId(Long comunidadId) {this.comunidadId = comunidadId;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public String getTelefono1() {return telefono1;}
    public void setTelefono1(String telefono1) {this.telefono1 = telefono1;}
    public String getTelefono2() {return telefono2;}
    public void setTelefono2(String telefono2) {this.telefono2 = telefono2;}
    public String getTelefono3() {return telefono3;}
    public void setTelefono3(String telefono3) {this.telefono3 = telefono3;}
    public String getNif() {return nif;}
    public void setNif(String nif) {this.nif = nif;}
    public String getIban() {return iban;}
    public void setIban(String iban) {this.iban = iban;}
    public String getBic() {return bic;}
    public void setBic(String bic) {this.bic = bic;}
    public String getDireccion() {return direccion;}
    public void setDireccion(String direccion) {this.direccion = direccion;}
    public String getPoblacion() {return poblacion;}
    public void setPoblacion(String poblacion) {this.poblacion = poblacion;}
    public String getCodigoPostal() {return codigoPostal;}
    public void setCodigoPostal(String codigoPostal) {this.codigoPostal = codigoPostal;}
    public String getProvincia() {return provincia;}
    public void setProvincia(String provincia) {this.provincia = provincia;}
    public String getPaisCod() {return paisCod;}
    public void setPaisCod(String paisCod) {this.paisCod = paisCod;}
    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}
    public String getVivienda() {return vivienda;}
    public void setVivienda(String vivienda) {this.vivienda = vivienda;}
    public boolean isDomiciliado() {return domiciliado;}
    public void setDomiciliado(boolean domiciliado) {this.domiciliado = domiciliado;}
    public boolean isActivo() {return activo;}
    public void setActivo(boolean activo) {this.activo = activo;}
    public String getReferenciaMandato() {return referenciaMandato;}
    public void setReferenciaMandato(String referenciaMandato) {this.referenciaMandato = referenciaMandato;}
    public String getDireccionNotificacion() {return direccionNotificacion;}
    public void setDireccionNotificacion(String direccionNotificacion) {this.direccionNotificacion = direccionNotificacion;}
    public String getRutaMandatoFirmado() {return rutaMandatoFirmado;}
    public void setRutaMandatoFirmado(String rutaMandatoFirmado) {this.rutaMandatoFirmado = rutaMandatoFirmado;}
    public BigDecimal getCoeficiente() {return coeficiente;}
    public void setCoeficiente(BigDecimal coeficiente) {this.coeficiente = coeficiente;}
    public String getNotas() {return notas;}
    public void setNotas(String notas) {this.notas = notas;}
}