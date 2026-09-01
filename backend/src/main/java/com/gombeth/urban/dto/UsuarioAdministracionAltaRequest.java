package com.gombeth.urban.dto;

import java.util.List;

public class UsuarioAdministracionAltaRequest {

    private String username;

    private String passwordInicial;

    private Long administradorId;

    private List<Long> rolIds;

    private List<Long> comunidadCompartidaIds;

    public String getUsername() {
        return username;
    }

    public void setUsername(
            String username
    ) {
        this.username = username;
    }

    public String getPasswordInicial() {
        return passwordInicial;
    }

    public void setPasswordInicial(
            String passwordInicial
    ) {
        this.passwordInicial = passwordInicial;
    }

    public Long getAdministradorId() {
        return administradorId;
    }

    public void setAdministradorId(
            Long administradorId
    ) {
        this.administradorId = administradorId;
    }

    public List<Long> getRolIds() {
        return rolIds;
    }

    public void setRolIds(
            List<Long> rolIds
    ) {
        this.rolIds = rolIds;
    }

    public List<Long> getComunidadCompartidaIds() {
        return comunidadCompartidaIds;
    }

    public void setComunidadCompartidaIds(
            List<Long> comunidadCompartidaIds
    ) {
        this.comunidadCompartidaIds =
                comunidadCompartidaIds;
    }
}