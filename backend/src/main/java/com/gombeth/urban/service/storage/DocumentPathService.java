package com.gombeth.urban.service.storage;

import com.gombeth.urban.entity.Comunidad;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;

@Service
public class DocumentPathService {

    public Path carpetaRemesaC19(
            Path basePath,
            Comunidad comunidad,
            LocalDate fechaEmision
    ) {
        return basePath
                .resolve("Remesas")
                .resolve(limpiar(comunidad.getNombre()))
                .resolve(String.valueOf(fechaEmision.getYear()))
                .resolve(String.format("%02d", fechaEmision.getMonthValue()))
                .resolve("C19");
    }

    private String limpiar(String texto) {
        if (texto == null) {
            return "COMUNIDAD";
        }

        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("\\p{M}", "");
        texto = texto.replaceAll("[\\\\/:*?\"<>|]", "");
        texto = texto.trim();

        return texto.isBlank() ? "COMUNIDAD" : texto;
    }

    public Path carpetaRemesaXml(
            Path basePath,
            Comunidad comunidad,
            LocalDate fechaEmision
    ) {
        return basePath
                .resolve("Remesas")
                .resolve(limpiar(comunidad.getNombre()))
                .resolve(String.valueOf(fechaEmision.getYear()))
                .resolve(String.format("%02d", fechaEmision.getMonthValue()))
                .resolve("XML");
    }
}