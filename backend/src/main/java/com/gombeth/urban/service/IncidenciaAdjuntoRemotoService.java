package com.gombeth.urban.service;

import com.gombeth.urban.dto.IncidenciaAdjuntoResponse;
import com.gombeth.urban.entity.IncidenciaAdjunto;
import com.gombeth.urban.repository.IncidenciaAdjuntoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class IncidenciaAdjuntoRemotoService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final IncidenciaAdjuntoRepository repository;
    private final HttpClient httpClient;
    private final String claveInterna;
    private final String urlRemota;

    public IncidenciaAdjuntoRemotoService(
            IncidenciaAdjuntoRepository repository,
            @Value("${GOMBETH_INCIDENCIAS_INTERNAL_KEY:}")
            String claveInterna,
            @Value(
                    "${GOMBETH_INCIDENCIAS_REMOTE_URL:"
                            + "https://jfgb.es/incidenciacomunidad/"
                            + "api/adjuntos-incidencia.php}"
            )
            String urlRemota
    ) {
        this.repository = repository;
        this.claveInterna = claveInterna == null
                ? ""
                : claveInterna.trim();

        this.urlRemota = urlRemota == null
                ? ""
                : urlRemota.trim();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<IncidenciaAdjuntoResponse> listarPorIncidencia(
            Long incidenciaId
    ) {
        validarIdentificador(
                incidenciaId,
                "incidencia"
        );

        return repository
                .findByIncidenciaIdOrderByFechaSubidaAsc(
                        incidenciaId
                )
                .stream()
                .map(IncidenciaAdjuntoResponse::desde)
                .toList();
    }

    public ContenidoAdjunto obtenerContenido(
            Long incidenciaId,
            Long adjuntoId
    ) {
        validarIdentificador(
                incidenciaId,
                "incidencia"
        );

        validarIdentificador(
                adjuntoId,
                "adjunto"
        );

        IncidenciaAdjunto adjunto = repository
                .findById(adjuntoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Adjunto no encontrado."
                        )
                );

        if (!incidenciaId.equals(adjunto.getIncidenciaId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El adjunto no pertenece a la incidencia indicada."
            );
        }

        validarConfiguracionRemota();

        URI uriRemota = construirUriRemota(
                adjuntoId
        );

        HttpRequest solicitud = HttpRequest
                .newBuilder()
                .uri(uriRemota)
                .timeout(Duration.ofSeconds(30))
                .header(
                        "X-Gombeth-Internal-Key",
                        claveInterna
                )
                .header(
                        "Accept",
                        adjunto.getContentType()
                )
                .GET()
                .build();

        try {
            HttpResponse<byte[]> respuesta = httpClient.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (respuesta.statusCode() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El archivo remoto no está disponible."
                );
            }

            if (
                    respuesta.statusCode() < 200
                            || respuesta.statusCode() >= 300
            ) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El alojamiento remoto rechazó la descarga "
                                + "del adjunto."
                );
            }

            byte[] contenido = respuesta.body();

            if (contenido == null || contenido.length == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El alojamiento remoto devolvió "
                                + "un archivo vacío."
                );
            }

            String contentTypeRespuesta = respuesta
                    .headers()
                    .firstValue("Content-Type")
                    .orElse(adjunto.getContentType());

            MediaType mediaType = obtenerMediaTypePermitido(
                    contentTypeRespuesta
            );

            return new ContenidoAdjunto(
                    contenido,
                    mediaType,
                    adjunto.getNombreOriginal()
            );

        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La descarga del adjunto fue interrumpida.",
                    excepcion
            );

        } catch (IOException excepcion) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con el alojamiento "
                            + "de fotografías.",
                    excepcion
            );
        }
    }

    private void validarIdentificador(
            Long identificador,
            String nombre
    ) {
        if (identificador == null || identificador <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de " + nombre
                            + " no es válido."
            );
        }
    }

    private void validarConfiguracionRemota() {
        if (claveInterna.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No está configurada la clave interna "
                            + "de acceso a las fotografías."
            );
        }

        if (urlRemota.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No está configurada la URL remota "
                            + "de fotografías."
            );
        }
    }

    private URI construirUriRemota(Long adjuntoId) {
        String separador = urlRemota.contains("?")
                ? "&"
                : "?";

        try {
            return URI.create(
                    urlRemota
                            + separador
                            + "adjuntoId="
                            + adjuntoId
            );

        } catch (IllegalArgumentException excepcion) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La URL remota de fotografías "
                            + "no es válida.",
                    excepcion
            );
        }
    }

    private MediaType obtenerMediaTypePermitido(
            String contentType
    ) {
        try {
            MediaType mediaType = MediaType.parseMediaType(
                    contentType
            );

            String tipoNormalizado = (
                    mediaType.getType()
                            + "/"
                            + mediaType.getSubtype()
            ).toLowerCase(Locale.ROOT);

            if (!TIPOS_PERMITIDOS.contains(tipoNormalizado)) {
                throw new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "El archivo remoto no es una imagen permitida."
                );
            }

            return mediaType;

        } catch (IllegalArgumentException excepcion) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El alojamiento remoto devolvió "
                            + "un tipo de archivo no válido.",
                    excepcion
            );
        }
    }

    public static class ContenidoAdjunto {

        private final byte[] contenido;
        private final MediaType mediaType;
        private final String nombreOriginal;

        public ContenidoAdjunto(
                byte[] contenido,
                MediaType mediaType,
                String nombreOriginal
        ) {
            this.contenido = contenido;
            this.mediaType = mediaType;
            this.nombreOriginal = nombreOriginal;
        }

        public byte[] getContenido() {
            return contenido;
        }

        public MediaType getMediaType() {
            return mediaType;
        }

        public String getNombreOriginal() {
            return nombreOriginal;
        }
    }
}