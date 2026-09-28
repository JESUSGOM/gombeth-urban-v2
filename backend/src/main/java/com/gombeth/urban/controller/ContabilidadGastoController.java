package com.gombeth.urban.controller;

import com.gombeth.urban.dto.GastoGuardarRequest;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ContabilidadAutomaticaService;
import com.gombeth.urban.service.ContabilidadGastoService;
import com.gombeth.urban.service.CuentaContableService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    /*
     * Las operaciones de pago y reversión permanecen
     * deshabilitadas por defecto. Solo se habilitan
     * mediante configuración explícita del backend.
     */
    @Value("${gombeth.gastos.pagos-habilitados:false}")
    private boolean pagosHabilitados;

    @Value("${gombeth.gastos.pdf-reemplazo-habilitado:false}")
    private boolean reemplazoPdfHabilitado;

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

    @GetMapping("/configuracion")
    public Map<String, Boolean> configuracion() {
        return Map.of(
                "pagosHabilitados",
                pagosHabilitados,
                "reemplazoPdfHabilitado",
                reemplazoPdfHabilitado
        );
    }

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

    @GetMapping("/{id}")
    public ContabilidadGasto obtener(
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

        return gasto;
    }

    @DeleteMapping("/{id}")
    public void eliminar(
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

        try {
            gastoService.eliminarPendiente(
                    id
            );

        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    error.getMessage()
            );
        }
    }

    @PostMapping
    public ContabilidadGasto crear(
            @RequestBody GastoGuardarRequest request,
            Authentication authentication
    ) {
        validarRequest(
                request
        );

        accesoComunidadService.validarAcceso(
                authentication,
                request.comunidadId()
        );

        validarCuentaGasto(
                request.cuentaGastoId(),
                request.comunidadId(),
                false
        );

        try {
            return gastoService.crear(
                    request
            );

        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );
        }
    }

    @PutMapping("/{id}")
    public ContabilidadGasto actualizar(
            @PathVariable Long id,
            @RequestBody GastoGuardarRequest request,
            Authentication authentication
    ) {
        validarRequest(
                request
        );

        ContabilidadGasto existente =
                obtenerGasto(
                        id
                );

        accesoComunidadService.validarAcceso(
                authentication,
                existente.getComunidadId()
        );

        if (
                !Objects.equals(
                        existente.getComunidadId(),
                        request.comunidadId()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se permite cambiar la comunidad "
                            + "de un gasto existente."
            );
        }

        validarCuentaGasto(
                request.cuentaGastoId(),
                existente.getComunidadId(),
                false
        );

        try {
            return gastoService.actualizar(
                    id,
                    request
            );

        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    error.getMessage()
            );
        }
    }

    @PostMapping("/{id}/contabilizar")
    public ContabilidadGasto contabilizar(
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

        validarCuentaGasto(
                gasto.getCuentaGastoId(),
                gasto.getComunidadId(),
                true
        );

        try {
            contabilidadAutomaticaService
                    .contabilizarGasto(
                            id
                    );

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );
        }

        return obtenerGasto(
                id
        );
    }

    @PostMapping("/{id}/pagar")
    public ContabilidadGasto pagar(
            @PathVariable Long id,
            @RequestParam(required = false)
            LocalDate fechaPago,
            Authentication authentication
    ) {
        ContabilidadGasto gasto =
                obtenerGasto(
                        id
                );

        /*
         * Primero comprobamos que el usuario puede acceder
         * a la comunidad. No queremos revelar información
         * sobre un gasto a un usuario no autorizado.
         */
        accesoComunidadService.validarAcceso(
                authentication,
                gasto.getComunidadId()
        );

        /*
         * Esta operación solo está disponible cuando
         * la configuración de pagos está habilitada.
         */
        validarPagosHabilitados();

        Usuario usuario =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        try {
            return gastoService.pagar(
                    id,
                    usuario.getId(),
                    fechaPago
            );

        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );
        }
    }

    @PostMapping("/{id}/deshacer-pago")
    public ContabilidadGasto deshacerPago(
            @PathVariable Long id,
            @RequestParam(required = false)
            LocalDate fechaAnulacion,
            Authentication authentication
    ) {
        ContabilidadGasto gasto =
                obtenerGasto(
                        id
                );

        /*
         * Igual que en pagar(), validamos primero el acceso
         * del usuario a la comunidad.
         */
        accesoComunidadService.validarAcceso(
                authentication,
                gasto.getComunidadId()
        );

        /*
         * Esta operación solo está disponible cuando
         * la configuración de pagos está habilitada.
         */
        validarPagosHabilitados();

        Usuario usuario =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        try {
            return gastoService.deshacerPago(
                    id,
                    usuario.getId(),
                    fechaAnulacion
            );

        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );
        }
    }

    @PostMapping("/{id}/deshacer-contabilizacion")
    public ContabilidadGasto deshacerContabilizacion(
            @PathVariable Long id,
            @RequestParam(required = false)
            LocalDate fechaAnulacion,
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

        /*
         * Esta operación solo está disponible cuando
         * la configuración de pagos está habilitada.
         */
        validarPagosHabilitados();

        Usuario usuario =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        try {
            return gastoService
                    .deshacerContabilizacion(
                            id,
                            usuario.getId(),
                            fechaAnulacion
                    );

        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );

        } catch (IllegalStateException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    error.getMessage()
            );
        }
    }

    private void validarRequest(
            GastoGuardarRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del gasto son obligatorios."
            );
        }

        if (request.comunidadId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad del gasto es obligatoria."
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
                        .findByComunidad(
                                comunidadId
                        )
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

    private void validarPagosHabilitados() {
        if (!pagosHabilitados) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Las operaciones de pago y reversión de gastos "
                            + "están deshabilitadas por configuración."
            );
        }
    }
}