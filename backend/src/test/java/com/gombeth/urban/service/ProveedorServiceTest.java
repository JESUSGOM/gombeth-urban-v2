package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.Proveedor;
import com.gombeth.urban.entity.ProveedorComunidad;
import com.gombeth.urban.entity.TipoCuenta;
import com.gombeth.urban.repository.AdministradorRepository;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import com.gombeth.urban.repository.ProveedorComunidadRepository;
import com.gombeth.urban.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProveedorComunidadRepository
            proveedorComunidadRepository;

    @Mock
    private ComunidadRepository comunidadRepository;

    @Mock
    private CuentaContableRepository
            cuentaContableRepository;

    @Mock
    private AdministradorRepository
            administradorRepository;

    private ProveedorService service;

    @BeforeEach
    void setUp() {
        service =
                new ProveedorService(
                        proveedorRepository,
                        proveedorComunidadRepository,
                        comunidadRepository,
                        cuentaContableRepository,
                        administradorRepository
                );
    }

    @Test
    void creaProveedorYNormalizaNif() {

        when(
                administradorRepository.existsById(2L)
        ).thenReturn(true);

        when(
                proveedorRepository
                        .findByAdministradorIdAndNifCifIgnoreCase(
                                2L,
                                "B12345678"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                proveedorRepository.save(
                        any(Proveedor.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        Proveedor proveedor =
                service.crearProveedor(
                        2L,
                        "  Proveedor de prueba  ",
                        " b-12345678 ",
                        "922000000",
                        "proveedor@prueba.es",
                        "Proveedor temporal"
                );

        assertEquals(
                2L,
                proveedor.getAdministradorId()
        );

        assertEquals(
                "Proveedor de prueba",
                proveedor.getNombre()
        );

        assertEquals(
                "B12345678",
                proveedor.getNifCif()
        );

        assertEquals(
                true,
                proveedor.getActivo()
        );
    }

    @Test
    void reutilizaCuenta410HistoricaDelProveedor() {

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Iberdrola Clientes, S.A.U.",
                        null
                );

        Comunidad comunidad =
                comunidad(
                        33L,
                        2L
                );

        CuentaContable cuentaHistorica =
                new CuentaContable();

        cuentaHistorica.setId(
                373L
        );

        cuentaHistorica.setCodigo(
                "41030466"
        );

        cuentaHistorica.setNombre(
                "PROV: Iberdrola Clientes, S.A.U."
        );

        cuentaHistorica.setTipo(
                TipoCuenta.PASIVO
        );

        when(
                proveedorRepository.findById(10L)
        ).thenReturn(
                Optional.of(proveedor)
        );

        when(
                comunidadRepository.findById(33L)
        ).thenReturn(
                Optional.of(comunidad)
        );

        when(
                proveedorComunidadRepository
                        .findByProveedorIdAndComunidadId(
                                10L,
                                33L
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                cuentaContableRepository.findByComunidadId(
                        33L
                )
        ).thenReturn(
                List.of(cuentaHistorica)
        );

        when(
                proveedorComunidadRepository.save(
                        any(ProveedorComunidad.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProveedorComunidad asociacion =
                service.asociarAComunidad(
                        10L,
                        33L
                );

        assertEquals(
                373L,
                asociacion.getCuentaContableId()
        );

        verify(
                cuentaContableRepository,
                never()
        ).save(
                any(CuentaContable.class)
        );
    }

    @Test
    void creaCuentaConCodigoCompatibleConProgramaAntiguo() {

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Iberdrola Clientes, S.A.U.",
                        null
                );

        Comunidad comunidad =
                comunidad(
                        33L,
                        2L
                );

        when(
                proveedorRepository.findById(10L)
        ).thenReturn(
                Optional.of(proveedor)
        );

        when(
                comunidadRepository.findById(33L)
        ).thenReturn(
                Optional.of(comunidad)
        );

        when(
                proveedorComunidadRepository
                        .findByProveedorIdAndComunidadId(
                                10L,
                                33L
                        )
        ).thenReturn(
                Optional.empty()
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
                        any(CuentaContable.class)
                )
        ).thenAnswer(
                invocation -> {

                    CuentaContable cuenta =
                            invocation.getArgument(0);

                    cuenta.setId(
                            2000L
                    );

                    return cuenta;
                }
        );

        when(
                proveedorComunidadRepository.save(
                        any(ProveedorComunidad.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProveedorComunidad asociacion =
                service.asociarAComunidad(
                        10L,
                        33L
                );

        ArgumentCaptor<CuentaContable>
                cuentaCaptor =
                ArgumentCaptor.forClass(
                        CuentaContable.class
                );

        verify(
                cuentaContableRepository
        ).save(
                cuentaCaptor.capture()
        );

        CuentaContable cuenta =
                cuentaCaptor.getValue();

        assertEquals(
                "41030466",
                cuenta.getCodigo()
        );

        assertEquals(
                "PROV: Iberdrola Clientes, S.A.U.",
                cuenta.getNombre()
        );

        assertEquals(
                TipoCuenta.PASIVO,
                cuenta.getTipo()
        );

        assertEquals(
                2000L,
                asociacion.getCuentaContableId()
        );
    }

    @Test
    void impideAsociarProveedorDeOtroAdministrador() {

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Proveedor de prueba",
                        null
                );

        Comunidad comunidad =
                comunidad(
                        33L,
                        3L
                );

        when(
                proveedorRepository.findById(10L)
        ).thenReturn(
                Optional.of(proveedor)
        );

        when(
                comunidadRepository.findById(33L)
        ).thenReturn(
                Optional.of(comunidad)
        );

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.asociarAComunidad(
                                        10L,
                                        33L
                                )
                );

        assertEquals(
                "El proveedor y la comunidad pertenecen "
                        + "a administradores distintos.",
                error.getMessage()
        );

        verify(
                cuentaContableRepository,
                never()
        ).save(
                any()
        );

        verify(
                proveedorComunidadRepository,
                never()
        ).save(
                any()
        );
    }

    private Proveedor proveedor(
            Long id,
            Long administradorId,
            String nombre,
            String nifCif
    ) {
        Proveedor proveedor =
                new Proveedor();

        proveedor.setId(id);
        proveedor.setAdministradorId(
                administradorId
        );
        proveedor.setNombre(nombre);
        proveedor.setNifCif(nifCif);
        proveedor.setActivo(true);

        return proveedor;
    }

    private Comunidad comunidad(
            Long id,
            Long administradorId
    ) {
        Comunidad comunidad =
                new Comunidad();

        comunidad.setId(id);
        comunidad.setAdministradorId(
                administradorId
        );

        return comunidad;
    }
}