package com.gombeth.urban.dto.remesa;

import java.time.LocalDateTime;

public record RemesaEventoResponse(
        Long id,
        Long remesaId,
        Long comunidadId,
        Long usuarioId,
        String tipoEvento,
        String estadoAnterior,
        String estadoNuevo,
        String formato,
        String nombreArchivo,
        LocalDateTime fechaEvento,
        String detalle
) {}