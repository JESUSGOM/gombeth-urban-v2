package com.gombeth.urban.controller;

import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ContabilidadGastoPdfService;
import com.gombeth.urban.service.ContabilidadGastoService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/gastos")
public class ContabilidadGastoPdfController {

    private final ContabilidadGastoService
            gastoService;

    private final ContabilidadGastoPdfService
            gastoPdfService;

    private final AccesoComunidadService
            accesoComunidadService;

    public ContabilidadGastoPdfController(
            ContabilidadGastoService gastoService,
            ContabilidadGastoPdfService gastoPdfService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.gastoService =
                gastoService;

        this.gastoPdfService =
                gastoPdfService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Sube la factura PDF de un gasto.
     *
     * El servicio de almacenamiento guarda físicamente
     * el documento en la carpeta compartida y en ruta_pdf
     * se conserva únicamente el nombre del fichero.
     */
    @PostMapping(
            value = "/{id}/pdf",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ContabilidadGasto subirPdf(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        ContabilidadGasto gasto =
                obtenerGasto(
                        id
                );

        /*
         * Validamos primero el acceso. De esta forma un
         * usuario sin permiso no puede averiguar ni
         * modificar documentos de otra comunidad.
         */
        accesoComunidadService.validarAcceso(
                authentication,
                gasto.getComunidadId()
        );

        try {
            return gastoPdfService.subirPdf(
                    id,
                    file
            );

        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage(),
                    error
            );

        } catch (IllegalStateException error) {
            /*
             * Durante la convivencia no permitimos
             * reemplazar silenciosamente un PDF ya
             * asociado al gasto.
             */
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    error.getMessage(),
                    error
            );
        }
    }

    /**
     * Visualiza inline el PDF de una factura.
     *
     * Funciona igualmente para los documentos históricos
     * cuyo nombre ya está almacenado en ruta_pdf.
     */
    @GetMapping(
            value = "/{id}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<Resource> visualizarPdf(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ContabilidadGasto gasto =
                obtenerGasto(
                        id
                );

        accesoComunidadService.validarAcceso(
                authentication,
                gasto.getComunidadId()
        );

        Path ruta =
                gastoPdfService.obtenerPdf(
                        id
                );

        FileSystemResource recurso =
                new FileSystemResource(
                        ruta
                );

        try {
            String nombreArchivo =
                    ruta.getFileName()
                            .toString();

            String contentDisposition =
                    ContentDisposition
                            .inline()
                            .filename(
                                    nombreArchivo,
                                    StandardCharsets.UTF_8
                            )
                            .build()
                            .toString();

            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.APPLICATION_PDF
                    )
                    .contentLength(
                            Files.size(
                                    ruta
                            )
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            contentDisposition
                    )
                    .body(
                            recurso
                    );

        } catch (IOException error) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo abrir el PDF de la factura.",
                    error
            );
        }
    }

    private ContabilidadGasto obtenerGasto(
            Long gastoId
    ) {
        if (
                gastoId == null
                        || gastoId <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del gasto no es válido."
            );
        }

        try {
            return gastoService.findById(
                    gastoId
            );

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    error.getMessage(),
                    error
            );
        }
    }
}