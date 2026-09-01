package com.gombeth.urban.service;

import com.gombeth.urban.dto.UsuarioAdministracionAltaRequest;
import com.gombeth.urban.dto.UsuarioAdministracionResponse;
import com.gombeth.urban.entity.Administrador;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Rol;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.entity.UsuarioComunidad;
import com.gombeth.urban.repository.AdministradorRepository;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.RolRepository;
import com.gombeth.urban.repository.UsuarioComunidadRepository;
import com.gombeth.urban.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAdministracionAltaServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdministradorRepository administradorRepository;

    @Mock
    private ComunidadRepository comunidadRepository;

    @Mock
    private UsuarioComunidadRepository
            usuarioComunidadRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private AccesoComunidadService accesoComunidadService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private Authentication authentication;

    @Mock
    private Usuario usuarioAutenticado;

    @Mock
    private Usuario usuarioGuardado;

    @Mock
    private Administrador administrador;

    @Mock
    private Rol rol;

    @Mock
    private Comunidad comunidadCompartida;

    private UsuarioAdministracionService service;

    @BeforeEach
    void configurar() {

        service =
                new UsuarioAdministracionService(
                        usuarioRepository,
                        administradorRepository,
                        comunidadRepository,
                        usuarioComunidadRepository,
                        rolRepository,
                        accesoComunidadService,
                        passwordEncoder,
                        passwordPolicyService
                );
    }

    @Test
    void rechazaAdministradorDistinto() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionAltaRequest request =
                crearPeticionValida();

        request.setAdministradorId(
                99L
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.crearUsuario(
                                        authentication,
                                        request
                                )
                );

        assertEquals(
                HttpStatus.FORBIDDEN.value(),
                exception.getStatusCode().value()
        );

        verify(
                usuarioRepository,
                never()
        ).save(
                any(Usuario.class)
        );
    }

    @Test
    void rechazaUsernameDuplicado() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionAltaRequest request =
                crearPeticionValida();

        when(
                usuarioRepository.existsByUsername(
                        "Nuevo"
                )
        ).thenReturn(
                true
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.crearUsuario(
                                        authentication,
                                        request
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT.value(),
                exception.getStatusCode().value()
        );

        verify(
                usuarioRepository,
                never()
        ).save(
                any(Usuario.class)
        );
    }

    @Test
    void rechazaPasswordInvalida() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionAltaRequest request =
                crearPeticionValida();

        request.setPasswordInicial(
                "debil"
        );

        when(
                passwordPolicyService.validar(
                        "debil"
                )
        ).thenReturn(
                "La contraseña debe tener "
                        + "al menos 8 caracteres."
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.crearUsuario(
                                        authentication,
                                        request
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        verify(
                passwordEncoder,
                never()
        ).encode(
                any()
        );

        verify(
                usuarioRepository,
                never()
        ).save(
                any(Usuario.class)
        );
    }

    @Test
    void rechazaRolInexistente() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionAltaRequest request =
                crearPeticionValida();

        request.setRolIds(
                List.of(
                        1L,
                        99L
                )
        );

        when(
                rolRepository.findAllById(
                        List.of(
                                1L,
                                99L
                        )
                )
        ).thenReturn(
                List.of(rol)
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.crearUsuario(
                                        authentication,
                                        request
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        verify(
                usuarioRepository,
                never()
        ).save(
                any(Usuario.class)
        );
    }

    @Test
    void rechazaComunidadDeOtroAdministrador() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionAltaRequest request =
                crearPeticionValida();

        when(
                rolRepository.findAllById(
                        List.of(1L)
                )
        ).thenReturn(
                List.of(rol)
        );

        when(
                comunidadRepository.findAllById(
                        List.of(11L)
                )
        ).thenReturn(
                List.of(comunidadCompartida)
        );

        when(
                comunidadCompartida.getAdministradorId()
        ).thenReturn(
                99L
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.crearUsuario(
                                        authentication,
                                        request
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        verify(
                usuarioRepository,
                never()
        ).save(
                any(Usuario.class)
        );
    }

    @Test
    void creaUsuarioCorrectamenteConPasswordCifrada() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionAltaRequest request =
                crearPeticionValida();

        prepararAltaCorrecta();

        UsuarioAdministracionResponse resultado =
                service.crearUsuario(
                        authentication,
                        request
                );

        assertNotNull(
                resultado
        );

        assertEquals(
                20L,
                resultado.usuarioId()
        );

        assertEquals(
                "Nuevo",
                resultado.username()
        );

        ArgumentCaptor<Usuario> usuarioCaptor =
                ArgumentCaptor.forClass(
                        Usuario.class
                );

        verify(
                usuarioRepository
        ).save(
                usuarioCaptor.capture()
        );

        Usuario usuarioCreado =
                usuarioCaptor.getValue();

        assertEquals(
                "Nuevo",
                usuarioCreado.getUsername()
        );

        assertEquals(
                "$2a$10$hashNuevo",
                usuarioCreado.getPassword()
        );

        assertEquals(
                7L,
                usuarioCreado.getAdministradorId()
        );

        assertEquals(
                1,
                usuarioCreado.getRoles().size()
        );

        assertTrue(
                usuarioCreado.getRoles().contains(
                        rol
                )
        );

        verify(
                passwordEncoder
        ).encode(
                "Nueva2026"
        );

        ArgumentCaptor<UsuarioComunidad>
                asignacionCaptor =
                ArgumentCaptor.forClass(
                        UsuarioComunidad.class
                );

        verify(
                usuarioComunidadRepository
        ).save(
                asignacionCaptor.capture()
        );

        assertEquals(
                20L,
                asignacionCaptor
                        .getValue()
                        .getUsuarioId()
        );

        assertEquals(
                11L,
                asignacionCaptor
                        .getValue()
                        .getComunidadId()
        );
    }

    private void prepararUsuarioAutenticado() {

        when(
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuarioAutenticado
        );

        when(
                usuarioAutenticado.getAdministradorId()
        ).thenReturn(
                7L
        );
    }

    private UsuarioAdministracionAltaRequest
    crearPeticionValida() {

        UsuarioAdministracionAltaRequest request =
                new UsuarioAdministracionAltaRequest();

        request.setUsername(
                " Nuevo "
        );

        request.setPasswordInicial(
                "Nueva2026"
        );

        request.setAdministradorId(
                7L
        );

        request.setRolIds(
                List.of(1L)
        );

        request.setComunidadCompartidaIds(
                List.of(11L)
        );

        return request;
    }

    private void prepararAltaCorrecta() {

        when(
                passwordEncoder.encode(
                        "Nueva2026"
                )
        ).thenReturn(
                "$2a$10$hashNuevo"
        );

        when(
                rolRepository.findAllById(
                        List.of(1L)
                )
        ).thenReturn(
                List.of(rol)
        );

        when(
                rol.getId()
        ).thenReturn(
                1L
        );

        when(
                rol.getNombre()
        ).thenReturn(
                "USER"
        );

        when(
                comunidadRepository.findAllById(
                        List.of(11L)
                )
        ).thenReturn(
                List.of(comunidadCompartida)
        );

        when(
                comunidadCompartida.getId()
        ).thenReturn(
                11L
        );

        when(
                comunidadCompartida.getNombre()
        ).thenReturn(
                "Comunidad compartida"
        );

        when(
                comunidadCompartida.getAdministradorId()
        ).thenReturn(
                7L
        );

        when(
                usuarioRepository.save(
                        any(Usuario.class)
                )
        ).thenReturn(
                usuarioGuardado
        );

        when(
                usuarioGuardado.getId()
        ).thenReturn(
                20L
        );

        when(
                usuarioGuardado.getUsername()
        ).thenReturn(
                "Nuevo"
        );

        when(
                usuarioGuardado.getAdministradorId()
        ).thenReturn(
                7L
        );

        when(
                usuarioGuardado.getRoles()
        ).thenReturn(
                Set.of(rol)
        );

        when(
                administradorRepository.findById(
                        7L
                )
        ).thenReturn(
                Optional.of(administrador)
        );

        when(
                administrador.getId()
        ).thenReturn(
                7L
        );

        when(
                administrador.getNombre()
        ).thenReturn(
                "Administrador de prueba"
        );

        when(
                comunidadRepository
                        .findByUsuarioIdOrderByNombreAsc(
                                20L
                        )
        ).thenReturn(
                List.of()
        );

        when(
                usuarioComunidadRepository
                        .findByUsuarioId(
                                20L
                        )
        ).thenReturn(
                List.of(
                        new UsuarioComunidad(
                                20L,
                                11L
                        )
                )
        );
    }
}