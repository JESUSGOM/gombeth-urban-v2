package com.gombeth.urban.dto;

import java.util.List;

public record LoginResponse(
        boolean ok,
        Long usuarioId,
        String username,
        Long administradorId,
        String administradorNombre,
        List<String> roles,
        String mensaje
) {}