package com.gombeth.urban.dto.proveedor;

public record ProveedorComunidadResponse(
        Long asociacionId,
        Long proveedorId,
        String nombre,
        String nifCif,
        String telefono,
        String email,
        String observaciones,
        Long cuentaContableId,
        Boolean proveedorActivo,
        Boolean asociacionActiva
) {
}