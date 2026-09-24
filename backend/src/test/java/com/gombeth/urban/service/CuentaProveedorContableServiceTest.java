package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.TipoCuenta;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaProveedorContableServiceTest {

    @Mock
    private CuentaContableRepository
            cuentaContableRepository;

    @Mock
    private ComunidadRepository
            comunidadRepository;

    private CuentaProveedorContableService service;

    @BeforeEach
    void setUp() {
        service =
                new CuentaProveedorContableService(
                        cuentaContableRepository,
                        comunidadRepository
                );
    }

    @Test
    void generaMismosCodigosQueProgramaAntiguo() {

        assertEquals(
                "41030466",
                service.generarCodigoLegacy(
                        "Iberdrola Clientes, S.A.U."
                )
        );

        assertEquals(
                "41024980",
                service.generarCodigoLegacy(
                        "OTIS MOBILITY S.A."
                )
        );

        assertEquals(
                "41003892",
                service.generarCodigoLegacy(
                        "PRUEBA PASO 2B"
                )
        );
    }

    @Test
    void reutilizaCuentaHistoricaExistente() {

        Comunidad comunidad =
                comunidad(
                        33L
                );

        CuentaContable cuentaHistorica =
                cuenta(
                        373L,
                        "41030466",
                        "PROV: Iberdrola Clientes, S.A.U.",
                        comunidad
                );

        when(
                comunidadRepository.findById(
                        33L
                )
        ).thenReturn(
                Optional.of(
                        comunidad
                )
        );

        when(
                cuentaContableRepository.findByComunidadId(
                        33L
                )
        ).thenReturn(
                List.of(
                        cuentaHistorica
                )
        );

        CuentaContable resultado =
                service.resolverOCrear(
                        33L,
                        "Iberdrola Clientes, S.A.U."
                );

        assertSame(
                cuentaHistorica,
                resultado
        );

        verify(
                cuentaContableRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void reutilizaCuentaHistoricaConCifEnElNombre() {

        Comunidad comunidad =
                comunidad(
                        33L
                );

        CuentaContable cuentaHistorica =
                cuenta(
                        373L,
                        "41030466",
                        "PROV: Iberdrola Clientes, S.A.U. CIF: B12345678",
                        comunidad
                );

        when(
                comunidadRepository.findById(
                        33L
                )
        ).thenReturn(
                Optional.of(
                        comunidad
                )
        );

        when(
                cuentaContableRepository.findByComunidadId(
                        33L
                )
        ).thenReturn(
                List.of(
                        cuentaHistorica
                )
        );

        CuentaContable resultado =
                service.resolverOCrear(
                        33L,
                        "Iberdrola Clientes, S.A.U."
                );

        assertSame(
                cuentaHistorica,
                resultado
        );

        verify(
                cuentaContableRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void creaCuentaCompatibleSiNoExiste() {

        Comunidad comunidad =
                comunidad(
                        33L
                );

        when(
                comunidadRepository.findById(
                        33L
                )
        ).thenReturn(
                Optional.of(
                        comunidad
                )
        );

        when(
                cuentaContableRepository.findByComunidadId(
                        33L
                )
        ).thenReturn(
                List.of()
        );

        when(
                cuentaContableRepository
                        .findFirstByComunidad_IdAndCodigoOrderByIdAsc(
                                33L,
                                "41003892"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                cuentaContableRepository.save(
                        any(
                                CuentaContable.class
                        )
                )
        ).thenAnswer(
                invocation -> {

                    CuentaContable cuenta =
                            invocation.getArgument(
                                    0
                            );

                    cuenta.setId(
                            3000L
                    );

                    return cuenta;
                }
        );

        CuentaContable resultado =
                service.resolverOCrear(
                        33L,
                        "PRUEBA PASO 2B"
                );

        ArgumentCaptor<CuentaContable>
                captor =
                ArgumentCaptor.forClass(
                        CuentaContable.class
                );

        verify(
                cuentaContableRepository
        ).save(
                captor.capture()
        );

        CuentaContable creada =
                captor.getValue();

        assertEquals(
                "41003892",
                creada.getCodigo()
        );

        assertEquals(
                "PROV: PRUEBA PASO 2B",
                creada.getNombre()
        );

        assertEquals(
                TipoCuenta.PASIVO,
                creada.getTipo()
        );

        assertEquals(
                comunidad,
                creada.getComunidad()
        );

        assertEquals(
                3000L,
                resultado.getId()
        );
    }

    @Test
    void bloqueaColisionDeCodigoConOtroProveedor() {

        Comunidad comunidad =
                comunidad(
                        33L
                );

        CuentaContable cuentaOtroProveedor =
                cuenta(
                        4000L,
                        "41003892",
                        "PROV: OTRO PROVEEDOR",
                        comunidad
                );

        when(
                comunidadRepository.findById(
                        33L
                )
        ).thenReturn(
                Optional.of(
                        comunidad
                )
        );

        when(
                cuentaContableRepository.findByComunidadId(
                        33L
                )
        ).thenReturn(
                List.of()
        );

        when(
                cuentaContableRepository
                        .findFirstByComunidad_IdAndCodigoOrderByIdAsc(
                                33L,
                                "41003892"
                        )
        ).thenReturn(
                Optional.of(
                        cuentaOtroProveedor
                )
        );

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.resolverOCrear(
                                        33L,
                                        "PRUEBA PASO 2B"
                                )
                );

        assertEquals(
                "El código contable 41003892 "
                        + "ya está utilizado por otro proveedor "
                        + "en la comunidad 33.",
                error.getMessage()
        );

        verify(
                cuentaContableRepository,
                never()
        ).save(
                any()
        );
    }

    private Comunidad comunidad(
            Long id
    ) {
        Comunidad comunidad =
                new Comunidad();

        comunidad.setId(
                id
        );

        return comunidad;
    }

    private CuentaContable cuenta(
            Long id,
            String codigo,
            String nombre,
            Comunidad comunidad
    ) {
        CuentaContable cuenta =
                new CuentaContable();

        cuenta.setId(
                id
        );

        cuenta.setCodigo(
                codigo
        );

        cuenta.setNombre(
                nombre
        );

        cuenta.setTipo(
                TipoCuenta.PASIVO
        );

        cuenta.setComunidad(
                comunidad
        );

        return cuenta;
    }

    @Test
    void reutilizaCuentaHistoricaConNifConcatenado() {

        Comunidad comunidad =
                comunidad(
                        33L
                );

        CuentaContable cuentaHistorica =
                cuenta(
                        374L,
                        "41012345",
                        "PROV: MIGUEL VILLAR RAMOS 11951496Y",
                        comunidad
                );

        when(
                comunidadRepository.findById(
                        33L
                )
        ).thenReturn(
                Optional.of(
                        comunidad
                )
        );

        when(
                cuentaContableRepository.findByComunidadId(
                        33L
                )
        ).thenReturn(
                List.of(
                        cuentaHistorica
                )
        );

        CuentaContable resultado =
                service.resolverOCrear(
                        33L,
                        "MIGUEL VILLAR RAMOS",
                        "11951496Y"
                );

        assertSame(
                cuentaHistorica,
                resultado
        );

        verify(
                cuentaContableRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void creaCuentaConCifCuandoProveedorTieneNif() {

        Comunidad comunidad =
                comunidad(
                        33L
                );

        when(
                comunidadRepository.findById(
                        33L
                )
        ).thenReturn(
                Optional.of(
                        comunidad
                )
        );

        when(
                cuentaContableRepository.findByComunidadId(
                        33L
                )
        ).thenReturn(
                List.of()
        );

        when(
                cuentaContableRepository
                        .findFirstByComunidad_IdAndCodigoOrderByIdAsc(
                                33L,
                                "41030466"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                cuentaContableRepository.save(
                        any(
                                CuentaContable.class
                        )
                )
        ).thenAnswer(
                invocation -> {

                    CuentaContable cuenta =
                            invocation.getArgument(
                                    0
                            );

                    cuenta.setId(
                            3001L
                    );

                    return cuenta;
                }
        );

        CuentaContable resultado =
                service.resolverOCrear(
                        33L,
                        "Iberdrola Clientes, S.A.U.",
                        "B12345678"
                );

        assertEquals(
                "PROV: Iberdrola Clientes, S.A.U. CIF: B12345678",
                resultado.getNombre()
        );
    }
}
