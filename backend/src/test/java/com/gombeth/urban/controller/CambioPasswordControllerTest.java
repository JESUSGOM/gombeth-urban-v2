package com.gombeth.urban.controller;

import com.gombeth.urban.dto.CambioPasswordRequest;
import com.gombeth.urban.dto.CambioPasswordResponse;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.UsuarioRepository;
import com.gombeth.urban.service.PasswordPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambioPasswordControllerTest {

    @Mock
    private AuthenticationManager
            authenticationManager;

    @Mock
    private UsuarioRepository
            usuarioRepository;

    @Mock
    private PasswordEncoder
            passwordEncoder;

    @Mock
    private PasswordPolicyService
            passwordPolicyService;

    @Mock
    private Authentication
            authentication;

    @Mock
    private Usuario usuario;

    private CambioPasswordController controller;

    @BeforeEach
    void configurar() {

        controller =
                new CambioPasswordController(
                        authenticationManager,
                        usuarioRepository,
                        passwordEncoder,
                        passwordPolicyService
                );
    }

    @Test
    void rechazaCamposIncompletos() {

        CambioPasswordRequest request =
                new CambioPasswordRequest();

        request.setUsername(
                "Probador"
        );

        ResponseEntity<CambioPasswordResponse>
                respuesta =
                controller.cambiarPassword(
                        request
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                respuesta.getStatusCode()
        );

        assertNotNull(
                respuesta.getBody()
        );

        assertEquals(
                false,
                respuesta.getBody().ok()
        );

        verifyNoInteractions(
                authenticationManager,
                usuarioRepository,
                passwordEncoder,
                passwordPolicyService
        );
    }

    @Test
    void rechazaConfirmacionDistinta() {

        CambioPasswordRequest request =
                crearPeticionValida();

        request.setConfirmarPassword(
                "Otra2026"
        );

        ResponseEntity<CambioPasswordResponse>
                respuesta =
                controller.cambiarPassword(
                        request
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                respuesta.getStatusCode()
        );

        assertEquals(
                "La nueva contraseña y su "
                        + "confirmación no coinciden.",
                respuesta.getBody().mensaje()
        );

        verifyNoInteractions(
                authenticationManager,
                usuarioRepository,
                passwordEncoder,
                passwordPolicyService
        );
    }

    @Test
    void rechazaPasswordDebil() {

        CambioPasswordRequest request =
                crearPeticionValida();

        request.setNuevaPassword(
                "sololetras"
        );

        request.setConfirmarPassword(
                "sololetras"
        );

        when(
                passwordPolicyService.validar(
                        "sololetras"
                )
        ).thenReturn(
                "La contraseña debe contener "
                        + "mayúsculas, minúsculas y números."
        );

        ResponseEntity<CambioPasswordResponse>
                respuesta =
                controller.cambiarPassword(
                        request
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                respuesta.getStatusCode()
        );

        verifyNoInteractions(
                authenticationManager,
                usuarioRepository,
                passwordEncoder
        );
    }

    @Test
    void rechazaPasswordActualIncorrecta() {

        CambioPasswordRequest request =
                crearPeticionValida();

        when(
                passwordPolicyService.validar(
                        "Nueva2026"
                )
        ).thenReturn(
                null
        );

        when(
                authenticationManager.authenticate(
                        any(Authentication.class)
                )
        ).thenThrow(
                new BadCredentialsException(
                        "Credenciales incorrectas"
                )
        );

        ResponseEntity<CambioPasswordResponse>
                respuesta =
                controller.cambiarPassword(
                        request
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                respuesta.getStatusCode()
        );

        assertEquals(
                "Usuario o contraseña actual incorrectos.",
                respuesta.getBody().mensaje()
        );

        verifyNoInteractions(
                usuarioRepository,
                passwordEncoder
        );
    }

    @Test
    void rechazaReutilizarLaPasswordActual() {

        CambioPasswordRequest request =
                crearPeticionValida();

        prepararUsuarioAutenticado();

        when(
                passwordPolicyService.validar(
                        "Nueva2026"
                )
        ).thenReturn(
                null
        );

        when(
                usuario.getPassword()
        ).thenReturn(
                "$2a$10$hashActual"
        );

        when(
                passwordEncoder.matches(
                        "Nueva2026",
                        "$2a$10$hashActual"
                )
        ).thenReturn(
                true
        );

        ResponseEntity<CambioPasswordResponse>
                respuesta =
                controller.cambiarPassword(
                        request
                );

        assertEquals(
                HttpStatus.CONFLICT,
                respuesta.getStatusCode()
        );

        verify(
                usuarioRepository,
                never()
        ).save(
                any(Usuario.class)
        );
    }

    @Test
    void cambiaPasswordCorrectamente() {

        CambioPasswordRequest request =
                crearPeticionValida();

        prepararUsuarioAutenticado();

        when(
                passwordPolicyService.validar(
                        "Nueva2026"
                )
        ).thenReturn(
                null
        );

        when(
                usuario.getPassword()
        ).thenReturn(
                "$2a$10$hashActual"
        );

        when(
                passwordEncoder.matches(
                        "Nueva2026",
                        "$2a$10$hashActual"
                )
        ).thenReturn(
                false
        );

        when(
                passwordEncoder.encode(
                        "Nueva2026"
                )
        ).thenReturn(
                "$2a$10$hashNuevo"
        );

        ResponseEntity<CambioPasswordResponse>
                respuesta =
                controller.cambiarPassword(
                        request
                );

        assertEquals(
                HttpStatus.OK,
                respuesta.getStatusCode()
        );

        assertNotNull(
                respuesta.getBody()
        );

        assertEquals(
                true,
                respuesta.getBody().ok()
        );

        verify(usuario).setPassword(
                "$2a$10$hashNuevo"
        );

        verify(usuarioRepository).save(
                usuario
        );
    }

    private void prepararUsuarioAutenticado() {

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
                usuarioRepository.findByUsername(
                        "Probador"
                )
        ).thenReturn(
                Optional.of(usuario)
        );
    }

    private CambioPasswordRequest
    crearPeticionValida() {

        CambioPasswordRequest request =
                new CambioPasswordRequest();

        request.setUsername(
                "Probador"
        );

        request.setPasswordActual(
                "Sepa2026"
        );

        request.setNuevaPassword(
                "Nueva2026"
        );

        request.setConfirmarPassword(
                "Nueva2026"
        );

        return request;
    }
}