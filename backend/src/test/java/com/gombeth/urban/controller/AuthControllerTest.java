package com.gombeth.urban.controller;

import com.gombeth.urban.dto.LoginRequest;
import com.gombeth.urban.dto.LoginResponse;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private SessionAuthenticationStrategy
            sessionAuthenticationStrategy;

    @Mock
    private CsrfTokenRepository csrfTokenRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @Mock
    private Authentication authentication;

    @Mock
    private CsrfToken csrfToken;

    @Mock
    private Usuario usuario;

    private AuthController controller;

    @BeforeEach
    void configurar() {

        controller =
                new AuthController(
                        authenticationManager,
                        securityContextRepository,
                        sessionAuthenticationStrategy,
                        csrfTokenRepository,
                        usuarioRepository,
                        jdbcTemplate
                );
    }

    @AfterEach
    void limpiarContextoSeguridad() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void rechazaCamposDeLoginVacios() {

        LoginRequest request =
                new LoginRequest();

        request.setUsername(
                "   "
        );

        request.setPassword(
                ""
        );

        ResponseEntity<LoginResponse> respuesta =
                controller.login(
                        request,
                        httpRequest,
                        httpResponse
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                respuesta.getStatusCode()
        );

        LoginResponse cuerpo =
                respuesta.getBody();

        assertNotNull(
                cuerpo
        );

        assertEquals(
                false,
                cuerpo.ok()
        );

        assertEquals(
                "Debe indicar usuario y contraseña.",
                cuerpo.mensaje()
        );

        verifyNoInteractions(
                authenticationManager,
                securityContextRepository,
                sessionAuthenticationStrategy,
                csrfTokenRepository,
                usuarioRepository,
                jdbcTemplate
        );
    }

    @Test
    void rechazaCredencialesIncorrectas() {

        LoginRequest request =
                crearPeticionLogin();

        when(
                authenticationManager.authenticate(
                        any(Authentication.class)
                )
        ).thenThrow(
                new BadCredentialsException(
                        "Credenciales incorrectas"
                )
        );

        ResponseEntity<LoginResponse> respuesta =
                controller.login(
                        request,
                        httpRequest,
                        httpResponse
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                respuesta.getStatusCode()
        );

        LoginResponse cuerpo =
                respuesta.getBody();

        assertNotNull(
                cuerpo
        );

        assertEquals(
                false,
                cuerpo.ok()
        );

        assertEquals(
                "Usuario o contraseña incorrectos.",
                cuerpo.mensaje()
        );

        verifyNoInteractions(
                securityContextRepository,
                sessionAuthenticationStrategy,
                csrfTokenRepository,
                usuarioRepository,
                jdbcTemplate
        );
    }

    @Test
    void aceptaCredencialesCorrectasYGuardaLaSesion() {

        LoginRequest request =
                crearPeticionLogin();

        when(
                authenticationManager.authenticate(
                        any(Authentication.class)
                )
        ).thenReturn(
                authentication
        );

        when(
                authentication.getName()
        ).thenReturn(
                "Probador"
        );

        when(
                csrfTokenRepository.generateToken(
                        httpRequest
                )
        ).thenReturn(
                csrfToken
        );

        when(
                usuarioRepository.findByUsername(
                        "Probador"
                )
        ).thenReturn(
                Optional.of(usuario)
        );

        when(
                usuario.getId()
        ).thenReturn(
                4L
        );

        when(
                usuario.getUsername()
        ).thenReturn(
                "Probador"
        );

        when(
                usuario.getAdministradorId()
        ).thenReturn(
                null
        );

        ResponseEntity<LoginResponse> respuesta =
                controller.login(
                        request,
                        httpRequest,
                        httpResponse
                );

        assertEquals(
                HttpStatus.OK,
                respuesta.getStatusCode()
        );

        LoginResponse cuerpo =
                respuesta.getBody();

        assertNotNull(
                cuerpo
        );

        assertEquals(
                true,
                cuerpo.ok()
        );

        assertEquals(
                4L,
                cuerpo.usuarioId()
        );

        assertEquals(
                "Probador",
                cuerpo.username()
        );

        assertEquals(
                "Login correcto",
                cuerpo.mensaje()
        );

        verify(
                sessionAuthenticationStrategy
        ).onAuthentication(
                same(authentication),
                same(httpRequest),
                same(httpResponse)
        );

        verify(
                securityContextRepository
        ).saveContext(
                any(SecurityContext.class),
                same(httpRequest),
                same(httpResponse)
        );

        verify(
                csrfTokenRepository
        ).saveToken(
                same(csrfToken),
                same(httpRequest),
                same(httpResponse)
        );

        verifyNoInteractions(
                jdbcTemplate
        );
    }

    private LoginRequest crearPeticionLogin() {

        LoginRequest request =
                new LoginRequest();

        request.setUsername(
                " Probador "
        );

        request.setPassword(
                "Probador123"
        );

        return request;
    }
}
