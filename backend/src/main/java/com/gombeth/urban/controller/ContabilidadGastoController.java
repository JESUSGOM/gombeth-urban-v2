package com.gombeth.urban.controller;

import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ContabilidadAutomaticaService;
import com.gombeth.urban.service.ContabilidadGastoService;
import com.gombeth.urban.service.CuentaContableService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/gastos")
public class ContabilidadGastoController {

    private final ContabilidadGastoService
            gastoService;

    private final ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    private final CuentaContableService
            cuentaContableService;

    private final AccesoComunidadService
            accesoComunidadService;

    public ContabilidadGastoController(
            ContabilidadGastoService gastoService,
            ContabilidadAutomaticaService
                    contabilidadAutomaticaService,
            CuentaContableService cuentaContableService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.gastoService = gastoService;

        this.contabilidadAutomaticaService =
                contabilidadAutomaticaService;

        this.cuentaContableService =
                cuentaContableService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Contabiliza un gasto solamente después de comprobar
     * la comunidad a la que pertenece.
     */
    @PostMapping("/{id}/contabilizar")
    public ContabilidadGasto contabilizar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ContabilidadGasto gasto =
                obtenerGasto(id);

        accesoComunidadService.validarAcceso(
                authentication,
                gasto.getComunidadId()
        );

        validarCuentaGasto(
                gasto.getCuentaGastoId(),
                gasto.getComunidadId(),
                true
        );

        try {
            contabilidadAutomaticaService
                    .contabilizarGasto(id);

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );
        }

        return obtenerGasto(id);
    }

    /**
     * Lista únicamente los gastos de una comunidad accesible
     * por el usuario de la sesión.
     */
    @GetMapping
    public List<ContabilidadGasto> listar(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        return gastoService.listarPorComunidad(
                comunidadId
        );
    }

    /**
     * Crea un gasto únicamente dentro de una comunidad
     * accesible por el usuario autenticado.
     *
     * Una operación de alta no puede recibir un
     * identificador, porque podría provocar la modificación
     * de un gasto existente mediante repository.save(...).
     *
     * Cuando se indica una cuenta de gasto, también se
     * comprueba que pertenezca a la misma comunidad.
     */
    @PostMapping
    public ContabilidadGasto crear(
            @RequestBody ContabilidadGasto gasto,
            Authentication authentication
    ) {
        if (gasto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del gasto son obligatorios."
            );
        }

        if (gasto.getId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se permite indicar un identificador "
                            + "al crear un gasto."
            );
        }

        if (gasto.getComunidadId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad del gasto es obligatoria."
            );
        }

        accesoComunidadService.validarAcceso(
                authentication,
                gasto.getComunidadId()
        );

        validarCuentaGasto(
                gasto.getCuentaGastoId(),
                gasto.getComunidadId(),
                false
        );

        try {
            return gastoService.crear(
                    gasto
            );

        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );
        }
    }

    private ContabilidadGasto obtenerGasto(
            Long gastoId
    ) {
        try {
            return gastoService.findById(
                    gastoId
            );

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    error.getMessage()
            );
        }
    }

    private void validarCuentaGasto(
            Long cuentaGastoId,
            Long comunidadId,
            boolean obligatoria
    ) {
        if (cuentaGastoId == null) {

            if (obligatoria) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El gasto no tiene una cuenta "
                                + "de gasto asociada."
                );
            }

            return;
        }

        boolean pertenece =
                cuentaContableService
                        .findByComunidad(comunidadId)
                        .stream()
                        .anyMatch(
                                cuenta ->
                                        Objects.equals(
                                                cuenta.getId(),
                                                cuentaGastoId
                                        )
                        );

        if (!pertenece) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cuenta de gasto no pertenece "
                            + "a la comunidad indicada."
            );
        }
    }
}