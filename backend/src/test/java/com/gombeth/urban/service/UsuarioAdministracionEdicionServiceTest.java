package com.gombeth.urban.service;

import com.gombeth.urban.dto.UsuarioAdministracionEdicionRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAdministracionEdicionServiceTest {

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
    private Usuario usuarioGestionado;

    @Mock
    private Usuario usuarioDuplicado;

    @Mock
    private Administrador administrador;

    @Mock
    private Rol rol;

    @Mock
    private Comunidad comunidad12;

    @Mock
    private Comunidad comunidad13;

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
    void rechazaUsuarioDeOtroAdministrador() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionEdicionRequest request =
                crearPeticionValida();

        when(
                usuarioRepository.findById(
                        20L
                )
        ).thenReturn(
                Optional.of(usuarioGestionado)
        );

        when(
                usuarioGestionado.getAdministradorId()
        ).thenReturn(
                99L
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.editarUsuario(
                                        authentication,
                                        20L,
                                        request
                                )
                );

        assertEquals(
                HttpStatus.NOT_FOUND.value(),
                exception.getStatusCode().value()
        );

        verify(
                usuarioRepository,
                never()
        ).save(
                any(Usuario.class)
        );

        verifyNoInteractions(
                passwordEncoder,
                passwordPolicyService
        );
    }

    @Test
    void rechazaUsernameUtilizadoPorOtroUsuario() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionEdicionRequest request =
                crearPeticionValida();

        when(
                usuarioRepository.findById(
                        20L
                )
        ).thenReturn(
                Optional.of(usuarioGestionado)
        );

        when(
                usuarioGestionado.getAdministradorId()
        ).thenReturn(
                7L
        );

        when(
                usuarioGestionado.getId()
        ).thenReturn(
                20L
        );

        when(
                usuarioRepository.findByUsername(
                        "NuevoNombre"
                )
        ).thenReturn(
                Optional.of(usuarioDuplicado)
        );

        when(
                usuarioDuplicado.getId()
        ).thenReturn(
                30L
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.editarUsuario(
                                        authentication,
                                        20L,
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

        verifyNoInteractions(
                passwordEncoder,
                passwordPolicyService
        );
    }

    @Test
    void rechazaComunidadDeOtroAdministrador() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionEdicionRequest request =
                crearPeticionValida();

        when(
                usuarioRepository.findById(
                        20L
                )
        ).thenReturn(
                Optional.of(usuarioGestionado)
        );

        when(
                usuarioGestionado.getAdministradorId()
        ).thenReturn(
                7L
        );

        when(
                usuarioRepository.findByUsername(
                        "NuevoNombre"
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                rolRepository.findAllById(
                        List.of(1L)
                )
        ).thenReturn(
                List.of(rol)
        );

        when(
                comunidadRepository.findAllById(
                        List.of(
                                12L,
                                13L
                        )
                )
        ).thenReturn(
                List.of(
                        comunidad12,
                        comunidad13
                )
        );

        when(
                comunidad12.getAdministradorId()
        ).thenReturn(
                7L
        );

        when(
                comunidad13.getAdministradorId()
        ).thenReturn(
                99L
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.editarUsuario(
                                        authentication,
                                        20L,
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

        verifyNoInteractions(
                passwordEncoder,
                passwordPolicyService
        );
    }

    @Test
    void editaSinModificarPasswordYSinRecrearAsignacionesIguales() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionEdicionRequest request =
                crearPeticionValida();

        prepararEdicionComun();

        when(
                usuarioComunidadRepository
                        .findByUsuarioId(
                                20L
                        )
        ).thenReturn(
                List.of(
                        new UsuarioComunidad(
                                20L,
                                12L
                        ),
                        new UsuarioComunidad(
                                20L,
                                13L
                        )
                ),
                List.of(
                        new UsuarioComunidad(
                                20L,
                                12L
                        ),
                        new UsuarioComunidad(
                                20L,
                                13L
                        )
                )
        );

        UsuarioAdministracionResponse resultado =
                service.editarUsuario(
                        authentication,
                        20L,
                        request
                );

        assertNotNull(
                resultado
        );

        assertEquals(
                "NuevoNombre",
                resultado.username()
        );

        verify(
                usuarioGestionado
        ).setUsername(
                "NuevoNombre"
        );

        verify(
                usuarioGestionado,
                never()
        ).setPassword(
                any()
        );

        verify(
                usuarioComunidadRepository,
                never()
        ).deleteByUsuarioIdAndComunidadId(
                any(),
                any()
        );

        verify(
                usuarioComunidadRepository,
                never()
        ).save(
                any(UsuarioComunidad.class)
        );

        verifyNoInteractions(
                passwordEncoder,
                passwordPolicyService
        );
    }

    @Test
    void sincronizaSoloDiferenciasDeComunidadesCompartidas() {

        prepararUsuarioAutenticado();

        UsuarioAdministracionEdicionRequest request =
                crearPeticionValida();

        prepararEdicionComun();

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
                        ),
                        new UsuarioComunidad(
                                20L,
                                12L
                        )
                ),
                List.of(
                        new UsuarioComunidad(
                                20L,
                                12L
                        ),
                        new UsuarioComunidad(
                                20L,
                                13L
                        )
                )
        );

        UsuarioAdministracionResponse resultado =
                service.editarUsuario(
                        authentication,
                        20L,
                        request
                );

        assertNotNull(
                resultado
        );

        verify(
                usuarioComunidadRepository
        ).deleteByUsuarioIdAndComunidadId(
                20L,
                11L
        );

        ArgumentCaptor<UsuarioComunidad>
                asignacionCaptor =
                ArgumentCaptor.forClass(
                        UsuarioComunidad.class
                );

        verify(
                usuarioComunidadRepository,
                times(1)
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
                13L,
                asignacionCaptor
                        .getValue()
                        .getComunidadId()
        );

        verify(
                usuarioComunidadRepository,
                never()
        ).deleteByUsuarioIdAndComunidadId(
                20L,
                12L
        );

        verify(
                usuarioGestionado,
                never()
        ).setPassword(
                any()
        );

        verifyNoInteractions(
                passwordEncoder,
                passwordPolicyService
        );
    }

    @Test
    void rechazaQuitarAdminAlPropioUsuarioAutenticado() {

        prepararUsuarioAutenticado();

        when(
                usuarioAutenticado.getId()
        ).thenReturn(
                20L
        );

        UsuarioAdministracionEdicionRequest request =
                new UsuarioAdministracionEdicionRequest();

        request.setUsername(
                "admin1"
        );

        request.setAdministradorId(
                7L
        );

        /*
         * Se intenta dejar al propio usuario autenticado
         * sin el rol ADMIN.
         */
        request.setRolIds(
                List.of()
        );

        request.setComunidadCompartidaIds(
                List.of()
        );

        when(
                usuarioRepository.findById(
                        20L
                )
        ).thenReturn(
                Optional.of(usuarioGestionado)
        );

        when(
                usuarioGestionado.getId()
        ).thenReturn(
                20L
        );

        when(
                usuarioGestionado.getAdministradorId()
        ).thenReturn(
                7L
        );

        when(
                usuarioRepository.findByUsername(
                        "admin1"
                )
        ).thenReturn(
                Optional.of(usuarioGestionado)
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.editarUsuario(
                                        authentication,
                                        20L,
                                        request
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        assertEquals(
                "El usuario administrador autenticado "
                        + "no puede quitarse a sí mismo "
                        + "el rol ADMIN.",
                exception.getReason()
        );

        verify(
                usuarioRepository,
                never()
        ).save(
                any(Usuario.class)
        );

        verify(
                usuarioGestionado,
                never()
        ).setRoles(
                any()
        );

        verify(
                usuarioComunidadRepository,
                never()
        ).deleteByUsuarioIdAndComunidadId(
                any(),
                any()
        );

        verify(
                usuarioComunidadRepository,
                never()
        ).save(
                any(UsuarioComunidad.class)
        );

        verifyNoInteractions(
                passwordEncoder,
                passwordPolicyService
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

    private UsuarioAdministracionEdicionRequest
    crearPeticionValida() {

        UsuarioAdministracionEdicionRequest request =
                new UsuarioAdministracionEdicionRequest();

        request.setUsername(
                " NuevoNombre "
        );

        request.setAdministradorId(
                7L
        );

        request.setRolIds(
                List.of(1L)
        );

        request.setComunidadCompartidaIds(
                List.of(
                        12L,
                        13L
                )
        );

        return request;
    }

    private void prepararEdicionComun() {

        when(
                usuarioRepository.findById(
                        20L
                )
        ).thenReturn(
                Optional.of(usuarioGestionado)
        );

        when(
                usuarioGestionado.getId()
        ).thenReturn(
                20L
        );

        when(
                usuarioGestionado.getUsername()
        ).thenReturn(
                "NuevoNombre"
        );

        when(
                usuarioGestionado.getAdministradorId()
        ).thenReturn(
                7L
        );

        when(
                usuarioRepository.findByUsername(
                        "NuevoNombre"
                )
        ).thenReturn(
                Optional.of(usuarioGestionado)
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
                        List.of(
                                12L,
                                13L
                        )
                )
        ).thenReturn(
                List.of(
                        comunidad12,
                        comunidad13
                )
        );

        when(
                comunidad12.getId()
        ).thenReturn(
                12L
        );

        when(
                comunidad12.getNombre()
        ).thenReturn(
                "Comunidad 12"
        );

        when(
                comunidad12.getAdministradorId()
        ).thenReturn(
                7L
        );

        when(
                comunidad13.getId()
        ).thenReturn(
                13L
        );

        when(
                comunidad13.getNombre()
        ).thenReturn(
                "Comunidad 13"
        );

        when(
                comunidad13.getAdministradorId()
        ).thenReturn(
                7L
        );

        when(
                usuarioGestionado.getRoles()
        ).thenReturn(
                Set.of(rol)
        );

        when(
                usuarioRepository.save(
                        usuarioGestionado
                )
        ).thenReturn(
                usuarioGestionado
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
    }
}