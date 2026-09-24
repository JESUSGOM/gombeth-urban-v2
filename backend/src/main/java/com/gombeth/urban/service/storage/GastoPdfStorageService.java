package com.gombeth.urban.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
public class GastoPdfStorageService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GastoPdfStorageService.class
            );

    private static final long TAMANIO_MAXIMO_BYTES =
            10L * 1024L * 1024L;

    private static final int LONGITUD_BASE_MAXIMA =
            170;

    private final Path facturasPath;

    public GastoPdfStorageService(
            @Value("${app.storage.facturas-path}")
            String facturasPath
    ) {
        if (
                facturasPath == null
                        || facturasPath.isBlank()
        ) {
            throw new IllegalStateException(
                    "La propiedad app.storage.facturas-path "
                            + "es obligatoria."
            );
        }

        this.facturasPath =
                Path.of(
                                facturasPath.trim()
                        )
                        .toAbsolutePath()
                        .normalize();
    }

    /**
     * Guarda una factura PDF en la carpeta compartida
     * con la aplicación histórica.
     *
     * El valor devuelto es únicamente el nombre del
     * fichero, que es exactamente lo que se almacena
     * en contabilidad_gastos.ruta_pdf.
     */
    public String guardarPdf(
            MultipartFile archivo
    ) {
        validarPdf(
                archivo
        );

        try {
            Files.createDirectories(
                    facturasPath
            );

            String nombreOriginal =
                    obtenerNombreSeguro(
                            archivo
                    );

            String nombreAsignado =
                    crearNombreUnico(
                            nombreOriginal
                    );

            Path destino =
                    resolverNombre(
                            nombreAsignado
                    );

            try (
                    InputStream entrada =
                            archivo.getInputStream()
            ) {
                Files.copy(
                        entrada,
                        destino
                );
            }

            return nombreAsignado;

        } catch (IOException error) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el PDF de la factura.",
                    error
            );
        }
    }

    /**
     * Devuelve la ruta física de una factura existente.
     *
     * Acepta tanto los nuevos nombres generados por V2
     * como los nombres históricos existentes en ruta_pdf.
     */
    public Path obtenerPdf(
            String rutaPdf
    ) {
        if (
                rutaPdf == null
                        || rutaPdf.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El gasto no tiene un PDF asociado."
            );
        }

        Path archivo =
                resolverNombre(
                        rutaPdf.trim()
                );

        if (
                !Files.exists(archivo)
                        || !Files.isRegularFile(archivo)
                        || !Files.isReadable(archivo)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se ha encontrado el PDF de la factura."
            );
        }

        return archivo;
    }

    /**
     * Permite consultar si el fichero asociado existe
     * sin lanzar un 404.
     */
    public boolean existePdf(
            String rutaPdf
    ) {
        if (
                rutaPdf == null
                        || rutaPdf.isBlank()
        ) {
            return false;
        }

        try {
            Path archivo =
                    resolverNombre(
                            rutaPdf.trim()
                    );

            return Files.isRegularFile(
                    archivo
            );

        } catch (ResponseStatusException error) {
            return false;
        }
    }

    /**
     * Elimina exclusivamente un PDF que acaba de ser creado
     * por guardarPdf() cuando posteriormente no puede
     * completarse la asociación con el gasto.
     *
     * Este método se utiliza únicamente como mecanismo
     * compensatorio ante errores o rollback.
     *
     * No debe utilizarse para eliminar documentos históricos
     * ni para ofrecer una operación de borrado al usuario.
     */
    public void eliminarPdfRecienCreado(
            String nombreArchivo
    ) {
        if (
                nombreArchivo == null
                        || nombreArchivo.isBlank()
        ) {
            return;
        }

        try {
            Path archivo =
                    resolverNombre(
                            nombreArchivo.trim()
                    );

            Files.deleteIfExists(
                    archivo
            );

        } catch (Exception error) {
            /*
             * Una limpieza compensatoria nunca debe ocultar
             * la excepción original que provocó el rollback.
             *
             * Dejamos constancia en log para poder revisar
             * manualmente el fichero si excepcionalmente
             * no pudiera eliminarse.
             */
            log.error(
                    "No se pudo eliminar el PDF recién creado: {}",
                    nombreArchivo,
                    error
            );
        }
    }

    private void validarPdf(
            MultipartFile archivo
    ) {
        if (
                archivo == null
                        || archivo.isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un archivo PDF."
            );
        }

        if (
                archivo.getSize() <= 0
                        || archivo.getSize()
                        > TAMANIO_MAXIMO_BYTES
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El PDF no puede superar los 10 MB."
            );
        }

        String nombre =
                obtenerNombreSeguro(
                        archivo
                );

        if (
                !nombre.toLowerCase(
                                Locale.ROOT
                        )
                        .endsWith(".pdf")
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se permiten archivos PDF."
            );
        }

        String contentType =
                archivo.getContentType();

        if (
                contentType != null
                        && !contentType.isBlank()
        ) {
            String tipo =
                    contentType
                            .trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (
                    !MediaType.APPLICATION_PDF_VALUE
                            .equals(tipo)
                            && !MediaType
                            .APPLICATION_OCTET_STREAM_VALUE
                            .equals(tipo)
            ) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El archivo recibido no es un PDF."
                );
            }
        }

        validarCabeceraPdf(
                archivo
        );
    }

    /**
     * No nos fiamos únicamente de la extensión ni del
     * Content-Type enviado por el navegador.
     */
    private void validarCabeceraPdf(
            MultipartFile archivo
    ) {
        try (
                InputStream entrada =
                        archivo.getInputStream()
        ) {
            byte[] cabecera =
                    entrada.readNBytes(
                            5
                    );

            boolean pdf =
                    cabecera.length == 5
                            && cabecera[0] == '%'
                            && cabecera[1] == 'P'
                            && cabecera[2] == 'D'
                            && cabecera[3] == 'F'
                            && cabecera[4] == '-';

            if (!pdf) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El contenido del archivo "
                                + "no corresponde a un PDF válido."
                );
            }

        } catch (IOException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pudo leer el PDF recibido.",
                    error
            );
        }
    }

    private String obtenerNombreSeguro(
            MultipartFile archivo
    ) {
        String nombre =
                archivo.getOriginalFilename();

        if (
                nombre == null
                        || nombre.isBlank()
        ) {
            return "factura.pdf";
        }

        nombre =
                nombre.replace(
                        "\\",
                        "/"
                );

        int ultimaBarra =
                nombre.lastIndexOf('/');

        if (ultimaBarra >= 0) {
            nombre =
                    nombre.substring(
                            ultimaBarra + 1
                    );
        }

        nombre =
                nombre.replaceAll(
                        "[\\\\/:*?\"<>|\\r\\n\\t]",
                        "_"
                );

        nombre =
                nombre.trim();

        if (nombre.isBlank()) {
            return "factura.pdf";
        }

        if (
                nombre.toLowerCase(
                                Locale.ROOT
                        )
                        .endsWith(".pdf")
        ) {
            String base =
                    nombre.substring(
                            0,
                            nombre.length() - 4
                    );

            if (
                    base.length()
                            > LONGITUD_BASE_MAXIMA
            ) {
                base =
                        base.substring(
                                0,
                                LONGITUD_BASE_MAXIMA
                        );
            }

            return base + ".pdf";
        }

        return nombre;
    }

    /**
     * Mantiene el estilo histórico:
     *
     * 1776847724991_FAT-2026-054412.pdf
     *
     * Si por una coincidencia excepcional ya existiera
     * ese nombre, añade un contador sin sobrescribirlo.
     */
    private String crearNombreUnico(
            String nombreOriginal
    ) {
        long instante =
                System.currentTimeMillis();

        String nombre =
                instante
                        + "_"
                        + nombreOriginal;

        int contador = 1;

        while (
                Files.exists(
                        resolverNombre(
                                nombre
                        )
                )
        ) {
            nombre =
                    instante
                            + "_"
                            + contador
                            + "_"
                            + nombreOriginal;

            contador++;
        }

        return nombre;
    }

    /**
     * Impide que un valor manipulado de ruta_pdf pueda
     * salir fuera de la carpeta de facturas.
     */
    private Path resolverNombre(
            String nombre
    ) {
        if (
                nombre == null
                        || nombre.isBlank()
                        || nombre.contains("/")
                        || nombre.contains("\\")
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre del PDF no es válido."
            );
        }

        Path resultado =
                facturasPath
                        .resolve(
                                nombre
                        )
                        .normalize();

        if (
                !resultado.startsWith(
                        facturasPath
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La ruta del PDF no es válida."
            );
        }

        return resultado;
    }
}