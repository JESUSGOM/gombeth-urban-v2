package com.gombeth.urban.dto;

public record LoginResponse(
        boolean ok,
        Long usuarioId,
        String username,
        Long administradorId,
        String administradorNombre,
        String mensaje
) {}