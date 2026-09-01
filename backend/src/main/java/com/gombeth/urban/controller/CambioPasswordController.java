package com.gombeth.urban.controller;

import com.gombeth.urban.dto.CambioPasswordRequest;
import com.gombeth.urban.dto.CambioPasswordResponse;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.UsuarioRepository;
import com.gombeth.urban.service.PasswordPolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CambioPasswordController {

    private final AuthenticationManager
            authenticationManager;

    private final UsuarioRepository
            usuarioRepository;

    private final PasswordEncoder
            passwordEncoder;

    private final PasswordPolicyService
            passwordPolicyService;

    public CambioPasswordController(
            AuthenticationManager authenticationManager,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService
    ) {
        this.authenticationManager =
                authenticationManager;

        this.usuarioRepository =
                usuarioRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.passwordPolicyService =
                passwordPolicyService;
    }

    @PostMapping("/cambiar-password")
    @Transactional
    public ResponseEntity<CambioPasswordResponse>
    cambiarPassword(
            @RequestBody(required = false)
            CambioPasswordRequest request
    ) {

        if (request == null) {
            return respuestaError(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar los datos del cambio "
                            + "de contraseña."
            );
        }

        String username =
                normalizarUsername(
                        request.getUsername()
                );

        String passwordActual =
                valorOTextoVacio(
                        request.getPasswordActual()
                );

        String nuevaPassword =
                valorOTextoVacio(
                        request.getNuevaPassword()
                );

        String confirmarPassword =
                valorOTextoVacio(
                        request.getConfirmarPassword()
                );

        if (
                username.isBlank()
                        || passwordActual.isBlank()
                        || nuevaPassword.isBlank()
                        || confirmarPassword.isBlank()
        ) {
            return respuestaError(
                    HttpStatus.BAD_REQUEST,
                    "Debe completar todos los campos."
            );
        }

        if (!nuevaPassword.equals(
                confirmarPassword
        )) {
            return respuestaError(
                    HttpStatus.BAD_REQUEST,
                    "La nueva contraseña y su "
                            + "confirmación no coinciden."
            );
        }

        String errorSeguridad =
                passwordPolicyService.validar(
                        nuevaPassword
                );

        if (errorSeguridad != null) {
            return respuestaError(
                    HttpStatus.BAD_REQUEST,
                    errorSeguridad
            );
        }

        try {

            Authentication autenticacion =
                    authenticationManager.authenticate(
                            UsernamePasswordAuthenticationToken
                                    .unauthenticated(
                                            username,
                                            passwordActual
                                    )
                    );

            Usuario usuario =
                    usuarioRepository
                            .findByUsername(
                                    autenticacion.getName()
                            )
                            .orElse(null);

            if (usuario == null) {
                return respuestaError(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario o contraseña actual "
                                + "incorrectos."
                );
            }

            if (
                    passwordEncoder.matches(
                            nuevaPassword,
                            usuario.getPassword()
                    )
            ) {
                return respuestaError(
                        HttpStatus.CONFLICT,
                        "La nueva contraseña debe ser "
                                + "diferente de la actual."
                );
            }

            String nuevoHash =
                    passwordEncoder.encode(
                            nuevaPassword
                    );

            usuario.setPassword(
                    nuevoHash
            );

            usuarioRepository.save(
                    usuario
            );

            return ResponseEntity.ok(
                    new CambioPasswordResponse(
                            true,
                            "Contraseña cambiada correctamente. "
                                    + "Ya puede iniciar sesión "
                                    + "con la nueva contraseña."
                    )
            );

        } catch (AuthenticationException exception) {
            return respuestaError(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario o contraseña actual "
                            + "incorrectos."
            );
        }
    }

    private String normalizarUsername(
            String username
    ) {
        return username == null
                ? ""
                : username.trim();
    }

    private String valorOTextoVacio(
            String valor
    ) {
        return valor == null
                ? ""
                : valor;
    }

    private ResponseEntity<CambioPasswordResponse>
    respuestaError(
            HttpStatus estado,
            String mensaje
    ) {
        return ResponseEntity
                .status(estado)
                .body(
                        new CambioPasswordResponse(
                                false,
                                mensaje
                        )
                );
    }
}