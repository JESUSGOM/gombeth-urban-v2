package com.gombeth.urban.controller;

import com.gombeth.urban.dto.ComunidadNombreResponse;
import com.gombeth.urban.dto.RolResponse;
import com.gombeth.urban.service.UsuarioAdministracionCatalogoService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/catalogos")
public class UsuarioAdministracionCatalogoController {

    private final UsuarioAdministracionCatalogoService
            usuarioAdministracionCatalogoService;

    public UsuarioAdministracionCatalogoController(
            UsuarioAdministracionCatalogoService
                    usuarioAdministracionCatalogoService
    ) {
        this.usuarioAdministracionCatalogoService =
                usuarioAdministracionCatalogoService;
    }

    @GetMapping("/roles")
    public List<RolResponse> listarRoles(
            Authentication authentication
    ) {
        return usuarioAdministracionCatalogoService
                .listarRolesDisponibles(
                        authentication
                );
    }

    @GetMapping("/comunidades")
    public List<ComunidadNombreResponse>
    listarComunidades(
            Authentication authentication
    ) {
        return usuarioAdministracionCatalogoService
                .listarComunidadesAsignables(
                        authentication
                );
    }
}