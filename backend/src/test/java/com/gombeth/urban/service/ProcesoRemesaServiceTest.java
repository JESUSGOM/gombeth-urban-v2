package com.gombeth.urban.service;

import com.gombeth.urban.dto.remesa.ProcesoRemesaRequest;
import com.gombeth.urban.dto.remesa.ProcesoRemesaResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuotaPresupuesto;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuotaPresupuestoRepository;
import com.gombeth.urban.repository.FicheroGeneradoRepository;
import com.gombeth.urban.repository.RemesaLineaRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.storage.DocumentStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesoRemesaServiceTest {

    @Mock
    private ComunidadRepository comunidadRepository;

    @Mock
    private CuotaPresupuestoRepository cuotaPresupuestoRepository;

    @Mock
    private ContabilidadReciboRepository reciboRepository;

    @Mock
    private GeneracionReciboConceptosService generacionReciboConceptosService;

    @Mock
    private ContabilidadAutomaticaService contabilidadAutomaticaService;

    @Mock
    private RemesaService remesaService;

    @Mock
    private RemesaLineaRepository remesaLineaRepository;

    @Mock
    private VecinoRepository vecinoRepository;

    @Mock
    private SepaRemesaValidationService sepaRemesaValidationService;

    @Mock
    private SepaC19Service sepaC19Service;

    @Mock
    private SepaCoreXmlService sepaCoreXmlService;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private FicheroGeneradoRepository ficheroGeneradoRepository;

    @InjectMocks
    private ProcesoRemesaService service;

    @Test
    void crearReciboDesdeProcesoRemesaRegistraSuDevengoContable() {
        Long comunidadId = 18L;
        LocalDate fechaEmision = LocalDate.of(2026, 8, 1);

        ProcesoRemesaRequest request =
                crearRequest(
                        comunidadId,
                        2026,
                        8
                );

        Comunidad comunidad = new Comunidad();
        comunidad.setId(comunidadId);

        CuotaPresupuesto cuota =
                org.mockito.Mockito.mock(
                        CuotaPresupuesto.class
                );

        when(cuota.getId()).thenReturn(101L);
        when(cuota.getMesInicio()).thenReturn(1);
        when(cuota.getMesFin()).thenReturn(12);
        when(cuota.getVecinoId()).thenReturn(196L);
        when(cuota.getDescripcion()).thenReturn("Cuota ordinaria");
        when(cuota.getImporteMensual())
                .thenReturn(new BigDecimal("75.50"));

        when(comunidadRepository.findById(comunidadId))
                .thenReturn(Optional.of(comunidad));

        when(
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                2026,
                                "APROBADA"
                        )
        ).thenReturn(List.of(cuota));

        when(
                reciboRepository
                        .existsByCuotaPresupuestoIdAndFechaEmision(
                                101L,
                                fechaEmision
                        )
        ).thenReturn(false);

        when(
                reciboRepository.save(
                        any(ContabilidadRecibo.class)
                )
        ).thenAnswer(invocacion -> invocacion.getArgument(0));

        when(
                remesaService.obtenerRecibosParaRemesa(
                        comunidadId,
                        fechaEmision
                )
        ).thenReturn(List.of());

        when(
                remesaService.eliminarRecibosYaIncluidos(
                        anyList()
                )
        ).thenReturn(List.of());

        ProcesoRemesaResponse response =
                service.ejecutar(request);

        ArgumentCaptor<ContabilidadRecibo> reciboCaptor =
                ArgumentCaptor.forClass(
                        ContabilidadRecibo.class
                );

        verify(reciboRepository).save(
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
                new BigDecimal("75.50"),
                reciboGuardado.getImporte()
        );

        assertEquals(
                "PENDIENTE",
                reciboGuardado.getEstado()
        );

        assertEquals(
                "Cuota ordinaria - 8/2026",
                reciboGuardado.getConcepto()
        );

        verify(generacionReciboConceptosService)
                .generarConceptosDesdeCuota(
                        reciboGuardado,
                        cuota,
                        8
                );

        verify(contabilidadAutomaticaService)
                .registrarDevengoRecibo(
                        reciboGuardado
                );

        assertFalse(response.isCorrecto());

        assertEquals(
                "No hay recibos pendientes nuevos para generar remesa.",
                response.getMensaje()
        );
    }

    @Test
    void reciboYaExistenteNoVuelveARegistrarDevengo() {
        Long comunidadId = 18L;
        LocalDate fechaEmision = LocalDate.of(2026, 8, 1);

        ProcesoRemesaRequest request =
                crearRequest(
                        comunidadId,
                        2026,
                        8
                );

        Comunidad comunidad = new Comunidad();
        comunidad.setId(comunidadId);

        CuotaPresupuesto cuota =
                org.mockito.Mockito.mock(
                        CuotaPresupuesto.class
                );

        when(cuota.getId()).thenReturn(101L);
        when(cuota.getMesInicio()).thenReturn(1);
        when(cuota.getMesFin()).thenReturn(12);

        when(comunidadRepository.findById(comunidadId))
                .thenReturn(Optional.of(comunidad));

        when(
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                2026,
                                "APROBADA"
                        )
        ).thenReturn(List.of(cuota));

        when(
                reciboRepository
                        .existsByCuotaPresupuestoIdAndFechaEmision(
                                101L,
                                fechaEmision
                        )
        ).thenReturn(true);

        when(
                remesaService.obtenerRecibosParaRemesa(
                        comunidadId,
                        fechaEmision
                )
        ).thenReturn(List.of());

        when(
                remesaService.eliminarRecibosYaIncluidos(
                        anyList()
                )
        ).thenReturn(List.of());

        service.ejecutar(request);

        verify(
                reciboRepository,
                never()
        ).save(
                any(ContabilidadRecibo.class)
        );

        verify(
                generacionReciboConceptosService,
                never()
        ).generarConceptosDesdeCuota(
                any(ContabilidadRecibo.class),
                any(CuotaPresupuesto.class),
                any(Integer.class)
        );

        verify(
                contabilidadAutomaticaService,
                never()
        ).registrarDevengoRecibo(
                any(ContabilidadRecibo.class)
        );
    }

    @Test
    void cuotaFueraDelPeriodoNoGeneraReciboNiDevengo() {
        Long comunidadId = 18L;
        LocalDate fechaEmision = LocalDate.of(2026, 8, 1);

        ProcesoRemesaRequest request =
                crearRequest(
                        comunidadId,
                        2026,
                        8
                );

        Comunidad comunidad = new Comunidad();
        comunidad.setId(comunidadId);

        CuotaPresupuesto cuota =
                org.mockito.Mockito.mock(
                        CuotaPresupuesto.class
                );

        when(cuota.getMesInicio()).thenReturn(9);
        when(cuota.getMesFin()).thenReturn(12);

        when(comunidadRepository.findById(comunidadId))
                .thenReturn(Optional.of(comunidad));

        when(
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                2026,
                                "APROBADA"
                        )
        ).thenReturn(List.of(cuota));

        when(
                remesaService.obtenerRecibosParaRemesa(
                        comunidadId,
                        fechaEmision
                )
        ).thenReturn(List.of());

        when(
                remesaService.eliminarRecibosYaIncluidos(
                        anyList()
                )
        ).thenReturn(List.of());

        service.ejecutar(request);

        verify(
                reciboRepository,
                never()
        ).existsByCuotaPresupuestoIdAndFechaEmision(
                any(),
                any(LocalDate.class)
        );

        verify(
                reciboRepository,
                never()
        ).save(
                any(ContabilidadRecibo.class)
        );

        verify(
                generacionReciboConceptosService,
                never()
        ).generarConceptosDesdeCuota(
                any(ContabilidadRecibo.class),
                any(CuotaPresupuesto.class),
                any(Integer.class)
        );

        verify(
                contabilidadAutomaticaService,
                never()
        ).registrarDevengoRecibo(
                any(ContabilidadRecibo.class)
        );
    }

    @Test
    void fechaCobroAnteriorAlPeriodoEsRechazadaAntesDeGenerarRecibos() {
        ProcesoRemesaRequest request =
                crearRequest(
                        33L,
                        2027,
                        9
                );

        request.setFechaCobro(
                LocalDate.of(
                        2026,
                        8,
                        13
                )
        );

        ProcesoRemesaResponse response =
                service.ejecutar(request);

        assertFalse(response.isCorrecto());

        assertEquals(
                "Error en proceso de remesa: "
                        + "La fecha de cobro no puede ser anterior "
                        + "a la fecha de emisión del período: 2027-09-01",
                response.getMensaje()
        );

        verify(
                comunidadRepository,
                never()
        ).findById(
                any()
        );

        verify(
                reciboRepository,
                never()
        ).save(
                any(ContabilidadRecibo.class)
        );

        verify(
                remesaService,
                never()
        ).crearRemesaInicial(
                any(),
                any(),
                any(),
                any(LocalDate.class),
                any(),
                any()
        );
    }

    private ProcesoRemesaRequest crearRequest(
            Long comunidadId,
            Integer anio,
            Integer mes
    ) {
        ProcesoRemesaRequest request =
                new ProcesoRemesaRequest();

        request.setComunidadId(comunidadId);
        request.setAnio(anio);
        request.setMes(mes);
        request.setFechaCobro(
                LocalDate.of(
                        anio,
                        mes,
                        10
                )
        );

        return request;
    }
}