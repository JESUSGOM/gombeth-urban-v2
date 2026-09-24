package com.gombeth.urban.controller;

import com.gombeth.urban.dto.proveedor.ProveedorRequest;
import com.gombeth.urban.dto.proveedor.ProveedorResponse;
import com.gombeth.urban.entity.Proveedor;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ProveedorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorControllerTest {

    @Mock
    private ProveedorService
            proveedorService;

    @Mock
    private AccesoComunidadService
            accesoComunidadService;

    @Mock
    private Authentication
            authentication;

    private ProveedorController
            controller;

    @BeforeEach
    void setUp() {
        controller =
                new ProveedorController(
                        proveedorService,
                        accesoComunidadService
                );
    }

    @Test
    void listarUsaAdministradorDelUsuarioAutenticado() {

        Usuario usuario =
                usuario(
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
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuario
        );

        when(
                proveedorService
                        .listarPorAdministrador(
                                2L
                        )
        ).thenReturn(
                List.of(
                        proveedor
                )
        );

        List<ProveedorResponse> resultado =
                controller.listar(
                        authentication
                );

        assertEquals(
                1,
                resultado.size()
        );

        assertEquals(
                10L,
                resultado.getFirst().id()
        );

        assertEquals(
                "Proveedor de prueba",
                resultado.getFirst().nombre()
        );

        verify(
                proveedorService
        ).listarPorAdministrador(
                2L
        );
    }

    @Test
    void obtieneProveedorDelAdministradorAutenticado() {

        Usuario usuario =
                usuario(
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
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuario
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

        ProveedorResponse resultado =
                controller.obtener(
                        10L,
                        authentication
                );

        assertEquals(
                10L,
                resultado.id()
        );

        assertEquals(
                "B12345678",
                resultado.nifCif()
        );

        verify(
                proveedorService
        ).obtenerPorAdministrador(
                2L,
                10L
        );
    }

    @Test
    void obtenerProveedorDeOtroAdministradorDevuelve404() {

        Usuario usuario =
                usuario(
                        2L
                );

        when(
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuario
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
                                controller.obtener(
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
    }

    @Test
    void crearUsaAdministradorDelUsuarioAutenticado() {

        Usuario usuario =
                usuario(
                        2L
                );

        ProveedorRequest request =
                requestValida();

        Proveedor creado =
                proveedor(
                        25L,
                        2L,
                        "Proveedor nuevo",
                        "B12345678",
                        true
                );

        creado.setTelefono(
                "922111222"
        );

        creado.setEmail(
                "proveedor@prueba.es"
        );

        creado.setObservaciones(
                "Proveedor de prueba"
        );

        when(
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuario
        );

        when(
                proveedorService.crearProveedor(
                        2L,
                        "Proveedor nuevo",
                        "B12345678",
                        "922111222",
                        "proveedor@prueba.es",
                        "Proveedor de prueba"
                )
        ).thenReturn(
                creado
        );

        ProveedorResponse resultado =
                controller.crear(
                        request,
                        authentication
                );

        assertEquals(
                25L,
                resultado.id()
        );

        assertEquals(
                "Proveedor nuevo",
                resultado.nombre()
        );

        assertEquals(
                "B12345678",
                resultado.nifCif()
        );

        verify(
                proveedorService
        ).crearProveedor(
                2L,
                "Proveedor nuevo",
                "B12345678",
                "922111222",
                "proveedor@prueba.es",
                "Proveedor de prueba"
        );
    }

    @Test
    void crearEstaMarcadoComoHttp201()
            throws NoSuchMethodException {

        ResponseStatus responseStatus =
                ProveedorController.class
                        .getMethod(
                                "crear",
                                ProveedorRequest.class,
                                Authentication.class
                        )
                        .getAnnotation(
                                ResponseStatus.class
                        );

        assertEquals(
                HttpStatus.CREATED,
                responseStatus.value()
        );
    }

    @Test
    void crearProveedorDuplicadoDevuelve409() {

        Usuario usuario =
                usuario(
                        2L
                );

        ProveedorRequest request =
                requestValida();

        when(
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuario
        );

        when(
                proveedorService.crearProveedor(
                        2L,
                        "Proveedor nuevo",
                        "B12345678",
                        "922111222",
                        "proveedor@prueba.es",
                        "Proveedor de prueba"
                )
        ).thenThrow(
                new IllegalStateException(
                        "Ya existe un proveedor con NIF/CIF "
                                + "B12345678"
                                + " para este administrador."
                )
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.crear(
                                        request,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        assertEquals(
                "Ya existe un proveedor con NIF/CIF "
                        + "B12345678"
                        + " para este administrador.",
                error.getReason()
        );
    }

    @Test
    void crearSinNombreDevuelve400() {

        ProveedorRequest request =
                requestValida();

        request.setNombre(
                "   "
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.crear(
                                        request,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        assertEquals(
                "El nombre del proveedor es obligatorio.",
                error.getReason()
        );

        verify(
                accesoComunidadService,
                never()
        ).obtenerUsuarioAutenticado(
                authentication
        );

        verify(
                proveedorService,
                never()
        ).crearProveedor(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void actualizarProveedorPropio() {

        Usuario usuario =
                usuario(
                        2L
                );

        ProveedorRequest request =
                requestValida();

        request.setNombre(
                "Proveedor actualizado"
        );

        request.setActivo(
                false
        );

        Proveedor actualizado =
                proveedor(
                        10L,
                        2L,
                        "Proveedor actualizado",
                        "B12345678",
                        false
                );

        actualizado.setTelefono(
                "922111222"
        );

        actualizado.setEmail(
                "proveedor@prueba.es"
        );

        actualizado.setObservaciones(
                "Proveedor de prueba"
        );

        when(
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuario
        );

        when(
                proveedorService.actualizarProveedor(
                        2L,
                        10L,
                        "Proveedor actualizado",
                        "B12345678",
                        "922111222",
                        "proveedor@prueba.es",
                        "Proveedor de prueba",
                        false
                )
        ).thenReturn(
                actualizado
        );

        ProveedorResponse resultado =
                controller.actualizar(
                        10L,
                        request,
                        authentication
                );

        assertEquals(
                10L,
                resultado.id()
        );

        assertEquals(
                "Proveedor actualizado",
                resultado.nombre()
        );

        assertEquals(
                false,
                resultado.activo()
        );

        verify(
                proveedorService
        ).actualizarProveedor(
                2L,
                10L,
                "Proveedor actualizado",
                "B12345678",
                "922111222",
                "proveedor@prueba.es",
                "Proveedor de prueba",
                false
        );
    }

    @Test
    void actualizarProveedorDeOtroAdministradorDevuelve404() {

        Usuario usuario =
                usuario(
                        2L
                );

        ProveedorRequest request =
                requestValida();

        when(
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuario
        );

        when(
                proveedorService.actualizarProveedor(
                        2L,
                        10L,
                        "Proveedor nuevo",
                        "B12345678",
                        "922111222",
                        "proveedor@prueba.es",
                        "Proveedor de prueba",
                        true
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
                                controller.actualizar(
                                        10L,
                                        request,
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
    }

    @Test
    void actualizarProveedorConNifDuplicadoDevuelve409() {

        Usuario usuario =
                usuario(
                        2L
                );

        ProveedorRequest request =
                requestValida();

        when(
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuario
        );

        when(
                proveedorService.actualizarProveedor(
                        2L,
                        10L,
                        "Proveedor nuevo",
                        "B12345678",
                        "922111222",
                        "proveedor@prueba.es",
                        "Proveedor de prueba",
                        true
                )
        ).thenThrow(
                new IllegalStateException(
                        "Ya existe un proveedor con NIF/CIF "
                                + "B12345678"
                                + " para este administrador."
                )
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.actualizar(
                                        10L,
                                        request,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        assertEquals(
                "Ya existe un proveedor con NIF/CIF "
                        + "B12345678"
                        + " para este administrador.",
                error.getReason()
        );
    }

    @Test
    void usuarioSinAdministradorDevuelve403() {

        Usuario usuario =
                usuario(
                        null
                );

        when(
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        )
        ).thenReturn(
                usuario
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.listar(
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        assertEquals(
                "El usuario autenticado no tiene "
                        + "administrador asociado.",
                error.getReason()
        );

        verify(
                proveedorService,
                never()
        ).listarPorAdministrador(
                any()
        );
    }

    private Usuario usuario(
            Long administradorId
    ) {
        Usuario usuario =
                new Usuario();

        usuario.setAdministradorId(
                administradorId
        );

        return usuario;
    }

    private ProveedorRequest requestValida() {

        ProveedorRequest request =
                new ProveedorRequest();

        request.setNombre(
                "Proveedor nuevo"
        );

        request.setNifCif(
                "B12345678"
        );

        request.setTelefono(
                "922111222"
        );

        request.setEmail(
                "proveedor@prueba.es"
        );

        request.setObservaciones(
                "Proveedor de prueba"
        );

        request.setActivo(
                true
        );

        return request;
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

        proveedor.setActivo(
                activo
        );

        return proveedor;
    }
}
