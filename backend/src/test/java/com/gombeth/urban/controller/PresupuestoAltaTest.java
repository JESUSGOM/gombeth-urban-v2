package com.gombeth.urban.controller;

import com.gombeth.urban.dto.PresupuestoAltaRequest;
import com.gombeth.urban.dto.PresupuestoResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.Presupuesto;
import com.gombeth.urban.entity.TipoCuenta;
import com.gombeth.urban.repository.ComunidadConfiguracionRepartoRepository;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import com.gombeth.urban.repository.CuotaPresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRevisionRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ContabilidadAutomaticaService;
import com.gombeth.urban.service.GeneracionReciboConceptosService;
import com.gombeth.urban.service.RegeneracionRecibosService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresupuestoAltaTest {

    @Mock
    private PresupuestoRepository presupuestoRepository;

    @Mock
    private ComunidadRepository comunidadRepository;

    @Mock
    private CuentaContableRepository cuentaContableRepository;

    @Mock
    private VecinoRepository vecinoRepository;

    @Mock
    private CuotaPresupuestoRepository cuotaPresupuestoRepository;

    @Mock
    private ComunidadConfiguracionRepartoRepository
            configuracionRepartoRepository;

    @Mock
    private PresupuestoRevisionRepository
            presupuestoRevisionRepository;

    @Mock
    private ContabilidadReciboRepository
            contabilidadReciboRepository;

    @Mock
    private GeneracionReciboConceptosService
            generacionReciboConceptosService;

    @Mock
    private ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    @Mock
    private RegeneracionRecibosService
            regeneracionRecibosService;

    @Mock
    private AccesoComunidadService accesoComunidadService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PresupuestoController controller;

    @Test
    void creaPartidaDeGastoParaLaComunidadAutorizada() {

        Long comunidadId = 33L;
        Long cuentaId = 1957L;

        Comunidad comunidad = new Comunidad();
        comunidad.setId(comunidadId);

        CuentaContable cuenta = new CuentaContable();
        cuenta.setId(cuentaId);
        cuenta.setCodigo("62900001");
        cuenta.setNombre("Servicio de Limpieza");
        cuenta.setTipo(TipoCuenta.GASTO);
        cuenta.setComunidad(comunidad);

        when(
                comunidadRepository.findById(comunidadId)
        ).thenReturn(
                Optional.of(comunidad)
        );

        when(
                cuentaContableRepository.findByIdAndComunidad_Id(
                        cuentaId,
                        comunidadId
                )
        ).thenReturn(
                Optional.of(cuenta)
        );

        when(
                presupuestoRepository
                        .existsByComunidad_IdAndCuenta_IdAndAnio(
                                comunidadId,
                                cuentaId,
                                2026
                        )
        ).thenReturn(false);

        when(
                presupuestoRepository.save(
                        any(Presupuesto.class)
                )
        ).thenAnswer(invocacion -> {

            Presupuesto presupuesto =
                    invocacion.getArgument(
                            0,
                            Presupuesto.class
                    );

            presupuesto.setId(77L);

            return presupuesto;
        });

        PresupuestoResponse response =
                controller.crearPartida(
                        comunidadId,
                        new PresupuestoAltaRequest(
                                cuentaId,
                                2026,
                                new BigDecimal("906.00")
                        ),
                        authentication
                );

        ArgumentCaptor<Presupuesto> captor =
                ArgumentCaptor.forClass(
                        Presupuesto.class
                );

        verify(presupuestoRepository).save(
                captor.capture()
        );

        Presupuesto guardado = captor.getValue();

        assertEquals(
                comunidad,
                guardado.getComunidad()
        );

        assertEquals(
                cuenta,
                guardado.getCuenta()
        );

        assertEquals(
                2026,
                guardado.getAnio()
        );

        assertEquals(
                new BigDecimal("906.00"),
                guardado.getImporte()
        );

        assertEquals(
                77L,
                response.id()
        );

        assertEquals(
                new BigDecimal("906.00"),
                response.importe()
        );

        verify(accesoComunidadService)
                .validarAcceso(
                        authentication,
                        comunidadId
                );
    }
}