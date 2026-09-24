package com.gombeth.urban.controller;

import com.gombeth.urban.dto.proveedor.ProveedorComunidadResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Proveedor;
import com.gombeth.urban.entity.ProveedorComunidad;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ProveedorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(
        "/api/comunidades/{comunidadId}/proveedores"
)
public class ProveedorComunidadController {

    private final ProveedorService
            proveedorService;

    private final AccesoComunidadService
            accesoComunidadService;



    public ProveedorComunidadController(
            ProveedorService proveedorService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.proveedorService =
                proveedorService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @GetMapping
    public List<ProveedorComunidadResponse> listar(
            @PathVariable Long comunidadId,
            Authentication authentication
    ) {
        validarId(
                comunidadId,
                "Debe indicar una comunidad válida."
        );

        Comunidad comunidad =
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                comunidadId
                        );

        Long administradorId =
                obtenerAdministradorId(
                        comunidad
                );

        return proveedorService
                .listarPorComunidad(
                        comunidadId
                )
                .stream()
                .map(
                        asociacion -> {
                            Proveedor proveedor =
                                    obtenerProveedor(
                                            administradorId,
                                            asociacion
                                                    .getProveedorId()
                                    );

                            return convertir(
                                    asociacion,
                                    proveedor
                            );
                        }
                )
                .toList();
    }

    @PostMapping("/{proveedorId}")
    public ProveedorComunidadResponse asociar(
            @PathVariable Long comunidadId,
            @PathVariable Long proveedorId,
            Authentication authentication
    ) {
        validarId(
                comunidadId,
                "Debe indicar una comunidad válida."
        );

        validarId(
                proveedorId,
                "Debe indicar un proveedor válido."
        );

        Comunidad comunidad =
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                comunidadId
                        );

        Long administradorId =
                obtenerAdministradorId(
                        comunidad
                );

        /*
         * Antes de asociar comprobamos que el proveedor
         * pertenece al mismo administrador que la
         * comunidad. De esta forma tampoco revelamos
         * proveedores pertenecientes a otro administrador.
         */
        Proveedor proveedor =
                obtenerProveedor(
                        administradorId,
                        proveedorId
                );

        try {
            ProveedorComunidad asociacion =
                    proveedorService
                            .asociarAComunidad(
                                    proveedorId,
                                    comunidadId
                            );

            return convertir(
                    asociacion,
                    proveedor
            );

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage()
            );

        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    ex.getMessage()
            );
        }
    }

    private Proveedor obtenerProveedor(
            Long administradorId,
            Long proveedorId
    ) {
        try {
            return proveedorService
                    .obtenerPorAdministrador(
                            administradorId,
                            proveedorId
                    );

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Proveedor no encontrado."
            );
        }
    }

    private Long obtenerAdministradorId(
            Comunidad comunidad
    ) {
        Long administradorId =
                comunidad.getAdministradorId();

        if (
                administradorId == null
                        || administradorId <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La comunidad no tiene "
                            + "administrador asociado."
            );
        }

        return administradorId;
    }

    private void validarId(
            Long id,
            String mensaje
    ) {
        if (
                id == null
                        || id <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    mensaje
            );
        }
    }

    private ProveedorComunidadResponse convertir(
            ProveedorComunidad asociacion,
            Proveedor proveedor
    ) {
        return new ProveedorComunidadResponse(
                asociacion.getId(),
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getNifCif(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getObservaciones(),
                asociacion.getCuentaContableId(),
                proveedor.getActivo(),
                asociacion.getActivo()
        );
    }
}