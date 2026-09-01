package com.gombeth.urban.service;

import com.gombeth.urban.dto.remesa.ProcesoRemesaRequest;
import com.gombeth.urban.dto.remesa.ProcesoRemesaResponse;
import com.gombeth.urban.dto.SepaValidacionResultado;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuotaPresupuesto;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.RemesaEstado;
import com.gombeth.urban.entity.RemesaEventoTipo;
import com.gombeth.urban.entity.Vecino;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private SepaC19ValidationService sepaC19ValidationService;

    @Mock
    private SepaCoreXmlService sepaCoreXmlService;

    @Mock
    private SepaXmlValidationService sepaXmlValidationService;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private FicheroGeneradoRepository ficheroGeneradoRepository;

    @Mock
    private RemesaSeguimientoService remesaSeguimientoService;

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


    @Test
    void c19InvalidoDetieneProcesoAntesDeGenerarXmlYAntesDeGuardarFicheros() {
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

        ContabilidadRecibo recibo =
                org.mockito.Mockito.mock(
                        ContabilidadRecibo.class
                );

        when(recibo.getId()).thenReturn(501L);
        when(recibo.getImporte())
                .thenReturn(new BigDecimal("80.22"));

        FicheroGenerado remesa =
                org.mockito.Mockito.mock(
                        FicheroGenerado.class
                );

        when(remesa.getId()).thenReturn(70L);

        RemesaLinea linea =
                new RemesaLinea();

        linea.setVecinoId(196L);
        linea.setImporte(new BigDecimal("80.22"));
        linea.setDomiciliado(true);
        linea.setIncluidoSepa(true);

        Vecino vecino = new Vecino();

        SepaValidacionResultado validacionSepa =
                new SepaValidacionResultado();

        SepaValidacionResultado validacionC19 =
                new SepaValidacionResultado();

        validacionC19.addError(
                "Registro C19 estructuralmente incorrecto"
        );

        when(comunidadRepository.findById(comunidadId))
                .thenReturn(Optional.of(comunidad));

        when(
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                2026,
                                "APROBADA"
                        )
        ).thenReturn(List.of());

        when(
                remesaService.obtenerRecibosParaRemesa(
                        comunidadId,
                        fechaEmision
                )
        ).thenReturn(List.of(recibo));

        when(
                remesaService.eliminarRecibosYaIncluidos(
                        List.of(recibo)
                )
        ).thenReturn(List.of(recibo));

        when(
                remesaService.crearRemesaInicial(
                        any(),
                        any(),
                        any(),
                        any(LocalDate.class),
                        any(),
                        any()
                )
        ).thenReturn(remesa);

        when(
                remesaService.reciboYaIncluidoEnRemesa(
                        501L
                )
        ).thenReturn(false);

        when(
                remesaService.crearLineaDesdeRecibo(
                        remesa,
                        recibo
                )
        ).thenReturn(linea);

        when(
                remesaLineaRepository
                        .findByRemesaIdOrderByIdAsc(
                                70L
                        )
        ).thenReturn(List.of(linea));

        when(
                vecinoRepository.findById(
                        196L
                )
        ).thenReturn(Optional.of(vecino));

        when(
                sepaRemesaValidationService
                        .validarRemesaSepa(
                                comunidad,
                                List.of(linea),
                                List.of(vecino)
                        )
        ).thenReturn(validacionSepa);

        when(
                sepaC19Service.generarC19(
                        remesa,
                        comunidad,
                        List.of(linea),
                        List.of(vecino)
                )
        ).thenReturn("C19-GENERADO");

        when(
                sepaC19ValidationService.validar(
                        "C19-GENERADO"
                )
        ).thenReturn(validacionC19);

        ProcesoRemesaResponse response =
                service.ejecutar(request);

        assertFalse(response.isCorrecto());

        assertTrue(
                response.getMensaje().contains(
                        "no supera la validación estructural"
                )
        );

        verify(sepaC19ValidationService)
                .validar(
                        "C19-GENERADO"
                );

        verify(
                sepaCoreXmlService,
                never()
        ).generarXmlCore(
                any(),
                any(),
                anyList(),
                anyList()
        );

        verifyNoInteractions(
                documentStorageService
        );
    }

    @Test
    void xmlInvalidoDetieneProcesoAntesDeGuardarC19YXml() {
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

        ContabilidadRecibo recibo =
                org.mockito.Mockito.mock(
                        ContabilidadRecibo.class
                );

        when(recibo.getId()).thenReturn(502L);
        when(recibo.getImporte())
                .thenReturn(new BigDecimal("80.22"));

        FicheroGenerado remesa =
                org.mockito.Mockito.mock(
                        FicheroGenerado.class
                );

        when(remesa.getId()).thenReturn(71L);

        RemesaLinea linea =
                new RemesaLinea();

        linea.setVecinoId(197L);
        linea.setImporte(new BigDecimal("80.22"));
        linea.setDomiciliado(true);
        linea.setIncluidoSepa(true);

        Vecino vecino = new Vecino();

        SepaValidacionResultado validacionSepa =
                new SepaValidacionResultado();

        SepaValidacionResultado validacionC19 =
                new SepaValidacionResultado();

        SepaValidacionResultado validacionXml =
                new SepaValidacionResultado();

        validacionXml.addError(
                "XML no válido contra XSD"
        );

        when(comunidadRepository.findById(comunidadId))
                .thenReturn(Optional.of(comunidad));

        when(
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                2026,
                                "APROBADA"
                        )
        ).thenReturn(List.of());

        when(
                remesaService.obtenerRecibosParaRemesa(
                        comunidadId,
                        fechaEmision
                )
        ).thenReturn(List.of(recibo));

        when(
                remesaService.eliminarRecibosYaIncluidos(
                        List.of(recibo)
                )
        ).thenReturn(List.of(recibo));

        when(
                remesaService.crearRemesaInicial(
                        any(),
                        any(),
                        any(),
                        any(LocalDate.class),
                        any(),
                        any()
                )
        ).thenReturn(remesa);

        when(
                remesaService.reciboYaIncluidoEnRemesa(
                        502L
                )
        ).thenReturn(false);

        when(
                remesaService.crearLineaDesdeRecibo(
                        remesa,
                        recibo
                )
        ).thenReturn(linea);

        when(
                remesaLineaRepository
                        .findByRemesaIdOrderByIdAsc(
                                71L
                        )
        ).thenReturn(List.of(linea));

        when(
                vecinoRepository.findById(
                        197L
                )
        ).thenReturn(Optional.of(vecino));

        when(
                sepaRemesaValidationService
                        .validarRemesaSepa(
                                comunidad,
                                List.of(linea),
                                List.of(vecino)
                        )
        ).thenReturn(validacionSepa);

        when(
                sepaC19Service.generarC19(
                        remesa,
                        comunidad,
                        List.of(linea),
                        List.of(vecino)
                )
        ).thenReturn("C19-VALIDO");

        when(
                sepaC19ValidationService.validar(
                        "C19-VALIDO"
                )
        ).thenReturn(validacionC19);

        when(
                sepaCoreXmlService.generarXmlCore(
                        remesa,
                        comunidad,
                        List.of(linea),
                        List.of(vecino)
                )
        ).thenReturn("<xml>generado</xml>");

        when(
                sepaXmlValidationService.validar(
                        "<xml>generado</xml>"
                )
        ).thenReturn(validacionXml);

        ProcesoRemesaResponse response =
                service.ejecutar(request);

        assertFalse(response.isCorrecto());

        assertTrue(
                response.getMensaje().contains(
                        "no supera la validación XSD oficial"
                )
        );

        verify(sepaC19ValidationService)
                .validar(
                        "C19-VALIDO"
                );

        verify(sepaXmlValidationService)
                .validar(
                        "<xml>generado</xml>"
                );

        verifyNoInteractions(
                documentStorageService
        );
    }


    @Test
    void procesoCompletoRegistraRemesaGeneradaAntesDeValidarSepa() {
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

        ContabilidadRecibo recibo =
                org.mockito.Mockito.mock(
                        ContabilidadRecibo.class
                );

        when(recibo.getId()).thenReturn(504L);
        when(recibo.getImporte())
                .thenReturn(new BigDecimal("80.22"));

        FicheroGenerado remesa =
                org.mockito.Mockito.mock(
                        FicheroGenerado.class
                );

        when(remesa.getId()).thenReturn(73L);

        RemesaLinea linea =
                new RemesaLinea();

        linea.setVecinoId(199L);
        linea.setImporte(
                new BigDecimal("80.22")
        );
        linea.setDomiciliado(true);
        linea.setIncluidoSepa(true);

        Vecino vecino = new Vecino();

        SepaValidacionResultado validacionSepa =
                new SepaValidacionResultado();

        validacionSepa.addError(
                "Error SEPA de prueba"
        );

        when(comunidadRepository.findById(comunidadId))
                .thenReturn(Optional.of(comunidad));

        when(
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                2026,
                                "APROBADA"
                        )
        ).thenReturn(List.of());

        when(
                remesaService.obtenerRecibosParaRemesa(
                        comunidadId,
                        fechaEmision
                )
        ).thenReturn(List.of(recibo));

        when(
                remesaService.eliminarRecibosYaIncluidos(
                        List.of(recibo)
                )
        ).thenReturn(List.of(recibo));

        when(
                remesaService.crearRemesaInicial(
                        any(),
                        any(),
                        any(),
                        any(LocalDate.class),
                        any(),
                        any()
                )
        ).thenReturn(remesa);

        when(
                remesaService.reciboYaIncluidoEnRemesa(
                        504L
                )
        ).thenReturn(false);

        when(
                remesaService.crearLineaDesdeRecibo(
                        remesa,
                        recibo
                )
        ).thenReturn(linea);

        when(
                remesaLineaRepository
                        .findByRemesaIdOrderByIdAsc(
                                73L
                        )
        ).thenReturn(List.of(linea));

        when(
                vecinoRepository.findById(
                        199L
                )
        ).thenReturn(Optional.of(vecino));

        when(
                sepaRemesaValidationService
                        .validarRemesaSepa(
                                comunidad,
                                List.of(linea),
                                List.of(vecino)
                        )
        ).thenReturn(validacionSepa);

        ProcesoRemesaResponse response =
                service.ejecutar(request);

        assertFalse(response.isCorrecto());

        assertTrue(
                response.getMensaje().contains(
                        "no es válida para SEPA"
                )
        );

        org.mockito.InOrder orden =
                org.mockito.Mockito.inOrder(
                        remesaSeguimientoService,
                        sepaRemesaValidationService
                );

        orden.verify(
                remesaSeguimientoService
        ).registrarEvento(
                remesa,
                null,
                RemesaEventoTipo.REMESA_GENERADA,
                "REMESA",
                null,
                "Remesa generada desde proceso completo."
        );

        orden.verify(
                sepaRemesaValidationService
        ).validarRemesaSepa(
                comunidad,
                List.of(linea),
                List.of(vecino)
        );

        verify(
                sepaC19Service,
                never()
        ).generarC19(
                any(),
                any(),
                anyList(),
                anyList()
        );
    }

    @Test
    void procesoCorrectoPasaDeGeneradaAValidadaYDespuesAFicheroGenerado() throws IOException {
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

        ContabilidadRecibo recibo =
                org.mockito.Mockito.mock(
                        ContabilidadRecibo.class
                );

        when(recibo.getId()).thenReturn(503L);
        when(recibo.getImporte())
                .thenReturn(new BigDecimal("80.22"));

        FicheroGenerado remesa =
                org.mockito.Mockito.mock(
                        FicheroGenerado.class
                );

        when(remesa.getId()).thenReturn(72L);
        when(remesa.getFechaCobro())
                .thenReturn(
                        request.getFechaCobro()
                );
        when(remesa.getEsquemaSepa())
                .thenReturn("CORE");
        when(remesa.getTotalDomiciliado())
                .thenReturn(
                        new BigDecimal("80.22")
                );

        RemesaLinea linea =
                new RemesaLinea();

        linea.setVecinoId(198L);
        linea.setImporte(
                new BigDecimal("80.22")
        );
        linea.setDomiciliado(true);
        linea.setIncluidoSepa(true);

        Vecino vecino = new Vecino();

        SepaValidacionResultado validacionSepa =
                new SepaValidacionResultado();

        SepaValidacionResultado validacionC19 =
                new SepaValidacionResultado();

        SepaValidacionResultado validacionXml =
                new SepaValidacionResultado();

        when(comunidadRepository.findById(comunidadId))
                .thenReturn(Optional.of(comunidad));

        when(
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                2026,
                                "APROBADA"
                        )
        ).thenReturn(List.of());

        when(
                remesaService.obtenerRecibosParaRemesa(
                        comunidadId,
                        fechaEmision
                )
        ).thenReturn(List.of(recibo));

        when(
                remesaService.eliminarRecibosYaIncluidos(
                        List.of(recibo)
                )
        ).thenReturn(List.of(recibo));

        when(
                remesaService.crearRemesaInicial(
                        any(),
                        any(),
                        any(),
                        any(LocalDate.class),
                        any(),
                        any()
                )
        ).thenReturn(remesa);

        when(
                remesaService.reciboYaIncluidoEnRemesa(
                        503L
                )
        ).thenReturn(false);

        when(
                remesaService.crearLineaDesdeRecibo(
                        remesa,
                        recibo
                )
        ).thenReturn(linea);

        when(
                remesaLineaRepository
                        .findByRemesaIdOrderByIdAsc(
                                72L
                        )
        ).thenReturn(List.of(linea));

        when(
                vecinoRepository.findById(
                        198L
                )
        ).thenReturn(Optional.of(vecino));

        when(
                sepaRemesaValidationService
                        .validarRemesaSepa(
                                comunidad,
                                List.of(linea),
                                List.of(vecino)
                        )
        ).thenReturn(validacionSepa);

        when(
                sepaC19Service.generarC19(
                        remesa,
                        comunidad,
                        List.of(linea),
                        List.of(vecino)
                )
        ).thenReturn("C19-VALIDO");

        when(
                sepaC19ValidationService.validar(
                        "C19-VALIDO"
                )
        ).thenReturn(validacionC19);

        when(
                sepaCoreXmlService.generarXmlCore(
                        remesa,
                        comunidad,
                        List.of(linea),
                        List.of(vecino)
                )
        ).thenReturn("<xml>valido</xml>");

        when(
                sepaXmlValidationService.validar(
                        "<xml>valido</xml>"
                )
        ).thenReturn(validacionXml);

        when(
                documentStorageService.guardarRemesaC19(
                        comunidad,
                        "C19-VALIDO",
                        fechaEmision,
                        request.getFechaCobro(),
                        "CORE"
                )
        ).thenReturn(
                Path.of("remesa-72.c19")
        );

        when(
                documentStorageService.guardarRemesaXml(
                        comunidad,
                        "<xml>valido</xml>",
                        fechaEmision,
                        request.getFechaCobro(),
                        "CORE"
                )
        ).thenReturn(
                Path.of("remesa-72.xml")
        );

        ProcesoRemesaResponse response =
                service.ejecutar(request);

        assertTrue(response.isCorrecto());
        assertEquals(72L, response.getRemesaId());
        assertEquals(
                "remesa-72.c19",
                response.getFicheroC19()
        );
        assertEquals(
                "remesa-72.xml",
                response.getFicheroXml()
        );

        verify(remesaSeguimientoService)
                .registrarEvento(
                        remesa,
                        null,
                        RemesaEventoTipo.REMESA_GENERADA,
                        "REMESA",
                        null,
                        "Remesa generada desde proceso completo."
                );

        verify(remesaSeguimientoService)
                .cambiarEstado(
                        remesa,
                        RemesaEstado.VALIDADA,
                        null,
                        RemesaEventoTipo.VALIDACION_CORRECTA,
                        "SEPA",
                        null,
                        "Validación SEPA, C19 estructural y XML XSD superada."
                );

        verify(remesaSeguimientoService)
                .registrarEvento(
                        remesa,
                        null,
                        RemesaEventoTipo.C19_GENERADO,
                        "C19",
                        "remesa-72.c19",
                        "Fichero C19 generado y almacenado correctamente."
                );

        verify(remesaSeguimientoService)
                .cambiarEstado(
                        remesa,
                        RemesaEstado.FICHERO_GENERADO,
                        null,
                        RemesaEventoTipo.XML_GENERADO,
                        "XML",
                        "remesa-72.xml",
                        "Ficheros bancarios C19 y XML generados y almacenados correctamente."
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