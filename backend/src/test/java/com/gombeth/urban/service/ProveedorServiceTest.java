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

    private CuentaProveedorContableService
            cuentaProveedorContableService;

    private ProveedorService service;

    @BeforeEach
    void setUp() {

        cuentaProveedorContableService =
                new CuentaProveedorContableService(
                        cuentaContableRepository,
                        comunidadRepository
                );

        service =
                new ProveedorService(
                        proveedorRepository,
                        proveedorComunidadRepository,
                        comunidadRepository,
                        administradorRepository,
                        cuentaProveedorContableService
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

    @Test
    void obtieneProveedorSoloDelAdministradorIndicado() {

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Proveedor propio",
                        "B12345678"
                );

        when(
                proveedorRepository
                        .findByIdAndAdministradorId(
                                10L,
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        proveedor
                )
        );

        Proveedor resultado =
                service.obtenerPorAdministrador(
                        2L,
                        10L
                );

        assertEquals(
                10L,
                resultado.getId()
        );

        assertEquals(
                2L,
                resultado.getAdministradorId()
        );

        verify(
                proveedorRepository
        ).findByIdAndAdministradorId(
                10L,
                2L
        );
    }

    @Test
    void noObtieneProveedorDeOtroAdministrador() {

        when(
                proveedorRepository
                        .findByIdAndAdministradorId(
                                10L,
                                2L
                        )
        ).thenReturn(
                Optional.empty()
        );

        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.obtenerPorAdministrador(
                                        2L,
                                        10L
                                )
                );

        assertEquals(
                "El proveedor indicado no existe "
                        + "para este administrador.",
                excepcion.getMessage()
        );
    }

    @Test
    void actualizaProveedorPropioYNormalizaDatos() {

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Proveedor antiguo",
                        "B12345678"
                );

        when(
                proveedorRepository
                        .findByIdAndAdministradorId(
                                10L,
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        proveedor
                )
        );

        when(
                proveedorRepository
                        .findByAdministradorIdAndNifCifIgnoreCase(
                                2L,
                                "B12345678"
                        )
        ).thenReturn(
                Optional.of(
                        proveedor
                )
        );

        when(
                proveedorRepository.save(
                        proveedor
                )
        ).thenReturn(
                proveedor
        );

        Proveedor resultado =
                service.actualizarProveedor(
                        2L,
                        10L,
                        "  Proveedor actualizado  ",
                        " b-12345678 ",
                        " 922111222 ",
                        " proveedor@prueba.es ",
                        " Observaciones actualizadas ",
                        false
                );

        assertEquals(
                2L,
                resultado.getAdministradorId()
        );

        assertEquals(
                "Proveedor actualizado",
                resultado.getNombre()
        );

        assertEquals(
                "B12345678",
                resultado.getNifCif()
        );

        assertEquals(
                "922111222",
                resultado.getTelefono()
        );

        assertEquals(
                "proveedor@prueba.es",
                resultado.getEmail()
        );

        assertEquals(
                "Observaciones actualizadas",
                resultado.getObservaciones()
        );

        assertEquals(
                false,
                resultado.getActivo()
        );

        verify(
                proveedorRepository
        ).save(
                proveedor
        );
    }

    @Test
    void actualizarRechazaNifDeOtroProveedorDelMismoAdministrador() {

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Proveedor uno",
                        "B11111111"
                );

        Proveedor otroProveedor =
                proveedor(
                        11L,
                        2L,
                        "Proveedor dos",
                        "B22222222"
                );

        when(
                proveedorRepository
                        .findByIdAndAdministradorId(
                                10L,
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        proveedor
                )
        );

        when(
                proveedorRepository
                        .findByAdministradorIdAndNifCifIgnoreCase(
                                2L,
                                "B22222222"
                        )
        ).thenReturn(
                Optional.of(
                        otroProveedor
                )
        );

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.actualizarProveedor(
                                        2L,
                                        10L,
                                        "Proveedor uno",
                                        "B22222222",
                                        null,
                                        null,
                                        null,
                                        true
                                )
                );

        assertEquals(
                "Ya existe un proveedor con NIF/CIF "
                        + "B22222222"
                        + " para este administrador.",
                excepcion.getMessage()
        );

        verify(
                proveedorRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void obtieneProveedorActivoDesdeAsociacionValida() {

        ProveedorComunidad asociacion =
                asociacion(
                        100L,
                        10L,
                        33L,
                        true
                );

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Proveedor asociado",
                        "B12345678"
                );

        when(
                proveedorComunidadRepository
                        .findByIdAndComunidadId(
                                100L,
                                33L
                        )
        ).thenReturn(
                Optional.of(
                        asociacion
                )
        );

        when(
                proveedorRepository.findById(
                        10L
                )
        ).thenReturn(
                Optional.of(
                        proveedor
                )
        );

        Proveedor resultado =
                service.obtenerProveedorActivoPorAsociacion(
                        33L,
                        100L
                );

        assertEquals(
                10L,
                resultado.getId()
        );

        assertEquals(
                "Proveedor asociado",
                resultado.getNombre()
        );

        verify(
                proveedorComunidadRepository
        ).findByIdAndComunidadId(
                100L,
                33L
        );

        verify(
                proveedorRepository
        ).findById(
                10L
        );
    }

    @Test
    void rechazaAsociacionProveedorDeOtraComunidad() {

        when(
                proveedorComunidadRepository
                        .findByIdAndComunidadId(
                                100L,
                                33L
                        )
        ).thenReturn(
                Optional.empty()
        );

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.obtenerProveedorActivoPorAsociacion(
                                        33L,
                                        100L
                                )
                );

        assertEquals(
                "La asociación de proveedor indicada "
                        + "no pertenece a la comunidad.",
                error.getMessage()
        );

        verify(
                proveedorRepository,
                never()
        ).findById(
                any()
        );
    }

    @Test
    void rechazaAsociacionProveedorInactiva() {

        ProveedorComunidad asociacion =
                asociacion(
                        100L,
                        10L,
                        33L,
                        false
                );

        when(
                proveedorComunidadRepository
                        .findByIdAndComunidadId(
                                100L,
                                33L
                        )
        ).thenReturn(
                Optional.of(
                        asociacion
                )
        );

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.obtenerProveedorActivoPorAsociacion(
                                        33L,
                                        100L
                                )
                );

        assertEquals(
                "La asociación de proveedor está inactiva.",
                error.getMessage()
        );

        verify(
                proveedorRepository,
                never()
        ).findById(
                any()
        );
    }

    @Test
    void rechazaProveedorAsociadoInactivo() {

        ProveedorComunidad asociacion =
                asociacion(
                        100L,
                        10L,
                        33L,
                        true
                );

        Proveedor proveedor =
                proveedor(
                        10L,
                        2L,
                        "Proveedor inactivo",
                        "B12345678"
                );

        proveedor.setActivo(
                false
        );

        when(
                proveedorComunidadRepository
                        .findByIdAndComunidadId(
                                100L,
                                33L
                        )
        ).thenReturn(
                Optional.of(
                        asociacion
                )
        );

        when(
                proveedorRepository.findById(
                        10L
                )
        ).thenReturn(
                Optional.of(
                        proveedor
                )
        );

        IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.obtenerProveedorActivoPorAsociacion(
                                        33L,
                                        100L
                                )
                );

        assertEquals(
                "El proveedor asociado está inactivo.",
                error.getMessage()
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

        proveedor.setNombre(
                nombre
        );

        proveedor.setNifCif(
                nifCif
        );

        proveedor.setActivo(
                true
        );

        return proveedor;
    }

    private ProveedorComunidad asociacion(
            Long id,
            Long proveedorId,
            Long comunidadId,
            Boolean activo
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
                373L
        );

        asociacion.setActivo(
                activo
        );

        return asociacion;
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