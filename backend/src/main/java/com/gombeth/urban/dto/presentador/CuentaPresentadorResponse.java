package com.gombeth.urban.dto.presentador;

public record CuentaPresentadorResponse(
        Long id,
        String alias,
        String banco,
        String identificadorPresentador,
        String nifCif,
        String sufijo,
        String iban,
        String bic,
        boolean activa,
        String observaciones
) {
}