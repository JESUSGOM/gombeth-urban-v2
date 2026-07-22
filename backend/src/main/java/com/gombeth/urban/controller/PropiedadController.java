package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Propiedad;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.PropiedadRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/propiedades")
public class PropiedadController {

    private final PropiedadRepository
            propiedadRepository;

    private final VecinoRepository
            vecinoRepository;

    private final AccesoComunidadService
            accesoComunidadService;

    public PropiedadController(
            PropiedadRepository propiedadRepository,
            VecinoRepository vecinoRepository,
            AccesoComunidadService accesoComunidadService
    ) {
        this.propiedadRepository =
                propiedadRepository;

        this.vecinoRepository =
                vecinoRepository;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Lista las propiedades de un propietario después
     * de verificar que su comunidad es accesible
     * para el usuario autenticado.
     */
    @GetMapping("/vecino/{vecinoId}")
    public List<Propiedad> listarPorVecino(
            @PathVariable Long vecinoId,
            Authentication authentication
    ) {
        Vecino vecino =
                obtenerVecino(vecinoId);

        accesoComunidadService.validarAcceso(
                authentication,
                vecino.getComunidadId()
        );

        return propiedadRepository.findByVecinoId(
                vecinoId
        );
    }

    /**
     * Lista propiedades únicamente de una comunidad
     * accesible para el usuario autenticado.
     */
    @GetMapping("/comunidad/{comunidadId}")
    public List<Propiedad> listarPorComunidad(
            @PathVariable Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        return propiedadRepository.findByComunidadId(
                comunidadId
        );
    }

    /**
     * Obtiene una propiedad y comprueba el acceso
     * a su comunidad.
     */
    @GetMapping("/{id}")
    public Propiedad obtenerPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return obtenerPropiedadAutorizada(
                id,
                authentication
        );
    }

    /**
     * Crea una propiedad exclusivamente dentro
     * de una comunidad accesible.
     *
     * También comprueba que el propietario indicado
     * pertenece a la misma comunidad.
     */
    @PostMapping
    public Propiedad crear(
            @RequestBody Propiedad propiedad,
            Authentication authentication
    ) {
        if (
                propiedad == null
                        || propiedad.getComunidadId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad de la propiedad "
                            + "es obligatoria."
            );
        }

        if (propiedad.getVecinoId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El propietario es obligatorio."
            );
        }

        accesoComunidadService.validarAcceso(
                authentication,
                propiedad.getComunidadId()
        );

        validarVecinoPerteneceAComunidad(
                propiedad.getVecinoId(),
                propiedad.getComunidadId()
        );

        if (propiedad.getActivo() == null) {
            propiedad.setActivo(true);
        }

        if (
                propiedad.getTipo() == null
                        || propiedad.getTipo().isBlank()
        ) {
            propiedad.setTipo("VIVIENDA");
        }

        if (propiedad.getCoeficiente() == null) {
            propiedad.setCoeficiente(
                    BigDecimal.ZERO
            );
        }

        return propiedadRepository.save(
                propiedad
        );
    }

    /**
     * Actualiza una propiedad autorizada.
     *
     * La comunidad recibida dentro del cuerpo JSON
     * se ignora. La propiedad permanece siempre
     * en su comunidad original.
     */
    @PutMapping("/{id}")
    public Propiedad actualizar(
            @PathVariable Long id,
            @RequestBody Propiedad datos,
            Authentication authentication
    ) {
        if (datos == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos de la propiedad "
                            + "son obligatorios."
            );
        }

        Propiedad propiedad =
                obtenerPropiedadAutorizada(
                        id,
                        authentication
                );

        if (datos.getVecinoId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El propietario es obligatorio."
            );
        }

        /*
         * Se utiliza siempre la comunidad original.
         * No se acepta un cambio de comunidad enviado
         * mediante el cuerpo JSON.
         */
        Long comunidadIdOriginal =
                propiedad.getComunidadId();

        validarVecinoPerteneceAComunidad(
                datos.getVecinoId(),
                comunidadIdOriginal
        );

        propiedad.setVecinoId(
                datos.getVecinoId()
        );

        propiedad.setReferencia(
                datos.getReferencia()
        );

        propiedad.setTipo(
                datos.getTipo()
        );

        propiedad.setDireccion(
                datos.getDireccion()
        );

        propiedad.setCoeficiente(
                datos.getCoeficiente()
        );

        propiedad.setActivo(
                datos.getActivo()
        );

        propiedad.setNotas(
                datos.getNotas()
        );

        return propiedadRepository.save(
                propiedad
        );
    }

    /**
     * Da de baja una propiedad únicamente cuando
     * pertenece a una comunidad accesible.
     */
    @DeleteMapping("/{id}")
    public void darDeBaja(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Propiedad propiedad =
                obtenerPropiedadAutorizada(
                        id,
                        authentication
                );

        propiedad.setActivo(false);

        propiedadRepository.save(
                propiedad
        );
    }

    /**
     * Recupera una propiedad y valida el acceso
     * a la comunidad a la que pertenece.
     */
    private Propiedad obtenerPropiedadAutorizada(
            Long propiedadId,
            Authentication authentication
    ) {
        Propiedad propiedad =
                propiedadRepository
                        .findById(propiedadId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Propiedad no encontrada "
                                                + "con ID: "
                                                + propiedadId
                                )
                        );

        accesoComunidadService.validarAcceso(
                authentication,
                propiedad.getComunidadId()
        );

        return propiedad;
    }

    /**
     * Recupera un propietario o devuelve un error 404.
     */
    private Vecino obtenerVecino(
            Long vecinoId
    ) {
        return vecinoRepository
                .findById(vecinoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Propietario no encontrado "
                                        + "con ID: "
                                        + vecinoId
                        )
                );
    }

    /**
     * Impide asociar una propiedad con un propietario
     * perteneciente a otra comunidad.
     */
    private void validarVecinoPerteneceAComunidad(
            Long vecinoId,
            Long comunidadId
    ) {
        Vecino vecino =
                obtenerVecino(vecinoId);

        if (
                !Objects.equals(
                        vecino.getComunidadId(),
                        comunidadId
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El propietario indicado no pertenece "
                            + "a la comunidad de la propiedad."
            );
        }
    }
}