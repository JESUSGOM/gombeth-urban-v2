package com.gombeth.urban.service;

import com.gombeth.urban.dto.ComunidadNombreResponse;
import com.gombeth.urban.dto.RolResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Rol;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.RolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAdministracionCatalogoServiceTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private ComunidadRepository comunidadRepository;

    @Mock
    private AccesoComunidadService accesoComunidadService;

    @Mock
    private Authentication authentication;

    @Mock
    private Usuario usuarioAutenticado;

    @Mock
    private Rol rolAdmin;

    @Mock
    private Rol rolUser;

    @Mock
    private Comunidad comunidadA;

    @Mock
    private Comunidad comunidadB;

    private UsuarioAdministracionCatalogoService service;

    @BeforeEach
    void configurar() {

        service =
                new UsuarioAdministracionCatalogoService(
                        rolRepository,
                        comunidadRepository,
                        accesoComunidadService
                );
    }

    @Test
    void listaRolesOrdenados() {

        prepararUsuarioAutenticado();

        when(
                rolRepository.findAll()
        ).thenReturn(
                List.of(
                        rolUser,
                        rolAdmin
                )
        );

        when(
                rolAdmin.getId()
        ).thenReturn(
                1L
        );

        when(
                rolAdmin.getNombre()
        ).thenReturn(
                "ADMIN"
        );

        when(
                rolUser.getId()
        ).thenReturn(
                2L
        );

        when(
                rolUser.getNombre()
        ).thenReturn(
                "USER"
        );

        List<RolResponse> resultado =
                service.listarRolesDisponibles(
                        authentication
                );

        assertEquals(
                2,
                resultado.size()
        );

        assertEquals(
                "ADMIN",
                resultado.getFirst().nombre()
        );

        assertEquals(
                "USER",
                resultado.get(1).nombre()
        );
    }

    @Test
    void listaSoloComunidadesDelAdministradorAutenticado() {

        prepararUsuarioAutenticado();

        when(
                comunidadRepository
                        .findByAdministradorIdOrderByNombreAsc(
                                7L
                        )
        ).thenReturn(
                List.of(
                        comunidadA,
                        comunidadB
                )
        );

        when(
                comunidadA.getId()
        ).thenReturn(
                10L
        );

        when(
                comunidadA.getNombre()
        ).thenReturn(
                "Comunidad A"
        );

        when(
                comunidadB.getId()
        ).thenReturn(
                11L
        );

        when(
                comunidadB.getNombre()
        ).thenReturn(
                "Comunidad B"
        );

        List<ComunidadNombreResponse> resultado =
                service.listarComunidadesAsignables(
                        authentication
                );

        assertEquals(
                2,
                resultado.size()
        );

        assertEquals(
                10L,
                resultado.getFirst().comunidadId()
        );

        assertEquals(
                "Comunidad A",
                resultado.getFirst().nombreComunidad()
        );

        verify(
                comunidadRepository
        ).findByAdministradorIdOrderByNombreAsc(
                7L
        );
    }

    @Test
    void rechazaCatalogosSinAdministradorAsociado() {

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
                null
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                service.listarComunidadesAsignables(
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.FORBIDDEN.value(),
                exception.getStatusCode().value()
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
}