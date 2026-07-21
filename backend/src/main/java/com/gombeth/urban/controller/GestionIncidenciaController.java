package com.gombeth.urban.controller;

import com.gombeth.urban.dto.GestionIncidenciaResponse;
import com.gombeth.urban.entity.GestionIncidencia;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.GestionIncidenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
public class GestionIncidenciaController {

    private final GestionIncidenciaService service;

    private final AccesoComunidadService
            accesoComunidadService;

    public GestionIncidenciaController(
            GestionIncidenciaService service,
            AccesoComunidadService accesoComunidadService
    ) {
        this.service = service;
        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Lista únicamente las incidencias de una comunidad
     * accesible por el usuario autenticado.
     */
    @GetMapping("/comunidad/{comunidadId}")
    public List<GestionIncidenciaResponse> listar(
            @PathVariable Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        return service.listarPorComunidad(
                comunidadId
        );
    }

    /**
     * Comprueba la comunidad de la incidencia antes de
     * devolver sus datos.
     */
    @GetMapping("/{id}")
    public GestionIncidenciaResponse obtener(
            @PathVariable Long id,
            Authentication authentication
    ) {
        GestionIncidenciaResponse incidencia =
                service.obtener(id);

        validarAccesoIncidencia(
                authentication,
                incidencia
        );

        return incidencia;
    }

    /**
     * Impide crear una incidencia interna dentro de una
     * comunidad ajena.
     */
    @PostMapping
    public GestionIncidenciaResponse crear(
            @RequestBody GestionIncidencia incidencia,
            Authentication authentication
    ) {
        Long comunidadId =
                obtenerComunidadIdParaAlta(
                        incidencia
                );

        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        return service.guardar(
                incidencia
        );
    }

    /**
     * Valida la comunidad de la incidencia existente.
     *
     * No se permite trasladar una incidencia a otra comunidad
     * modificando el cuerpo JSON.
     */
    @PutMapping("/{id}")
    public GestionIncidenciaResponse actualizar(
            @PathVariable Long id,
            @RequestBody GestionIncidencia incidencia,
            Authentication authentication
    ) {
        if (incidencia == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se han recibido los datos de la incidencia."
            );
        }

        GestionIncidenciaResponse existente =
                service.obtener(id);

        Long comunidadIdExistente =
                obtenerComunidadId(
                        existente
                );

        accesoComunidadService.validarAcceso(
                authentication,
                comunidadIdExistente
        );

        if (incidencia.getComunidad() != null) {

            Long comunidadIdRecibida =
                    incidencia
                            .getComunidad()
                            .getId();

            if (comunidadIdRecibida == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La comunidad indicada no es válida."
                );
            }

            if (
                    !comunidadIdExistente.equals(
                            comunidadIdRecibida
                    )
            ) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No se permite trasladar una incidencia "
                                + "a otra comunidad."
                );
            }
        }

        /*
         * GestionIncidenciaService solo modifica la comunidad
         * cuando el valor recibido no es nulo.
         *
         * La dejamos a null para conservar siempre la comunidad
         * original almacenada en la base de datos.
         */
        incidencia.setComunidad(null);

        return service.actualizar(
                id,
                incidencia
        );
    }

    private void validarAccesoIncidencia(
            Authentication authentication,
            GestionIncidenciaResponse incidencia
    ) {
        Long comunidadId =
                obtenerComunidadId(
                        incidencia
                );

        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );
    }

    private Long obtenerComunidadIdParaAlta(
            GestionIncidencia incidencia
    ) {
        if (
                incidencia == null
                        || incidencia.getComunidad() == null
                        || incidencia
                        .getComunidad()
                        .getId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad de la incidencia "
                            + "es obligatoria."
            );
        }

        return incidencia
                .getComunidad()
                .getId();
    }

    private Long obtenerComunidadId(
            GestionIncidenciaResponse incidencia
    ) {
        if (
                incidencia == null
                        || incidencia.getComunidad() == null
                        || incidencia
                        .getComunidad()
                        .getId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La incidencia no tiene una comunidad "
                            + "asociada correctamente."
            );
        }

        return incidencia
                .getComunidad()
                .getId();
    }
}