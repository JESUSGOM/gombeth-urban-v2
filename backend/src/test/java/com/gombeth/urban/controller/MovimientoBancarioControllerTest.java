package com.gombeth.urban.controller;

import com.gombeth.urban.dto.ConciliacionRequest;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ConciliacionBancariaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoBancarioControllerTest {

    @Mock
    private MovimientoBancarioRepository repository;

    @Mock
    private ContabilidadReciboRepository reciboRepository;

    @Mock
    private VecinoRepository vecinoRepository;

    @Mock
    private ComunidadRepository comunidadRepository;

    @Mock
    private AccesoComunidadService accesoComunidadService;

    @Mock
    private ConciliacionBancariaService
            conciliacionBancariaService;

    @Mock
    private MovimientoBancario movimiento;

    @Mock
    private ConciliacionRequest conciliacionRequest;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MovimientoBancarioController controller;

    @Test
    void conciliarUsaElUsuarioDeLaSesion() {
        prepararMovimientoAutorizado();

        when(
                accesoComunidadService.obtenerUsuarioId(
                        authentication
                )
        ).thenReturn(
                4L
        );

        when(
                conciliacionRequest.reciboIds()
        ).thenReturn(
                List.of(1554L)
        );

        when(
                conciliacionBancariaService
                        .conciliarMovimientoConRecibos(
                                328L,
                                List.of(1554L),
                                4L
                        )
        ).thenReturn(
                movimiento
        );

        MovimientoBancario resultado =
                controller.conciliar(
                        328L,
                        conciliacionRequest,
                        authentication
                );

        assertSame(
                movimiento,
                resultado
        );

        verify(accesoComunidadService)
                .validarAcceso(
                        authentication,
                        33L
                );

        verify(conciliacionBancariaService)
                .conciliarMovimientoConRecibos(
                        328L,
                        List.of(1554L),
                        4L
                );
    }

    @Test
    void desconciliarUsaElUsuarioDeLaSesion() {
        prepararMovimientoAutorizado();

        when(
                accesoComunidadService.obtenerUsuarioId(
                        authentication
                )
        ).thenReturn(
                4L
        );

        when(
                conciliacionBancariaService
                        .desconciliarMovimiento(
                                328L,
                                4L,
                                null
                        )
        ).thenReturn(
                movimiento
        );

        MovimientoBancario resultado =
                controller.desconciliar(
                        328L,
                        authentication
                );

        assertSame(
                movimiento,
                resultado
        );

        verify(conciliacionBancariaService)
                .desconciliarMovimiento(
                        328L,
                        4L,
                        null
                );
    }

    @Test
    void desconciliarMovimientoNoConciliadoDevuelveConflicto() {
        prepararMovimientoAutorizado();

        when(
                accesoComunidadService.obtenerUsuarioId(
                        authentication
                )
        ).thenReturn(
                4L
        );

        when(
                conciliacionBancariaService
                        .desconciliarMovimiento(
                                328L,
                                4L,
                                null
                        )
        ).thenThrow(
                new IllegalStateException(
                        "El movimiento no está conciliado."
                )
        );

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.desconciliar(
                                328L,
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                excepcion.getStatusCode()
        );

        assertEquals(
                "El movimiento no está conciliado.",
                excepcion.getReason()
        );
    }

    @Test
    void bloqueaMovimientoDeOtraComunidad() {
        when(
                repository.findById(328L)
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(
                movimiento.getComunidadId()
        ).thenReturn(
                33L
        );

        doThrow(
                new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tiene permisos para acceder "
                                + "a esta comunidad."
                )
        ).when(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                33L
        );

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.candidatos(
                                328L,
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                excepcion.getStatusCode()
        );

        verifyNoInteractions(
                reciboRepository,
                conciliacionBancariaService
        );
    }

    private void prepararMovimientoAutorizado() {
        when(
                repository.findById(328L)
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(
                movimiento.getId()
        ).thenReturn(
                328L
        );

        when(
                movimiento.getComunidadId()
        ).thenReturn(
                33L
        );
    }
}