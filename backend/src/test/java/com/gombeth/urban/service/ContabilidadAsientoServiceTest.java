package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContabilidadAsientoServiceTest {

    @Mock
    private ContabilidadAsientoRepository
            asientoRepository;

    @InjectMocks
    private ContabilidadAsientoService service;

    @Test
    void devuelveAsientoConfirmadoExistente() {
        ContabilidadAsiento existente =
                new ContabilidadAsiento();

        existente.setEstado(
                "CONFIRMADO"
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "RECIBO_COBRADO",
                                1554L
                        )
        ).thenReturn(
                Optional.of(existente)
        );

        ContabilidadAsiento resultado =
                service.crearAsientoAutomatico(
                        33L,
                        LocalDate.of(
                                2026,
                                7,
                                4
                        ),
                        "Cobro recibo 1554",
                        "RECIBO_COBRADO",
                        1554L,
                        4L
                );

        assertSame(
                existente,
                resultado
        );

        verify(
                asientoRepository,
                never()
        ).save(
                any(ContabilidadAsiento.class)
        );
    }

    @Test
    void creaNuevoAsientoCuandoElAnteriorEstaAnulado() {
        ContabilidadAsiento anterior =
                new ContabilidadAsiento();

        anterior.setEstado(
                "ANULADO"
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                33L,
                                "RECIBO_COBRADO",
                                1554L
                        )
        ).thenReturn(
                Optional.of(anterior)
        );

        when(
                asientoRepository
                        .findTopByComunidadIdAndEjercicioOrderByNumeroAsientoDesc(
                                33L,
                                2026
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                asientoRepository.save(
                        any(ContabilidadAsiento.class)
                )
        ).thenAnswer(
                invocacion ->
                        invocacion.getArgument(0)
        );

        ContabilidadAsiento resultado =
                service.crearAsientoAutomatico(
                        33L,
                        LocalDate.of(
                                2026,
                                7,
                                5
                        ),
                        "Nuevo cobro recibo 1554",
                        "RECIBO_COBRADO",
                        1554L,
                        4L
                );

        assertEquals(
                33L,
                resultado.getComunidadId()
        );

        assertEquals(
                2026,
                resultado.getEjercicio()
        );

        assertEquals(
                1L,
                resultado.getNumeroAsiento()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        7,
                        5
                ),
                resultado.getFecha()
        );

        assertEquals(
                "Nuevo cobro recibo 1554",
                resultado.getConcepto()
        );

        assertEquals(
                "RECIBO_COBRADO",
                resultado.getOrigen()
        );

        assertEquals(
                1554L,
                resultado.getOrigenId()
        );

        assertEquals(
                4L,
                resultado.getUsuarioId()
        );

        assertEquals(
                "CONFIRMADO",
                resultado.getEstado()
        );

        verify(
                asientoRepository
        ).save(
                any(ContabilidadAsiento.class)
        );
    }
}