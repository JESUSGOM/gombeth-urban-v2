package com.gombeth.urban.controller;

import com.gombeth.urban.dto.proveedor.ProveedorRequest;
import com.gombeth.urban.dto.proveedor.ProveedorResponse;
import com.gombeth.urban.entity.Proveedor;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ProveedorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    private final AccesoComunidadService
            accesoComunidadService;

    public ProveedorController(
            ProveedorService proveedorService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.proveedorService =
                proveedorService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @GetMapping
    public List<ProveedorResponse> listar(
            Authentication authentication
    ) {
        Long administradorId =
                obtenerAdministradorId(
                        authentication
                );

        return proveedorService
                .listarPorAdministrador(
                        administradorId
                )
                .stream()
                .map(this::convertir)
                .toList();
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(
            @PathVariable Long id,
            Authentication authentication
    ) {
        validarProveedorId(
                id
        );

        Long administradorId =
                obtenerAdministradorId(
                        authentication
                );

        try {
            return convertir(
                    proveedorService
                            .obtenerPorAdministrador(
                                    administradorId,
                                    id
                            )
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Proveedor no encontrado."
            );
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorResponse crear(
            @RequestBody ProveedorRequest request,
            Authentication authentication
    ) {
        validarRequest(
                request
        );

        Long administradorId =
                obtenerAdministradorId(
                        authentication
                );

        try {
            Proveedor proveedor =
                    proveedorService
                            .crearProveedor(
                                    administradorId,
                                    request.getNombre(),
                                    request.getNifCif(),
                                    request.getTelefono(),
                                    request.getEmail(),
                                    request.getObservaciones()
                            );

            return convertir(
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

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(
            @PathVariable Long id,
            @RequestBody ProveedorRequest request,
            Authentication authentication
    ) {
        validarProveedorId(
                id
        );

        validarRequest(
                request
        );

        Long administradorId =
                obtenerAdministradorId(
                        authentication
                );

        try {
            Proveedor proveedor =
                    proveedorService
                            .actualizarProveedor(
                                    administradorId,
                                    id,
                                    request.getNombre(),
                                    request.getNifCif(),
                                    request.getTelefono(),
                                    request.getEmail(),
                                    request.getObservaciones(),
                                    request.getActivo()
                            );

            return convertir(
                    proveedor
            );

        } catch (IllegalArgumentException ex) {
            /*
             * El administrador procede de la sesión y
             * el nombre ya se ha validado en este
             * controlador. Por tanto, aquí una
             * IllegalArgumentException significa que
             * el proveedor no existe para ese
             * administrador.
             */
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Proveedor no encontrado."
            );

        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    ex.getMessage()
            );
        }
    }

    private Long obtenerAdministradorId(
            Authentication authentication
    ) {
        Usuario usuario =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        Long administradorId =
                usuario.getAdministradorId();

        if (
                administradorId == null
                        || administradorId <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario autenticado no tiene "
                            + "administrador asociado."
            );
        }

        return administradorId;
    }

    private void validarProveedorId(
            Long proveedorId
    ) {
        if (
                proveedorId == null
                        || proveedorId <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar un proveedor válido."
            );
        }
    }

    private void validarRequest(
            ProveedorRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del proveedor "
                            + "no pueden estar vacíos."
            );
        }

        if (
                request.getNombre() == null
                        || request.getNombre().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre del proveedor "
                            + "es obligatorio."
            );
        }

        validarLongitud(
                request.getNombre(),
                255,
                "El nombre"
        );

        validarLongitud(
                request.getNifCif(),
                20,
                "El NIF o CIF"
        );

        validarLongitud(
                request.getTelefono(),
                50,
                "El teléfono"
        );

        validarLongitud(
                request.getEmail(),
                255,
                "El correo electrónico"
        );

        validarLongitud(
                request.getObservaciones(),
                1000,
                "Las observaciones"
        );
    }

    private void validarLongitud(
            String valor,
            int longitudMaxima,
            String campo
    ) {
        if (
                valor != null
                        && valor.trim().length()
                        > longitudMaxima
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    campo
                            + " no puede superar "
                            + longitudMaxima
                            + " caracteres."
            );
        }
    }

    private ProveedorResponse convertir(
            Proveedor proveedor
    ) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getNifCif(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getObservaciones(),
                proveedor.getActivo()
        );
    }
}
