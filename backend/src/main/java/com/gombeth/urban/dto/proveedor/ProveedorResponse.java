package com.gombeth.urban.dto.proveedor;

public record ProveedorResponse(
        Long id,
        String nombre,
        String nifCif,
        String telefono,
        String email,
        String observaciones,
        Boolean activo
) {
}
