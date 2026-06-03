package com.gombeth.urban.dto;

public class LoginResponse {

    private boolean ok;
    private Long usuarioId;
    private String username;
    private Long administradorId;
    private String administradorNombre;
    private String mensaje;

    public LoginResponse(
            boolean ok,
            Long usuarioId,
            String username,
            Long administradorId,
            String administradorNombre,
            String mensaje
    ) {
        this.ok = ok;
        this.usuarioId = usuarioId;
        this.username = username;
        this.administradorId = administradorId;
        this.administradorNombre = administradorNombre;
        this.mensaje = mensaje;
    }

    public boolean isOk() {
        return ok;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getUsername() {
        return username;
    }

    public Long getAdministradorId() {
        return administradorId;
    }

    public String getAdministradorNombre() {
        return administradorNombre;
    }

    public String getMensaje() {
        return mensaje;
    }
}