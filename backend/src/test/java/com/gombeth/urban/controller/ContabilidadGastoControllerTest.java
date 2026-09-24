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
    void bloqueaPagoMientrasConviveConAplicacionAnterior() {

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
                "El pago y la anulación de gastos desde "
                        + "Gombeth Urban V2 están temporalmente "
                        + "deshabilitados mientras convive con "
                        + "la aplicación anterior.",
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
    void bloqueaDeshacerPagoMientrasConviveConAplicacionAnterior() {

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
                "El pago y la anulación de gastos desde "
                        + "Gombeth Urban V2 están temporalmente "
                        + "deshabilitados mientras convive con "
                        + "la aplicación anterior.",
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
    void bloqueaDeshacerContabilizacionMientrasConviveConAplicacionAnterior() {

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
                "El pago y la anulación de gastos desde "
                        + "Gombeth Urban V2 están temporalmente "
                        + "deshabilitados mientras convive con "
                        + "la aplicación anterior.",
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