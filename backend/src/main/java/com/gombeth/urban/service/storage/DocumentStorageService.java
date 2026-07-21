package com.gombeth.urban.service.storage;

import com.gombeth.urban.entity.Comunidad;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentStorageService {

    private final DocumentNameService documentNameService;
    private final DocumentPathService documentPathService;

    public DocumentStorageService(
            DocumentNameService documentNameService,
            DocumentPathService documentPathService
    ) {
        this.documentNameService = documentNameService;
        this.documentPathService = documentPathService;
    }

    public Path guardarRemesaC19(
            Path basePath,
            Comunidad comunidad,
            String contenido,
            java.time.LocalDate fechaEmision,
            java.time.LocalDate fechaCobro,
            String esquema
    ) throws IOException {

        Path carpeta =
                documentPathService.carpetaRemesaC19(
                        basePath,
                        comunidad,
                        fechaEmision
                );

        Files.createDirectories(carpeta);

        String nombreArchivo =
                documentNameService.nombreRemesaC19(
                        comunidad,
                        fechaEmision,
                        fechaCobro,
                        esquema
                );

        Path destino =
                carpeta.resolve(nombreArchivo);

        Files.write(
                destino,
                contenido.getBytes(StandardCharsets.ISO_8859_1)
        );

        return destino;
    }

    public Path guardarRemesaXml(
            Path basePath,
            Comunidad comunidad,
            String contenido,
            java.time.LocalDate fechaEmision,
            java.time.LocalDate fechaCobro,
            String esquema
    ) throws IOException {

        Path carpeta =
                documentPathService.carpetaRemesaXml(
                        basePath,
                        comunidad,
                        fechaEmision
                );

        Files.createDirectories(carpeta);

        String nombreArchivo =
                documentNameService.nombreRemesaXml(
                        comunidad,
                        fechaEmision,
                        fechaCobro,
                        esquema
                );

        Path destino =
                carpeta.resolve(nombreArchivo);

        Files.write(
                destino,
                contenido.getBytes(StandardCharsets.UTF_8)
        );

        return destino;
    }
}