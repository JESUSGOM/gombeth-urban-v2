package com.gombeth.urban.controller;

import com.gombeth.urban.dto.presentador.CuentaPresentadorRequest;
import com.gombeth.urban.dto.presentador.CuentaPresentadorResponse;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.CuentaPresentadorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/cuentas-presentador")
public class CuentaPresentadorController {

    private final CuentaPresentadorService
            cuentaPresentadorService;

    private final AccesoComunidadService
            accesoComunidadService;

    public CuentaPresentadorController(
            CuentaPresentadorService
                    cuentaPresentadorService,
            AccesoComunidadService
                    accesoComunidadService
    ) {
        this.cuentaPresentadorService =
                cuentaPresentadorService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @GetMapping
    public List<CuentaPresentadorResponse> listar(
            Authentication authentication
    ) {
        return cuentaPresentadorService.listar(
                obtenerAdministradorId(
                        authentication
                )
        );
    }

    @GetMapping("/activas")
    public List<CuentaPresentadorResponse> listarActivas(
            Authentication authentication
    ) {
        return cuentaPresentadorService.listarActivas(
                obtenerAdministradorId(
                        authentication
                )
        );
    }

    @GetMapping("/{id}")
    public CuentaPresentadorResponse obtener(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return cuentaPresentadorService.obtener(
                obtenerAdministradorId(
                        authentication
                ),
                id
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaPresentadorResponse crear(
            @RequestBody CuentaPresentadorRequest request,
            Authentication authentication
    ) {
        return cuentaPresentadorService.crear(
                obtenerAdministradorId(
                        authentication
                ),
                request
        );
    }

    @PutMapping("/{id}")
    public CuentaPresentadorResponse actualizar(
            @PathVariable Long id,
            @RequestBody CuentaPresentadorRequest request,
            Authentication authentication
    ) {
        return cuentaPresentadorService.actualizar(
                obtenerAdministradorId(
                        authentication
                ),
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        cuentaPresentadorService.eliminar(
                obtenerAdministradorId(
                        authentication
                ),
                id
        );
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
}