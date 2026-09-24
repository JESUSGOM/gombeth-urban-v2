package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Acta;
import com.gombeth.urban.repository.ActaRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.PdfActaService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.server.ResponseStatusException;
import com.gombeth.urban.dto.ActaResumenResponse;
import com.gombeth.urban.dto.ActaDetalleResponse;
import com.gombeth.urban.dto.ActaGuardarRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.EstadoActa;

import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/actas")
public class ActaController {

    private final ActaRepository actaRepository;

    private final PdfActaService pdfActaService;

    private final AccesoComunidadService
            accesoComunidadService;

    public ActaController(
            ActaRepository actaRepository,
            PdfActaService pdfActaService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.actaRepository =
                actaRepository;

        this.pdfActaService =
                pdfActaService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @GetMapping("/comunidad/{comunidadId}")
    public List<ActaResumenResponse> listarPorComunidad(
            @PathVariable Long comunidadId,
            Authentication authentication
    ) {

        Comunidad comunidad =
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                comunidadId
                        );

        return actaRepository
                .findByComunidad(
                        comunidad
                )
                .stream()
                .map(acta ->
                        new ActaResumenResponse(
                                acta.getId(),
                                acta.getComunidad().getId(),
                                acta.getTitulo(),
                                acta.getFechaReunion(),
                                acta.getEstado() != null
                                        ? acta.getEstado().name()
                                        : null
                        )
                )
                .toList();
    }

    @PostMapping
    public ActaDetalleResponse crear(
            @RequestBody ActaGuardarRequest request,
            Authentication authentication
    ) {

        Comunidad comunidad =
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                request.comunidadId()
                        );

        Acta acta =
                new Acta();

        acta.setComunidad(
                comunidad
        );

        acta.setTitulo(
                request.titulo()
        );

        acta.setFechaReunion(
                request.fechaReunion()
        );

        acta.setContenido(
                request.contenido()
        );

        acta.setEstado(
                EstadoActa.BORRADOR
        );

        Acta guardada =
                actaRepository.save(
                        acta
                );

        return new ActaDetalleResponse(
                guardada.getId(),
                guardada.getComunidad().getId(),
                guardada.getTitulo(),
                guardada.getFechaReunion(),
                guardada.getContenido(),
                guardada.getEstado().name()
        );
    }

    @PutMapping("/{id}")
    public ActaDetalleResponse actualizar(
            @PathVariable Long id,
            @RequestBody ActaGuardarRequest request,
            Authentication authentication
    ) {

        Acta acta =
                obtenerActa(
                        id
                );

        accesoComunidadService.validarAcceso(
                authentication,
                acta.getComunidad().getId()
        );

        if (acta.getEstado() != EstadoActa.BORRADOR) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden modificar actas "
                            + "en estado BORRADOR."
            );
        }

        acta.setTitulo(
                request.titulo()
        );

        acta.setFechaReunion(
                request.fechaReunion()
        );

        acta.setContenido(
                request.contenido()
        );

        Acta guardada =
                actaRepository.save(
                        acta
                );

        return new ActaDetalleResponse(
                guardada.getId(),
                guardada.getComunidad().getId(),
                guardada.getTitulo(),
                guardada.getFechaReunion(),
                guardada.getContenido(),
                guardada.getEstado().name()
        );
    }

    @GetMapping("/{id}")
    public ActaDetalleResponse obtenerDetalle(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Acta acta =
                obtenerActa(
                        id
                );

        accesoComunidadService.validarAcceso(
                authentication,
                acta.getComunidad().getId()
        );

        return new ActaDetalleResponse(
                acta.getId(),
                acta.getComunidad().getId(),
                acta.getTitulo(),
                acta.getFechaReunion(),
                acta.getContenido(),
                acta.getEstado() != null
                        ? acta.getEstado().name()
                        : null
        );
    }

    @GetMapping(
            value = "/{id}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<Resource> visualizarPdf(
            @PathVariable Long id,
            Authentication authentication
    ) {

        Acta acta =
                obtenerActa(
                        id
                );

        accesoComunidadService.validarAcceso(
                authentication,
                acta.getComunidad().getId()
        );

        Path ruta;

        try {

            ruta =
                    pdfActaService.obtenerPdf(
                            acta
                    );

        } catch (IllegalStateException error) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    error.getMessage(),
                    error
            );

        } catch (Exception error) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo obtener el PDF del acta.",
                    error
            );
        }

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
                    "No se pudo abrir el PDF del acta.",
                    error
            );
        }
    }

    private Acta obtenerActa(
            Long actaId
    ) {

        if (
                actaId == null
                        || actaId <= 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del acta no es válido."
            );
        }

        return actaRepository
                .findById(
                        actaId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No existe el acta "
                                        + actaId
                        )
                );
    }
}