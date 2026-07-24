package com.gombeth.urban.service;

import com.gombeth.urban.dto.presentador.CuentaPresentadorRequest;
import com.gombeth.urban.dto.presentador.CuentaPresentadorResponse;
import com.gombeth.urban.entity.CuentaPresentador;
import com.gombeth.urban.repository.CuentaPresentadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaPresentadorServiceTest {

    @Mock
    private CuentaPresentadorRepository repository;

    @InjectMocks
    private CuentaPresentadorService service;

    @Test
    void listarConsultaSoloLasCuentasDelAdministrador() {

        CuentaPresentador cuenta =
                crearCuenta(
                        10L,
                        2L,
                        true
                );

        when(
                repository
                        .findByAdministradorIdOrderByAliasAsc(
                                2L
                        )
        ).thenReturn(List.of(cuenta));

        List<CuentaPresentadorResponse> resultado =
                service.listar(2L);

        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.getFirst().id());
        assertEquals(
                "Cuenta principal",
                resultado.getFirst().alias()
        );

        verify(repository)
                .findByAdministradorIdOrderByAliasAsc(
                        2L
                );
    }

    @Test
    void listarActivasExcluyeLasCuentasInactivas() {

        CuentaPresentador cuentaActiva =
                crearCuenta(
                        10L,
                        2L,
                        true
                );

        when(
                repository
                        .findByAdministradorIdAndActivaTrueOrderByAliasAsc(
                                2L
                        )
        ).thenReturn(List.of(cuentaActiva));

        List<CuentaPresentadorResponse> resultado =
                service.listarActivas(2L);

        assertEquals(1, resultado.size());
        assertTrue(resultado.getFirst().activa());

        verify(repository)
                .findByAdministradorIdAndActivaTrueOrderByAliasAsc(
                        2L
                );
    }

    @Test
    void crearAsignaAdministradorAutenticadoYNormalizaDatos() {

        CuentaPresentadorRequest request =
                requestValida();

        when(repository.save(any(CuentaPresentador.class)))
                .thenAnswer(invocacion -> {

                    CuentaPresentador cuenta =
                            invocacion.getArgument(0);

                    cuenta.setId(25L);

                    return cuenta;
                });

        CuentaPresentadorResponse resultado =
                service.crear(
                        4L,
                        request
                );

        ArgumentCaptor<CuentaPresentador> captor =
                ArgumentCaptor.forClass(
                        CuentaPresentador.class
                );

        verify(repository).save(
                captor.capture()
        );

        CuentaPresentador guardada =
                captor.getValue();

        assertEquals(
                4L,
                guardada.getAdministradorId()
        );

        assertEquals(
                "Cuenta principal",
                guardada.getAlias()
        );

        assertEquals(
                "ES6000311956607B",
                guardada.getIdentificadorPresentador()
        );

        assertEquals(
                "B12345678",
                guardada.getNifCif()
        );

        assertEquals(
                "ES9121000418450200051332",
                guardada.getIban()
        );

        assertEquals(
                "CAIXESBBXXX",
                guardada.getBic()
        );

        assertTrue(guardada.isActiva());
        assertEquals(25L, resultado.id());
    }

    @Test
    void actualizarSoloPermiteCuentasDelAdministrador() {

        CuentaPresentador existente =
                crearCuenta(
                        10L,
                        4L,
                        true
                );

        when(
                repository.findByIdAndAdministradorId(
                        10L,
                        4L
                )
        ).thenReturn(Optional.of(existente));

        when(repository.save(existente))
                .thenReturn(existente);

        CuentaPresentadorRequest request =
                requestValida();

        request.setAlias(
                "Cuenta actualizada"
        );

        CuentaPresentadorResponse resultado =
                service.actualizar(
                        4L,
                        10L,
                        request
                );

        assertEquals(
                "Cuenta actualizada",
                resultado.alias()
        );

        assertEquals(
                4L,
                existente.getAdministradorId()
        );

        verify(repository).save(existente);
    }

    @Test
    void actualizarCuentaDeOtroAdministradorDevuelveNoEncontrado() {

        when(
                repository.findByIdAndAdministradorId(
                        10L,
                        4L
                )
        ).thenReturn(Optional.empty());

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.actualizar(
                                4L,
                                10L,
                                requestValida()
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                excepcion.getStatusCode()
        );

        verify(repository, never())
                .save(any(CuentaPresentador.class));
    }

    @Test
    void obtenerActivaPropiaRechazaCuentaInactiva() {

        CuentaPresentador inactiva =
                crearCuenta(
                        10L,
                        4L,
                        false
                );

        when(
                repository.findByIdAndAdministradorId(
                        10L,
                        4L
                )
        ).thenReturn(Optional.of(inactiva));

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.obtenerActivaPropia(
                                4L,
                                10L
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                excepcion.getStatusCode()
        );

        assertFalse(inactiva.isActiva());
    }

    @Test
    void eliminarSoloBorraCuentaPropia() {

        CuentaPresentador cuenta =
                crearCuenta(
                        10L,
                        4L,
                        true
                );

        when(
                repository.findByIdAndAdministradorId(
                        10L,
                        4L
                )
        ).thenReturn(Optional.of(cuenta));

        service.eliminar(
                4L,
                10L
        );

        verify(repository).delete(cuenta);
    }

    @Test
    void crearRechazaAliasVacio() {

        CuentaPresentadorRequest request =
                requestValida();

        request.setAlias("   ");

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.crear(
                                4L,
                                request
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                excepcion.getStatusCode()
        );

        verify(repository, never())
                .save(any(CuentaPresentador.class));
    }

    private CuentaPresentadorRequest requestValida() {

        CuentaPresentadorRequest request =
                new CuentaPresentadorRequest();

        request.setAlias(
                "  Cuenta   principal  "
        );

        request.setBanco(
                "CaixaBank"
        );

        request.setIdentificadorPresentador(
                " es60 0031 1956 607b "
        );

        request.setNifCif(
                " b12345678 "
        );

        request.setSufijo(
                "000"
        );

        request.setIban(
                "ES91 2100 0418 4502 0005 1332"
        );

        request.setBic(
                "caix es bb xxx"
        );

        request.setActiva(true);

        request.setObservaciones(
                " Cuenta para remesas ordinarias. "
        );

        return request;
    }

    private CuentaPresentador crearCuenta(
            Long id,
            Long administradorId,
            boolean activa
    ) {

        CuentaPresentador cuenta =
                new CuentaPresentador();

        cuenta.setId(id);
        cuenta.setAdministradorId(
                administradorId
        );

        cuenta.setAlias(
                "Cuenta principal"
        );

        cuenta.setBanco(
                "CaixaBank"
        );

        cuenta.setIdentificadorPresentador(
                "ES6000311956607B"
        );

        cuenta.setNifCif(
                "B12345678"
        );

        cuenta.setSufijo(
                "000"
        );

        cuenta.setIban(
                "ES9121000418450200051332"
        );

        cuenta.setBic(
                "CAIXESBBXXX"
        );

        cuenta.setActiva(activa);

        return cuenta;
    }
}