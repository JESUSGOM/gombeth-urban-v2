package com.gombeth.urban.dto;

public class CambioPasswordRequest {

    private String username;

    private String passwordActual;

    private String nuevaPassword;

    private String confirmarPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(
            String username
    ) {
        this.username = username;
    }

    public String getPasswordActual() {
        return passwordActual;
    }

    public void setPasswordActual(
            String passwordActual
    ) {
        this.passwordActual = passwordActual;
    }

    public String getNuevaPassword() {
        return nuevaPassword;
    }

    public void setNuevaPassword(
            String nuevaPassword
    ) {
        this.nuevaPassword = nuevaPassword;
    }

    public String getConfirmarPassword() {
        return confirmarPassword;
    }

    public void setConfirmarPassword(
            String confirmarPassword
    ) {
        this.confirmarPassword =
                confirmarPassword;
    }
}