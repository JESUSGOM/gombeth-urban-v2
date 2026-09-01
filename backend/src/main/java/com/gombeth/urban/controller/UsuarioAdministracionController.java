package com.gombeth.urban.controller;

import com.gombeth.urban.dto.UsuarioAdministracionAltaRequest;
import com.gombeth.urban.dto.UsuarioAdministracionEdicionRequest;
import com.gombeth.urban.dto.UsuarioAdministracionResponse;
import com.gombeth.urban.service.UsuarioAdministracionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioAdministracionController {

    private final UsuarioAdministracionService
            usuarioAdministracionService;

    public UsuarioAdministracionController(
            UsuarioAdministracionService
                    usuarioAdministracionService
    ) {
        this.usuarioAdministracionService =
                usuarioAdministracionService;
    }

    @GetMapping
    public List<UsuarioAdministracionResponse>
    listarUsuarios(
            Authentication authentication
    ) {
        return usuarioAdministracionService
                .listarUsuarios(
                        authentication
                );
    }

    @GetMapping("/{usuarioId}")
    public UsuarioAdministracionResponse
    obtenerUsuario(
            Authentication authentication,
            @PathVariable Long usuarioId
    ) {
        return usuarioAdministracionService
                .obtenerUsuario(
                        authentication,
                        usuarioId
                );
    }

    @PostMapping
    public ResponseEntity<UsuarioAdministracionResponse>
    crearUsuario(
            Authentication authentication,
            @RequestBody(required = false)
            UsuarioAdministracionAltaRequest request
    ) {

        UsuarioAdministracionResponse response =
                usuarioAdministracionService
                        .crearUsuario(
                                authentication,
                                request
                        );

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        response
                );
    }

    @PutMapping("/{usuarioId}")
    public UsuarioAdministracionResponse
    editarUsuario(
            Authentication authentication,
            @PathVariable Long usuarioId,
            @RequestBody(required = false)
            UsuarioAdministracionEdicionRequest request
    ) {
        return usuarioAdministracionService
                .editarUsuario(
                        authentication,
                        usuarioId,
                        request
                );
    }
}