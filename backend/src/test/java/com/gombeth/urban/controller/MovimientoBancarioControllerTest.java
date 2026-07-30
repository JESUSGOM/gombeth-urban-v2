package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import com.gombeth.urban.repository.UsuarioComunidadRepository;
import com.gombeth.urban.repository.UsuarioRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.ConciliacionBancariaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
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
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioComunidadRepository usuarioComunidadRepository;

    @Mock
    private ConciliacionBancariaService conciliacionBancariaService;

    @Mock
    private MovimientoBancario movimiento;

    @Mock
    private Usuario usuario;

    @Mock
    private Comunidad comunidad;

    @InjectMocks
    private MovimientoBancarioController controller;

    @Test
    void desconciliarMovimientoAutorizadoLlamaAlServicio() {
        prepararAccesoAutorizado();

        when(
                conciliacionBancariaService
                        .desconciliarMovimiento(
                                328L,
                                4L,
                                null
                        )
        ).thenReturn(movimiento);

        MovimientoBancario resultado =
                controller.desconciliar(
                        328L,
                        4L
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
        prepararAccesoAutorizado();

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
                        () ->
                                controller.desconciliar(
                                        328L,
                                        4L
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

    private void prepararAccesoAutorizado() {
        when(
                repository.findById(328L)
        ).thenReturn(
                Optional.of(movimiento)
        );

        when(
                movimiento.getId()
        ).thenReturn(328L);

        when(
                movimiento.getComunidadId()
        ).thenReturn(33L);

        when(
                usuarioRepository.findById(4L)
        ).thenReturn(
                Optional.of(usuario)
        );

        when(
                comunidadRepository.findById(33L)
        ).thenReturn(
                Optional.of(comunidad)
        );

        when(
                comunidad.getUsuarioId()
        ).thenReturn(4L);
    }
}