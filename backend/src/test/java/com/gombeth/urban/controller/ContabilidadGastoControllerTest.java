package com.gombeth.urban.controller;

import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ContabilidadAutomaticaService;
import com.gombeth.urban.service.ContabilidadGastoService;
import com.gombeth.urban.service.CuentaContableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContabilidadGastoControllerTest {

    @Mock
    private ContabilidadGastoService
            gastoService;

    @Mock
    private ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    @Mock
    private CuentaContableService
            cuentaContableService;

    @Mock
    private AccesoComunidadService
            accesoComunidadService;

    @Mock
    private Authentication
            authentication;

    private ContabilidadGastoController
            controller;

    @BeforeEach
    void setUp() {
        controller =
                new ContabilidadGastoController(
                        gastoService,
                        contabilidadAutomaticaService,
                        cuentaContableService,
                        accesoComunidadService
                );

        /*
         * No establecemos pagosHabilitados.
         *
         * Al ser boolean y estar deshabilitado por defecto,
         * debe permanecer en false, igual que con:
         *
         * gombeth.gastos.pagos-habilitados:false
         */
    }

    @Test
    void configuracionMantienePagosDeshabilitadosPorDefecto() {

        var configuracion =
                controller.configuracion();

        assertEquals(
                Boolean.FALSE,
                configuracion.get(
                        "pagosHabilitados"
                )
        );
    }

    @Test
    void permitePagoCuandoConfiguracionLoHabilita() {

        org.springframework.test.util.ReflectionTestUtils
                .setField(
                        controller,
                        "pagosHabilitados",
                        true
                );

        ContabilidadGasto gasto =
                new ContabilidadGasto();

        gasto.setComunidadId(
                33L
        );

        com.gombeth.urban.entity.Usuario usuario =
                org.mockito.Mockito.mock(
                        com.gombeth.urban.entity.Usuario.class
                );

        LocalDate fechaPago =
                LocalDate.of(
                        2026,
                        9,
                        18
                );

        when(
                gastoService.findById(
                        25L
                )
        ).thenReturn(
                gasto
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
                usuario.getId()
        ).thenReturn(
                7L
        );

        when(
                gastoService.pagar(
                        25L,
                        7L,
                        fechaPago
                )
        ).thenReturn(
                gasto
        );

        var configuracion =
                controller.configuracion();

        assertEquals(
                Boolean.TRUE,
                configuracion.get(
                        "pagosHabilitados"
                )
        );

        ContabilidadGasto resultado =
                controller.pagar(
                        25L,
                        fechaPago,
                        authentication
                );

        assertEquals(
                gasto,
                resultado
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                33L
        );

        verify(
                gastoService
        ).pagar(
                25L,
                7L,
                fechaPago
        );
    }

    @Test
    void bloqueaPagoCuandoConfiguracionEstaDeshabilitada() {

        ContabilidadGasto gasto =
                new ContabilidadGasto();

        gasto.setComunidadId(
                33L
        );

        when(
                gastoService.findById(
                        25L
                )
        ).thenReturn(
                gasto
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.pagar(
                                        25L,
                                        LocalDate.of(
                                                2026,
                                                9,
                                                18
                                        ),
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        assertEquals(
                "Las operaciones de pago y reversión de gastos "
                        + "están deshabilitadas por configuración.",
                error.getReason()
        );

        /*
         * Se valida primero que el usuario pueda acceder
         * a la comunidad.
         */
        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                33L
        );

        /*
         * Pero el bloqueo debe producirse antes de obtener
         * el usuario y, sobre todo, antes de efectuar el pago.
         */
        verify(
                accesoComunidadService,
                never()
        ).obtenerUsuarioAutenticado(
                authentication
        );

        verify(
                gastoService,
                never()
        ).pagar(
                anyLong(),
                anyLong(),
                any()
        );
    }

    @Test
    void bloqueaDeshacerPagoCuandoConfiguracionEstaDeshabilitada() {

        ContabilidadGasto gasto =
                new ContabilidadGasto();

        gasto.setComunidadId(
                17L
        );

        when(
                gastoService.findById(
                        22L
                )
        ).thenReturn(
                gasto
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.deshacerPago(
                                        22L,
                                        LocalDate.of(
                                                2026,
                                                9,
                                                18
                                        ),
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        assertEquals(
                "Las operaciones de pago y reversión de gastos "
                        + "están deshabilitadas por configuración.",
                error.getReason()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                17L
        );

        verify(
                accesoComunidadService,
                never()
        ).obtenerUsuarioAutenticado(
                authentication
        );

        verify(
                gastoService,
                never()
        ).deshacerPago(
                anyLong(),
                anyLong(),
                any()
        );
    }

    @Test
    void bloqueaDeshacerContabilizacionCuandoConfiguracionEstaDeshabilitada() {

        ContabilidadGasto gasto =
                new ContabilidadGasto();

        gasto.setComunidadId(
                33L
        );

        when(
                gastoService.findById(
                        25L
                )
        ).thenReturn(
                gasto
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.deshacerContabilizacion(
                                        25L,
                                        LocalDate.of(
                                                2026,
                                                9,
                                                21
                                        ),
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        assertEquals(
                "Las operaciones de pago y reversión de gastos "
                        + "están deshabilitadas por configuración.",
                error.getReason()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                33L
        );

        verify(
                accesoComunidadService,
                never()
        ).obtenerUsuarioAutenticado(
                authentication
        );

        verify(
                gastoService,
                never()
        ).deshacerContabilizacion(
                anyLong(),
                anyLong(),
                any()
        );
    }

    @Test
    void eliminaGastoPendienteConAccesoValido() {

        ContabilidadGasto gasto =
                new ContabilidadGasto();

        gasto.setComunidadId(
                33L
        );

        gasto.setPagado(
                false
        );

        gasto.setNumeroAsiento(
                null
        );

        when(
                gastoService.findById(
                        25L
                )
        ).thenReturn(
                gasto
        );

        controller.eliminar(
                25L,
                authentication
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                33L
        );

        verify(
                gastoService
        ).eliminarPendiente(
                25L
        );
    }
}