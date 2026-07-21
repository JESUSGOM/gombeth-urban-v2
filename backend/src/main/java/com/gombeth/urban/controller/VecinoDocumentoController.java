package com.gombeth.urban.controller;

import com.gombeth.urban.dto.VecinoDocumentoResponse;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.entity.VecinoDocumento;
import com.gombeth.urban.repository.VecinoDocumentoRepository;
import com.gombeth.urban.repository.VecinoRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/vecino-documentos")
public class VecinoDocumentoController {

    private static final long TAMANIO_MAXIMO_BYTES =
            10L * 1024L * 1024L;

    private static final List<String> TIPOS_PERMITIDOS =
            List.of(
                    "MANDATO_SEPA_FIRMADO",
                    "MANDATO",
                    "OTRO"
            );

    private static final List<String> CONTENT_TYPES_PERMITIDOS =
            List.of(
                    MediaType.APPLICATION_PDF_VALUE,
                    MediaType.IMAGE_JPEG_VALUE,
                    MediaType.IMAGE_PNG_VALUE
            );

    private final VecinoDocumentoRepository documentoRepository;
    private final VecinoRepository vecinoRepository;

    public VecinoDocumentoController(
            VecinoDocumentoRepository documentoRepository,
            VecinoRepository vecinoRepository
    ) {
        this.documentoRepository = documentoRepository;
        this.vecinoRepository = vecinoRepository;
    }

    @GetMapping("/vecino/{vecinoId}")
    @Transactional(readOnly = true)
    public List<VecinoDocumentoResponse> listarPorVecino(
            @PathVariable Long vecinoId
    ) {
        obtenerVecino(vecinoId);

        return documentoRepository
                .findByVecinoIdOrderByFechaSubidaDescIdDesc(
                        vecinoId
                )
                .stream()
                .map(VecinoDocumentoResponse::desde)
                .toList();
    }

    @GetMapping("/{documentoId}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> visualizar(
            @PathVariable Long documentoId
    ) {
        VecinoDocumento documento =
                obtenerDocumento(documentoId);

        MediaType mediaType =
                obtenerMediaType(
                        documento.getContentType()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        crearContentDisposition(
                                "inline",
                                documento.getNombreArchivo()
                        )
                )
                .contentType(mediaType)
                .contentLength(
                        documento.getContenido().length
                )
                .body(
                        documento.getContenido()
                );
    }

    @GetMapping("/{documentoId}/descarga")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargar(
            @PathVariable Long documentoId
    ) {
        VecinoDocumento documento =
                obtenerDocumento(documentoId);

        MediaType mediaType =
                obtenerMediaType(
                        documento.getContentType()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        crearContentDisposition(
                                "attachment",
                                documento.getNombreArchivo()
                        )
                )
                .contentType(mediaType)
                .contentLength(
                        documento.getContenido().length
                )
                .body(
                        documento.getContenido()
                );
    }

    @PostMapping("/vecino/{vecinoId}")
    @Transactional
    public ResponseEntity<VecinoDocumentoResponse> subir(
            @PathVariable Long vecinoId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    defaultValue = "MANDATO_SEPA_FIRMADO"
            )
            String tipoDocumento
    ) {
        Vecino vecino =
                obtenerVecino(vecinoId);

        validarArchivo(file);

        String tipoNormalizado =
                normalizarTipoDocumento(
                        tipoDocumento
                );

        VecinoDocumento documento =
                new VecinoDocumento();

        documento.setVecinoId(vecinoId);
        documento.setTipoDocumento(
                tipoNormalizado
        );
        documento.setNombreArchivo(
                obtenerNombreArchivo(file)
        );
        documento.setContentType(
                normalizarContentType(
                        file.getContentType()
                )
        );
        documento.setContenido(
                obtenerContenido(file)
        );
        documento.setFechaSubida(
                LocalDateTime.now()
        );

        VecinoDocumento documentoGuardado =
                documentoRepository.save(documento);

        if (
                "MANDATO_SEPA_FIRMADO"
                        .equals(tipoNormalizado)
                        || "MANDATO".equals(tipoNormalizado)
        ) {
            vecino.setRutaMandatoFirmado(
                    "BD:" + documentoGuardado.getId()
            );

            vecinoRepository.save(vecino);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        VecinoDocumentoResponse.desde(
                                documentoGuardado
                        )
                );
    }

    @DeleteMapping("/{documentoId}")
    @Transactional
    public ResponseEntity<Void> eliminar(
            @PathVariable Long documentoId
    ) {
        VecinoDocumento documento =
                obtenerDocumento(documentoId);

        Vecino vecino =
                obtenerVecino(
                        documento.getVecinoId()
                );

        String referenciaDocumento =
                "BD:" + documento.getId();

        if (
                referenciaDocumento.equals(
                        vecino.getRutaMandatoFirmado()
                )
        ) {
            vecino.setRutaMandatoFirmado(null);
            vecinoRepository.save(vecino);
        }

        documentoRepository.delete(documento);

        return ResponseEntity.noContent().build();
    }

    private VecinoDocumento obtenerDocumento(
            Long documentoId
    ) {
        if (
                documentoId == null
                        || documentoId <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del documento no es válido."
            );
        }

        return documentoRepository
                .findById(documentoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Documento no encontrado: "
                                        + documentoId
                        )
                );
    }

    private Vecino obtenerVecino(
            Long vecinoId
    ) {
        if (
                vecinoId == null
                        || vecinoId <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del propietario no es válido."
            );
        }

        return vecinoRepository
                .findById(vecinoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Propietario no encontrado: "
                                        + vecinoId
                        )
                );
    }

    private void validarArchivo(
            MultipartFile file
    ) {
        if (
                file == null
                        || file.isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un documento."
            );
        }

        if (
                file.getSize() <= 0
                        || file.getSize()
                        > TAMANIO_MAXIMO_BYTES
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El documento no puede superar los 10 MB."
            );
        }

        String contentType =
                normalizarContentType(
                        file.getContentType()
                );

        if (
                !CONTENT_TYPES_PERMITIDOS
                        .contains(contentType)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se permiten documentos PDF, JPG o PNG."
            );
        }
    }

    private String normalizarTipoDocumento(
            String tipoDocumento
    ) {
        String tipoNormalizado =
                tipoDocumento == null
                        ? ""
                        : tipoDocumento
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                !TIPOS_PERMITIDOS
                        .contains(tipoNormalizado)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El tipo de documento no es válido."
            );
        }

        return tipoNormalizado;
    }

    private String normalizarContentType(
            String contentType
    ) {
        if (
                contentType == null
                        || contentType.isBlank()
        ) {
            return MediaType
                    .APPLICATION_OCTET_STREAM_VALUE;
        }

        return contentType
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String obtenerNombreArchivo(
            MultipartFile file
    ) {
        String nombreOriginal =
                file.getOriginalFilename();

        if (
                nombreOriginal == null
                        || nombreOriginal.isBlank()
        ) {
            return "documento";
        }

        String nombreLimpio =
                nombreOriginal
                        .replace("\\", "/");

        int ultimaBarra =
                nombreLimpio.lastIndexOf('/');

        if (ultimaBarra >= 0) {
            nombreLimpio =
                    nombreLimpio.substring(
                            ultimaBarra + 1
                    );
        }

        nombreLimpio =
                nombreLimpio.replaceAll(
                        "[\\r\\n\\t\"]",
                        "_"
                );

        if (nombreLimpio.length() > 255) {
            nombreLimpio =
                    nombreLimpio.substring(
                            0,
                            255
                    );
        }

        return nombreLimpio;
    }

    private byte[] obtenerContenido(
            MultipartFile file
    ) {
        try {
            return file.getBytes();
        } catch (IOException error) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo leer el documento recibido.",
                    error
            );
        }
    }

    private MediaType obtenerMediaType(
            String contentType
    ) {
        try {
            return MediaType.parseMediaType(
                    contentType
            );
        } catch (Exception error) {
            return MediaType
                    .APPLICATION_OCTET_STREAM;
        }
    }

    private String crearContentDisposition(
            String disposicion,
            String nombreArchivo
    ) {
        String nombreSeguro =
                obtenerNombreArchivoSeguro(
                        nombreArchivo
                );

        String nombreCodificado =
                java.net.URLEncoder.encode(
                                nombreSeguro,
                                StandardCharsets.UTF_8
                        )
                        .replace("+", "%20");

        return disposicion
                + "; filename=\""
                + nombreSeguro
                + "\"; filename*=UTF-8''"
                + nombreCodificado;
    }

    private String obtenerNombreArchivoSeguro(
            String nombreArchivo
    ) {
        if (
                nombreArchivo == null
                        || nombreArchivo.isBlank()
        ) {
            return "documento";
        }

        return nombreArchivo
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\r", "_")
                .replace("\n", "_")
                .replace("\"", "_");
    }
}