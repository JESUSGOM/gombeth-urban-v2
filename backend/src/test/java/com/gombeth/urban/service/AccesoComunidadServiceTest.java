package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.UsuarioComunidadRepository;
import com.gombeth.urban.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccesoComunidadServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ComunidadRepository comunidadRepository;

    @Mock
    private UsuarioComunidadRepository
            usuarioComunidadRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private Usuario usuario;

    private AccesoComunidadService service;

    @BeforeEach
    void configurar() {

        service =
                new AccesoComunidadService(
                        usuarioRepository,
                        comunidadRepository,
                        usuarioComunidadRepository
                );
    }

    @Test
    void rechazaPeticionSinAutenticacion() {

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service
                                .obtenerUsuarioAutenticado(
                                        null
                                )
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(
                usuarioRepository,
                comunidadRepository,
                usuarioComunidadRepository
        );
    }

    @Test
    void permiteAccesoPorAsignacionDirecta() {

        prepararUsuarioAutenticado(
                4L
        );

        Comunidad comunidad =
                crearComunidad(
                        18L,
                        4L
                );

        when(
                comunidadRepository.findById(
                        18L
                )
        ).thenReturn(
                Optional.of(comunidad)
        );

        when(
                usuarioComunidadRepository
                        .existsByUsuarioIdAndComunidadId(
                                4L,
                                18L
                        )
        ).thenReturn(false);

        Comunidad resultado =
                service.obtenerComunidadAutorizada(
                        authentication,
                        18L
                );

        assertSame(
                comunidad,
                resultado
        );
    }

    @Test
    void permiteAccesoPorAsignacionCompartida() {

        prepararUsuarioAutenticado(
                4L
        );

        Comunidad comunidad =
                crearComunidad(
                        33L,
                        99L
                );

        when(
                comunidadRepository.findById(
                        33L
                )
        ).thenReturn(
                Optional.of(comunidad)
        );

        when(
                usuarioComunidadRepository
                        .existsByUsuarioIdAndComunidadId(
                                4L,
                                33L
                        )
        ).thenReturn(true);

        Comunidad resultado =
                service.obtenerComunidadAutorizada(
                        authentication,
                        33L
                );

        assertSame(
                comunidad,
                resultado
        );
    }

    @Test
    void rechazaAccesoCruzadoAOtraComunidad() {

        prepararUsuarioAutenticado(
                4L
        );

        Comunidad comunidadAjena =
                crearComunidad(
                        20L,
                        99L
                );

        when(
                comunidadRepository.findById(
                        20L
                )
        ).thenReturn(
                Optional.of(comunidadAjena)
        );

        when(
                usuarioComunidadRepository
                        .existsByUsuarioIdAndComunidadId(
                                4L,
                                20L
                        )
        ).thenReturn(false);

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service
                                .obtenerComunidadAutorizada(
                                        authentication,
                                        20L
                                )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                excepcion.getStatusCode()
        );
    }

    @Test
    void devuelveNoEncontradaCuandoLaComunidadNoExiste() {

        when(
                authentication.isAuthenticated()
        ).thenReturn(true);

        when(
                authentication.getName()
        ).thenReturn("Probador");

        when(
                usuarioRepository.findByUsername(
                        "Probador"
                )
        ).thenReturn(
                Optional.of(usuario)
        );

        when(
                comunidadRepository.findById(
                        999999L
                )
        ).thenReturn(
                Optional.empty()
        );

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service
                                .obtenerComunidadAutorizada(
                                        authentication,
                                        999999L
                                )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                excepcion.getStatusCode()
        );
    }

    private void prepararUsuarioAutenticado(
            Long usuarioId
    ) {

        when(
                authentication.isAuthenticated()
        ).thenReturn(true);

        when(
                authentication.getName()
        ).thenReturn("Probador");

        when(
                usuarioRepository.findByUsername(
                        "Probador"
                )
        ).thenReturn(
                Optional.of(usuario)
        );

        when(
                usuario.getId()
        ).thenReturn(usuarioId);
    }

    private Comunidad crearComunidad(
            Long comunidadId,
            Long usuarioId
    ) {

        Comunidad comunidad =
                new Comunidad();

        comunidad.setId(
                comunidadId
        );

        comunidad.setNombre(
                "Comunidad " + comunidadId
        );

        comunidad.setUsuarioId(
                usuarioId
        );

        return comunidad;
    }
}
