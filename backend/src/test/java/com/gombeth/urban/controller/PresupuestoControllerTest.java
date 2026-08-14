package com.gombeth.urban.controller;

import com.gombeth.urban.dto.GeneracionCuotasResponse;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuotaPresupuesto;
import com.gombeth.urban.repository.ComunidadConfiguracionRepartoRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuotaPresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRevisionRepository;
import com.gombeth.urban.repository.PresupuestoRepartoConfiguracionRepository;
import com.gombeth.urban.repository.PresupuestoRepartoVecinoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresupuestoControllerTest {

    @Mock
    private PresupuestoRepository presupuestoRepository;

    @Mock
    private VecinoRepository vecinoRepository;

    @Mock
    private CuotaPresupuestoRepository cuotaPresupuestoRepository;

    @Mock
    private ComunidadConfiguracionRepartoRepository configuracionRepartoRepository;

    @Mock
    private PresupuestoRevisionRepository presupuestoRevisionRepository;

    @Mock
    private PresupuestoRepartoConfiguracionRepository
            presupuestoRepartoConfiguracionRepository;

    @Mock
    private PresupuestoRepartoVecinoRepository
            presupuestoRepartoVecinoRepository;

    @Mock
    private ContabilidadReciboRepository contabilidadReciboRepository;

    @Mock
    private GeneracionReciboConceptosService generacionReciboConceptosService;

    @Mock
    private ContabilidadAutomaticaService contabilidadAutomaticaService;

    @Mock
    private RegeneracionRecibosService regeneracionRecibosService;

    @Mock
    private AccesoComunidadService accesoComunidadService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PresupuestoController controller;

    @Test
    void generarRecibosRegistraDevengoYOmiteCuotasFueraDelPeriodo() {
        Long comunidadId = 18L;
        Integer anio = 2026;
        Integer mes = 8;
        LocalDate fechaEmision = LocalDate.of(anio, mes, 1);

        CuotaPresupuesto cuotaVigente =
                org.mockito.Mockito.mock(
                        CuotaPresupuesto.class
                );

        when(cuotaVigente.getId()).thenReturn(101L);
        when(cuotaVigente.getVecinoId()).thenReturn(196L);
        when(cuotaVigente.getDescripcion()).thenReturn("Cuota ordinaria");
        when(cuotaVigente.getImporteMensual())
                .thenReturn(new BigDecimal("26.67"));
        when(cuotaVigente.getMesInicio()).thenReturn(1);
        when(cuotaVigente.getMesFin()).thenReturn(12);

        CuotaPresupuesto cuotaFutura =
                org.mockito.Mockito.mock(
                        CuotaPresupuesto.class
                );

        when(cuotaFutura.getMesInicio()).thenReturn(9);
        when(cuotaFutura.getMesFin()).thenReturn(12);

        when(
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                anio,
                                "APROBADA"
                        )
        ).thenReturn(
                List.of(
                        cuotaVigente,
                        cuotaFutura
                )
        );

        when(
                contabilidadReciboRepository
                        .existsByCuotaPresupuestoIdAndFechaEmision(
                                101L,
                                fechaEmision
                        )
        ).thenReturn(false);

        when(
                contabilidadReciboRepository.save(
                        any(ContabilidadRecibo.class)
                )
        ).thenAnswer(invocacion -> invocacion.getArgument(0));

        GeneracionCuotasResponse response =
                controller.generarRecibosDesdeCuotas(
                        comunidadId,
                        anio,
                        mes,
                        false,
                        authentication
                );

        ArgumentCaptor<ContabilidadRecibo> reciboCaptor =
                ArgumentCaptor.forClass(
                        ContabilidadRecibo.class
                );

        verify(contabilidadReciboRepository).save(
                reciboCaptor.capture()
        );

        ContabilidadRecibo reciboGuardado =
                reciboCaptor.getValue();

        assertEquals(
                comunidadId,
                reciboGuardado.getComunidadId()
        );

        assertEquals(
                196L,
                reciboGuardado.getVecinoId()
        );

        assertEquals(
                101L,
                reciboGuardado.getCuotaPresupuestoId()
        );

        assertEquals(
                fechaEmision,
                reciboGuardado.getFechaEmision()
        );

        assertEquals(
                new BigDecimal("26.67"),
                reciboGuardado.getImporte()
        );

        verify(generacionReciboConceptosService)
                .generarConceptosDesdeCuota(
                        reciboGuardado,
                        cuotaVigente,
                        mes
                );

        verify(contabilidadAutomaticaService)
                .registrarDevengoRecibo(
                        reciboGuardado
                );

        verify(
                contabilidadReciboRepository,
                never()
        ).existsByCuotaPresupuestoIdAndFechaEmision(
                202L,
                fechaEmision
        );

        assertEquals(
                1,
                response.cuotasGeneradas()
        );
    }
}