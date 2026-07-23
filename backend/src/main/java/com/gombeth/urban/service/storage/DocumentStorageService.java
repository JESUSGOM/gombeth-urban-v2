package com.gombeth.urban.service.storage;

import com.gombeth.urban.entity.Comunidad;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentStorageService {

    private final DocumentNameService documentNameService;

    private final DocumentPathService documentPathService;

    private final Path basePath;

    public DocumentStorageService(
            DocumentNameService documentNameService,
            DocumentPathService documentPathService,
            @Value("${app.storage.base-path}")
            String basePath
    ) {
        this.documentNameService =
                documentNameService;

        this.documentPathService =
                documentPathService;

        this.basePath =
                normalizarBasePath(
                        basePath
                );
    }

    public Path guardarRemesaC19(
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

        Files.createDirectories(
                carpeta
        );

        String nombreArchivo =
                documentNameService.nombreRemesaC19(
                        comunidad,
                        fechaEmision,
                        fechaCobro,
                        esquema
                );

        Path destino =
                carpeta.resolve(
                        nombreArchivo
                );

        Files.write(
                destino,
                contenido.getBytes(
                        StandardCharsets.ISO_8859_1
                )
        );

        return destino;
    }

    public Path guardarRemesaXml(
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

        Files.createDirectories(
                carpeta
        );

        String nombreArchivo =
                documentNameService.nombreRemesaXml(
                        comunidad,
                        fechaEmision,
                        fechaCobro,
                        esquema
                );

        Path destino =
                carpeta.resolve(
                        nombreArchivo
                );

        Files.write(
                destino,
                contenido.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        return destino;
    }

    private Path normalizarBasePath(
            String valor
    ) {
        if (
                valor == null
                        || valor.isBlank()
        ) {
            throw new IllegalStateException(
                    "La propiedad app.storage.base-path "
                            + "es obligatoria."
            );
        }

        return Path.of(
                        valor.trim()
                )
                .toAbsolutePath()
                .normalize();
    }
}