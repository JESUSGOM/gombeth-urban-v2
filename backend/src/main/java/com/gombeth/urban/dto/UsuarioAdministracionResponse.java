package com.gombeth.urban.dto;

import java.util.List;

public record UsuarioAdministracionResponse(
        Long usuarioId,
        String username,
        AdministradorResumenResponse administrador,
        List<RolResponse> roles,
        List<ComunidadNombreResponse> comunidadesDirectas,
        List<ComunidadNombreResponse> comunidadesCompartidas
) {
}