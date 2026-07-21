package com.gombeth.urban.controller;

import com.gombeth.urban.dto.LoginRequest;
import com.gombeth.urban.dto.LoginResponse;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final CsrfTokenRepository csrfTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;

    private final SecurityContextHolderStrategy
            securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            CsrfTokenRepository csrfTokenRepository,
            UsuarioRepository usuarioRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.csrfTokenRepository = csrfTokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Genera y devuelve el token CSRF que Angular enviará
     * en las peticiones POST, PUT, PATCH y DELETE.
     */
    @GetMapping("/csrf")
    public CsrfToken csrf(
            CsrfToken csrfToken
    ) {
        return csrfToken;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        String username = request.getUsername() == null
                ? ""
                : request.getUsername().trim();

        String password = request.getPassword() == null
                ? ""
                : request.getPassword();

        if (username.isBlank() || password.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new LoginResponse(
                            false,
                            null,
                            null,
                            null,
                            null,
                            "Debe indicar usuario y contraseña."
                    ));
        }

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            UsernamePasswordAuthenticationToken
                                    .unauthenticated(
                                            username,
                                            password
                                    )
                    );

            /*
             * Cambia el identificador de sesión para evitar
             * ataques de fijación de sesión.
             */
            sessionAuthenticationStrategy.onAuthentication(
                    authentication,
                    httpRequest,
                    httpResponse
            );

            SecurityContext securityContext =
                    securityContextHolderStrategy
                            .createEmptyContext();

            securityContext.setAuthentication(
                    authentication
            );

            securityContextHolderStrategy.setContext(
                    securityContext
            );

            /*
             * Spring Security 6 exige guardar explícitamente
             * el contexto cuando el login se hace desde
             * un controlador propio.
             */
            securityContextRepository.saveContext(
                    securityContext,
                    httpRequest,
                    httpResponse
            );

            /*
             * Emite un nuevo token CSRF después del login.
             */
            CsrfToken nuevoCsrfToken =
                    csrfTokenRepository.generateToken(
                            httpRequest
                    );

            csrfTokenRepository.saveToken(
                    nuevoCsrfToken,
                    httpRequest,
                    httpResponse
            );

            Usuario usuario = usuarioRepository
                    .findByUsername(
                            authentication.getName()
                    )
                    .orElseThrow();

            return ResponseEntity.ok(
                    crearRespuestaUsuario(
                            usuario,
                            "Login correcto"
                    )
            );

        } catch (AuthenticationException exception) {
            securityContextHolderStrategy.clearContext();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(
                            false,
                            null,
                            null,
                            null,
                            null,
                            "Usuario o contraseña incorrectos."
                    ));
        }
    }

    /**
     * Permite comprobar que la sesión sigue existiendo
     * y devuelve el usuario autenticado por el servidor.
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(
            Authentication authentication
    ) {

        if (
                authentication == null ||
                        !authentication.isAuthenticated()
        ) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(
                            false,
                            null,
                            null,
                            null,
                            null,
                            "Sesión no iniciada o caducada."
                    ));
        }

        Usuario usuario = usuarioRepository
                .findByUsername(
                        authentication.getName()
                )
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(
                            false,
                            null,
                            null,
                            null,
                            null,
                            "El usuario de la sesión ya no existe."
                    ));
        }

        return ResponseEntity.ok(
                crearRespuestaUsuario(
                        usuario,
                        "Sesión válida"
                )
        );
    }

    private LoginResponse crearRespuestaUsuario(
            Usuario usuario,
            String mensaje
    ) {

        String administradorNombre =
                obtenerNombreAdministrador(
                        usuario.getAdministradorId()
                );

        return new LoginResponse(
                true,
                usuario.getId(),
                usuario.getUsername(),
                usuario.getAdministradorId(),
                administradorNombre,
                mensaje
        );
    }

    private String obtenerNombreAdministrador(
            Long administradorId
    ) {

        if (administradorId == null) {
            return null;
        }

        List<String> nombres = jdbcTemplate.query(
                "SELECT nombre FROM administradores WHERE id = ?",
                (resultSet, rowNum) ->
                        resultSet.getString("nombre"),
                administradorId
        );

        return nombres.isEmpty()
                ? null
                : nombres.getFirst();
    }
}