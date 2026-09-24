package com.gombeth.urban.controller;

import com.gombeth.urban.dto.proveedor.ProveedorComunidadResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Proveedor;
import com.gombeth.urban.entity.ProveedorComunidad;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ProveedorService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorComunidadControllerTest {

    @Mock
    private ProveedorService
            proveedorService;

    @Mock
    private AccesoComunidadService
            accesoComunidadService;

    @Mock
    private Authentication
            authentication;

    private ProveedorComunidadController
            controller;

    @BeforeEach
    void setUp() {
        controller =
                new ProveedorComunidadController(
                        proveedorService,
                        accesoComunidadService
                );
    }

    @Test
    void listarValidaAccesoYDevuelveProveedorAsociado() {

        Comunidad comunidad =
                comunidad(
                        33L,
                        2L
                );

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Iberdrola Clientes, S.A.U.",
                        "B12345678",
                        true
                );

        ProveedorComunidad asociacion =
                asociacion(
                        100L,
                        10L,
                        33L,
                        373L,
                        true
                );

        when(
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                33L
                        )
        ).thenReturn(
                comunidad
        );

        when(
                proveedorService
                        .listarPorComunidad(
                                33L
                        )
        ).thenReturn(
                List.of(
                        asociacion
                )
        );

        when(
                proveedorService
                        .obtenerPorAdministrador(
                                2L,
                                10L
                        )
        ).thenReturn(
                proveedor
        );

        List<ProveedorComunidadResponse> resultado =
                controller.listar(
                        33L,
                        authentication
                );

        assertEquals(
                1,
                resultado.size()
        );

        ProveedorComunidadResponse respuesta =
                resultado.getFirst();

        assertEquals(
                100L,
                respuesta.asociacionId()
        );

        assertEquals(
                10L,
                respuesta.proveedorId()
        );

        assertEquals(
                "Iberdrola Clientes, S.A.U.",
                respuesta.nombre()
        );

        assertEquals(
                "B12345678",
                respuesta.nifCif()
        );

        assertEquals(
                373L,
                respuesta.cuentaContableId()
        );

        assertEquals(
                true,
                respuesta.proveedorActivo()
        );

        assertEquals(
                true,
                respuesta.asociacionActiva()
        );

        verify(
                accesoComunidadService
        ).obtenerComunidadAutorizada(
                authentication,
                33L
        );

        verify(
                proveedorService
        ).obtenerPorAdministrador(
                2L,
                10L
        );
    }

    @Test
    void asociarValidaComunidadYProveedorDelMismoAdministrador() {

        Comunidad comunidad =
                comunidad(
                        33L,
                        2L
                );

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Proveedor de prueba",
                        "B12345678",
                        true
                );

        ProveedorComunidad asociacion =
                asociacion(
                        100L,
                        10L,
                        33L,
                        373L,
                        true
                );

        when(
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                33L
                        )
        ).thenReturn(
                comunidad
        );

        when(
                proveedorService
                        .obtenerPorAdministrador(
                                2L,
                                10L
                        )
        ).thenReturn(
                proveedor
        );

        when(
                proveedorService
                        .asociarAComunidad(
                                10L,
                                33L
                        )
        ).thenReturn(
                asociacion
        );

        ProveedorComunidadResponse resultado =
                controller.asociar(
                        33L,
                        10L,
                        authentication
                );

        assertEquals(
                100L,
                resultado.asociacionId()
        );

        assertEquals(
                10L,
                resultado.proveedorId()
        );

        assertEquals(
                373L,
                resultado.cuentaContableId()
        );

        verify(
                accesoComunidadService
        ).obtenerComunidadAutorizada(
                authentication,
                33L
        );

        verify(
                proveedorService
        ).obtenerPorAdministrador(
                2L,
                10L
        );

        verify(
                proveedorService
        ).asociarAComunidad(
                10L,
                33L
        );
    }

    @Test
    void asociarProveedorDeOtroAdministradorDevuelve404() {

        Comunidad comunidad =
                comunidad(
                        33L,
                        2L
                );

        when(
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                33L
                        )
        ).thenReturn(
                comunidad
        );

        when(
                proveedorService
                        .obtenerPorAdministrador(
                                2L,
                                10L
                        )
        ).thenThrow(
                new IllegalArgumentException(
                        "El proveedor indicado no existe "
                                + "para este administrador."
                )
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.asociar(
                                        33L,
                                        10L,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        assertEquals(
                "Proveedor no encontrado.",
                error.getReason()
        );

        verify(
                proveedorService,
                never()
        ).asociarAComunidad(
                anyLong(),
                anyLong()
        );
    }

    @Test
    void comunidadSinAdministradorDevuelve409() {

        Comunidad comunidad =
                comunidad(
                        33L,
                        null
                );

        when(
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                33L
                        )
        ).thenReturn(
                comunidad
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.listar(
                                        33L,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        assertEquals(
                "La comunidad no tiene "
                        + "administrador asociado.",
                error.getReason()
        );

        verify(
                proveedorService,
                never()
        ).listarPorComunidad(
                anyLong()
        );
    }

    @Test
    void comunidadInvalidaDevuelve400AntesDeValidarAcceso() {

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.listar(
                                        0L,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        assertEquals(
                "Debe indicar una comunidad válida.",
                error.getReason()
        );

        verify(
                accesoComunidadService,
                never()
        ).obtenerComunidadAutorizada(
                authentication,
                0L
        );
    }

    @Test
    void proveedorInvalidoDevuelve400AntesDeConsultarProveedor() {

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.asociar(
                                        33L,
                                        0L,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        assertEquals(
                "Debe indicar un proveedor válido.",
                error.getReason()
        );

        verify(
                accesoComunidadService,
                never()
        ).obtenerComunidadAutorizada(
                authentication,
                33L
        );

        verify(
                proveedorService,
                never()
        ).obtenerPorAdministrador(
                anyLong(),
                anyLong()
        );
    }

    @Test
    void conflictoAlAsociarDevuelve409() {

        Comunidad comunidad =
                comunidad(
                        33L,
                        2L
                );

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Proveedor de prueba",
                        "B12345678",
                        true
                );

        when(
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                33L
                        )
        ).thenReturn(
                comunidad
        );

        when(
                proveedorService
                        .obtenerPorAdministrador(
                                2L,
                                10L
                        )
        ).thenReturn(
                proveedor
        );

        when(
                proveedorService
                        .asociarAComunidad(
                                10L,
                                33L
                        )
        ).thenThrow(
                new IllegalStateException(
                        "No se puede asociar el proveedor."
                )
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.asociar(
                                        33L,
                                        10L,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        assertEquals(
                "No se puede asociar el proveedor.",
                error.getReason()
        );
    }


    private Comunidad comunidad(
            Long id,
            Long administradorId
    ) {
        Comunidad comunidad =
                new Comunidad();

        comunidad.setId(
                id
        );

        comunidad.setAdministradorId(
                administradorId
        );

        return comunidad;
    }

    private Proveedor proveedor(
            Long id,
            Long administradorId,
            String nombre,
            String nifCif,
            boolean activo
    ) {
        Proveedor proveedor =
                new Proveedor();

        proveedor.setId(
                id
        );

        proveedor.setAdministradorId(
                administradorId
        );

        proveedor.setNombre(
                nombre
        );

        proveedor.setNifCif(
                nifCif
        );

        proveedor.setTelefono(
                "922111222"
        );

        proveedor.setEmail(
                "proveedor@prueba.es"
        );

        proveedor.setObservaciones(
                "Proveedor de prueba"
        );

        proveedor.setActivo(
                activo
        );

        return proveedor;
    }

    private ProveedorComunidad asociacion(
            Long id,
            Long proveedorId,
            Long comunidadId,
            Long cuentaContableId,
            boolean activo
    ) {
        ProveedorComunidad asociacion =
                new ProveedorComunidad();

        asociacion.setId(
                id
        );

        asociacion.setProveedorId(
                proveedorId
        );

        asociacion.setComunidadId(
                comunidadId
        );

        asociacion.setCuentaContableId(
                cuentaContableId
        );

        asociacion.setActivo(
                activo
        );

        return asociacion;
    }
}