package com.gombeth.urban.service.storage;

import com.gombeth.urban.entity.Comunidad;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DocumentNameService {

    private static final DateTimeFormatter F =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public String nombreRemesaC19(
            Comunidad comunidad,
            LocalDate fechaEmision,
            LocalDate fechaCobro,
            String esquema
    ) {
        return limpiar(comunidad.getNombre())
                + "_"
                + esquema
                + "_EMISION_"
                + F.format(fechaEmision)
                + "_COBRO_"
                + F.format(fechaCobro)
                + ".c19";
    }

    private String limpiar(String texto) {
        if (texto == null) {
            return "COMUNIDAD";
        }
        texto = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        );
        texto = texto.replaceAll("\\p{M}", "");
        texto = texto.toUpperCase();
        texto = texto.replaceAll("[^A-Z0-9 ]", "");
        texto = texto.trim();
        texto = texto.replace(" ", "_");
        while (texto.contains("__")) {
            texto = texto.replace("__", "_");
        }
        return texto;
    }

    public String nombreRemesaXml(
            Comunidad comunidad,
            LocalDate fechaEmision,
            LocalDate fechaCobro,
            String esquema
    ) {
        return limpiar(comunidad.getNombre())
                + "_"
                + esquema
                + "_EMISION_"
                + F.format(fechaEmision)
                + "_COBRO_"
                + F.format(fechaCobro)
                + ".xml";
    }
}