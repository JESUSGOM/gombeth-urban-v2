package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.QrCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ComunidadControllerTest {

    @Test
    void crearAsignaUsuarioYAdministradorDeLaSesion() {

        ComunidadRepository comunidadRepository =
                mock(ComunidadRepository.class);

        VecinoRepository vecinoRepository =
                mock(VecinoRepository.class);

        QrCodeService qrCodeService =
                mock(QrCodeService.class);

        AccesoComunidadService accesoComunidadService =
                mock(AccesoComunidadService.class);

        Authentication authentication =
                mock(Authentication.class);

        Usuario usuario =
                mock(Usuario.class);

        when(usuario.getId())
                .thenReturn(4L);

        when(usuario.getAdministradorId())
                .thenReturn(2L);

        when(
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(usuario);

        when(
                comunidadRepository.save(
                        any(Comunidad.class)
                )
        ).thenAnswer(invocacion -> {

            Comunidad comunidad =
                    invocacion.getArgument(0);

            comunidad.setId(101L);

            return comunidad;
        });

        ComunidadController controller =
                new ComunidadController(
                        comunidadRepository,
                        vecinoRepository,
                        qrCodeService,
                        accesoComunidadService,
                        "https://jfgb.es/incidenciacomunidad/"
                );

        Comunidad datosRecibidos =
                new Comunidad();

        datosRecibidos.setNombre(
                " Comunidad creada en prueba "
        );

        datosRecibidos.setUsuarioId(
                999L
        );

        datosRecibidos.setAdministradorId(
                888L
        );

        datosRecibidos.setPaiscod(
                null
        );

        datosRecibidos.setSufijo(
                null
        );

        ResponseEntity<Comunidad> respuesta =
                controller.crear(
                        datosRecibidos,
                        authentication
                );

        assertEquals(
                HttpStatus.CREATED,
                respuesta.getStatusCode()
        );

        Comunidad comunidadCreada =
                respuesta.getBody();

        assertNotNull(
                comunidadCreada
        );

        assertNotSame(
                datosRecibidos,
                comunidadCreada
        );

        assertEquals(
                101L,
                comunidadCreada.getId()
        );

        assertEquals(
                "Comunidad creada en prueba",
                comunidadCreada.getNombre()
        );

        assertEquals(
                4L,
                comunidadCreada.getUsuarioId()
        );

        assertEquals(
                2L,
                comunidadCreada.getAdministradorId()
        );

        assertEquals(
                "ES",
                comunidadCreada.getPaiscod()
        );

        assertEquals(
                "000",
                comunidadCreada.getSufijo()
        );
    }

    @Test
    void crearRechazaIdentificadorEnviadoPorElCliente() {

        ComunidadRepository comunidadRepository =
                mock(ComunidadRepository.class);

        VecinoRepository vecinoRepository =
                mock(VecinoRepository.class);

        QrCodeService qrCodeService =
                mock(QrCodeService.class);

        AccesoComunidadService accesoComunidadService =
                mock(AccesoComunidadService.class);

        Authentication authentication =
                mock(Authentication.class);

        ComunidadController controller =
                new ComunidadController(
                        comunidadRepository,
                        vecinoRepository,
                        qrCodeService,
                        accesoComunidadService,
                        "https://jfgb.es/incidenciacomunidad/"
                );

        Comunidad datosRecibidos =
                new Comunidad();

        datosRecibidos.setId(
                999999L
        );

        datosRecibidos.setNombre(
                "No debe crearse"
        );

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.crear(
                                datosRecibidos,
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(
                accesoComunidadService,
                comunidadRepository
        );
    }
}
