package com.gombeth.urban.controller;

import com.gombeth.urban.dto.CuotaPresupuestoResponse;
import com.gombeth.urban.dto.GeneracionCuotasResponse;
import com.gombeth.urban.dto.PresupuestoResponse;
import com.gombeth.urban.dto.PresupuestoRevisionResponse;
import com.gombeth.urban.dto.RepartoPresupuestoResponse;
import com.gombeth.urban.dto.RevisionCuotasRequest;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuotaPresupuesto;
import com.gombeth.urban.entity.Presupuesto;
import com.gombeth.urban.entity.PresupuestoRevision;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadConfiguracionRepartoRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuotaPresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRevisionRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.GeneracionReciboConceptosService;
import com.gombeth.urban.service.RegeneracionRecibosService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {

    private final PresupuestoRepository presupuestoRepository;

    private final VecinoRepository vecinoRepository;

    private final CuotaPresupuestoRepository cuotaPresupuestoRepository;

    private final ComunidadConfiguracionRepartoRepository
            configuracionRepartoRepository;

    private final PresupuestoRevisionRepository
            presupuestoRevisionRepository;

    private final ContabilidadReciboRepository
            contabilidadReciboRepository;

    private final GeneracionReciboConceptosService
            generacionReciboConceptosService;

    private final RegeneracionRecibosService
            regeneracionRecibosService;

    private final AccesoComunidadService
            accesoComunidadService;

    public PresupuestoController(
            PresupuestoRepository presupuestoRepository,
            VecinoRepository vecinoRepository,
            CuotaPresupuestoRepository cuotaPresupuestoRepository,
            ComunidadConfiguracionRepartoRepository
                    configuracionRepartoRepository,
            PresupuestoRevisionRepository
                    presupuestoRevisionRepository,
            ContabilidadReciboRepository
                    contabilidadReciboRepository,
            GeneracionReciboConceptosService
                    generacionReciboConceptosService,
            RegeneracionRecibosService
                    regeneracionRecibosService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.presupuestoRepository =
                presupuestoRepository;

        this.vecinoRepository =
                vecinoRepository;

        this.cuotaPresupuestoRepository =
                cuotaPresupuestoRepository;

        this.configuracionRepartoRepository =
                configuracionRepartoRepository;

        this.presupuestoRevisionRepository =
                presupuestoRevisionRepository;

        this.contabilidadReciboRepository =
                contabilidadReciboRepository;

        this.generacionReciboConceptosService =
                generacionReciboConceptosService;

        this.regeneracionRecibosService =
                regeneracionRecibosService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    @GetMapping("/comunidad/{comunidadId}")
    public List<PresupuestoResponse> listarPorComunidad(
            @PathVariable Long comunidadId,
            @RequestParam(required = false) Integer anio,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        return obtenerPresupuestos(
                comunidadId,
                anio
        )
                .stream()
                .map(presupuesto ->
                        new PresupuestoResponse(
                                presupuesto.getId(),
                                presupuesto
                                        .getCuenta()
                                        .getId(),
                                presupuesto
                                        .getCuenta()
                                        .getCodigo(),
                                presupuesto
                                        .getCuenta()
                                        .getNombre(),
                                presupuesto.getAnio(),
                                presupuesto.getImporte()
                        )
                )
                .toList();
    }

    @GetMapping("/comunidad/{comunidadId}/resumen")
    public BigDecimal resumenPorComunidad(
            @PathVariable Long comunidadId,
            @RequestParam(required = false) Integer anio,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        return calcularTotalPresupuesto(
                comunidadId,
                anio
        );
    }

    @GetMapping("/comunidad/{comunidadId}/reparto")
    public List<RepartoPresupuestoResponse> simularReparto(
            @PathVariable Long comunidadId,
            @RequestParam int anio,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        return calcularReparto(
                comunidadId,
                anio
        );
    }

    @PostMapping("/comunidad/{comunidadId}/generar-recibos")
    public GeneracionCuotasResponse generarRecibosDesdeCuotas(
            @PathVariable Long comunidadId,
            @RequestParam Integer anio,
            @RequestParam Integer mes,
            @RequestParam(defaultValue = "false")
            Boolean regenerar,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        List<CuotaPresupuesto> cuotas =
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                anio,
                                "APROBADA"
                        );

        int borrados = 0;

        if (Boolean.TRUE.equals(regenerar)) {
            borrados =
                    regeneracionRecibosService
                            .borrarRecibosPeriodo(
                                    comunidadId,
                                    anio,
                                    mes
                            );
        }

        int generados = 0;

        LocalDate fechaEmision =
                LocalDate.of(
                        anio,
                        mes,
                        1
                );

        for (CuotaPresupuesto cuota : cuotas) {

            if (
                    contabilidadReciboRepository
                            .existsByCuotaPresupuestoIdAndFechaEmision(
                                    cuota.getId(),
                                    fechaEmision
                            )
            ) {
                continue;
            }

            ContabilidadRecibo recibo =
                    new ContabilidadRecibo();

            recibo.setComunidadId(
                    comunidadId
            );

            recibo.setVecinoId(
                    cuota.getVecinoId()
            );

            recibo.setCuotaPresupuestoId(
                    cuota.getId()
            );

            recibo.setFechaEmision(
                    fechaEmision
            );

            recibo.setImporte(
                    cuota.getImporteMensual()
            );

            recibo.setEstado(
                    "PENDIENTE"
            );

            recibo.setConcepto(
                    cuota.getDescripcion()
                            + " - "
                            + mes
                            + "/"
                            + anio
            );

            recibo.setTipoRemesa(
                    "ORDINARIA"
            );

            recibo.setPagadoAcumulado(
                    BigDecimal.ZERO
            );

            ContabilidadRecibo reciboGuardado =
                    contabilidadReciboRepository
                            .save(recibo);

            generacionReciboConceptosService
                    .generarConceptosDesdeCuota(
                            reciboGuardado,
                            cuota,
                            mes
                    );

            generados++;
        }

        return new GeneracionCuotasResponse(
                comunidadId,
                anio,
                generados,
                "Recibos generados correctamente. "
                        + "Recibos anteriores borrados: "
                        + borrados
        );
    }

    @GetMapping("/comunidad/{comunidadId}/cuotas-borrador")
    public List<CuotaPresupuestoResponse> listarCuotasBorrador(
            @PathVariable Long comunidadId,
            @RequestParam Integer anio,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        List<CuotaPresupuesto> cuotas =
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioOrderByIdAsc(
                                comunidadId,
                                anio
                        );

        return cuotas.stream()
                .map(cuota -> {

                    Vecino vecino =
                            vecinoRepository
                                    .findById(
                                            cuota.getVecinoId()
                                    )
                                    .orElse(null);

                    String nombre =
                            vecino != null
                                    ? vecino.getNombre()
                                    : "";

                    String vivienda =
                            vecino != null
                                    ? vecino.getVivienda()
                                    : "";

                    return new CuotaPresupuestoResponse(
                            cuota.getId(),
                            cuota.getComunidadId(),
                            cuota.getVecinoId(),
                            nombre,
                            vivienda,
                            cuota.getAnio(),
                            cuota.getMesInicio(),
                            cuota.getMesFin(),
                            cuota.getVersion(),
                            cuota.getMotivoRevision(),
                            cuota.getDescripcion(),
                            cuota.getCoeficiente(),
                            cuota.getImporteAnual(),
                            cuota.getImporteMensual(),
                            cuota.getEstado(),
                            cuota.getFechaGeneracion()
                    );
                })
                .toList();
    }

    @PostMapping("/comunidad/{comunidadId}/aprobar-cuotas")
    public GeneracionCuotasResponse aprobarCuotas(
            @PathVariable Long comunidadId,
            @RequestParam Integer anio,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        List<CuotaPresupuesto> cuotas =
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                anio,
                                "BORRADOR"
                        );

        for (CuotaPresupuesto cuota : cuotas) {
            cuota.setEstado(
                    "APROBADA"
            );

            cuotaPresupuestoRepository
                    .save(cuota);
        }

        return new GeneracionCuotasResponse(
                comunidadId,
                anio,
                cuotas.size(),
                "Cuotas aprobadas correctamente"
        );
    }

    @PostMapping("/comunidad/{comunidadId}/generar-revision-cuotas")
    public GeneracionCuotasResponse generarRevisionCuotas(
            @PathVariable Long comunidadId,
            @RequestBody RevisionCuotasRequest request,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        int mesesAplicacion =
                request.mesFin()
                        - request.mesInicio()
                        + 1;

        if (mesesAplicacion <= 0) {
            throw new IllegalArgumentException(
                    "El mes de inicio no puede "
                            + "ser posterior al mes de fin."
            );
        }

        BigDecimal totalPresupuesto =
                calcularTotalPresupuesto(
                        comunidadId,
                        request.anio()
                );

        if (
                totalPresupuesto.compareTo(
                        BigDecimal.ZERO
                ) == 0
        ) {
            throw new IllegalStateException(
                    "No se puede generar revisión "
                            + "porque el presupuesto total es cero."
            );
        }

        List<PresupuestoRevision> revisiones =
                presupuestoRevisionRepository
                        .findByComunidadIdAndAnioOrderByVersionAsc(
                                comunidadId,
                                request.anio()
                        );

        int nuevaVersion =
                revisiones.isEmpty()
                        ? 2
                        : revisiones
                        .get(
                                revisiones.size() - 1
                        )
                        .getVersion()
                        + 1;

        PresupuestoRevision revision =
                new PresupuestoRevision();

        revision.setComunidadId(
                comunidadId
        );

        revision.setAnio(
                request.anio()
        );

        revision.setVersion(
                nuevaVersion
        );

        revision.setMesInicio(
                request.mesInicio()
        );

        revision.setMesFin(
                request.mesFin()
        );

        revision.setImporteRevision(
                request.importeRevision()
        );

        revision.setEstado(
                "BORRADOR"
        );

        revision.setMotivoRevision(
                request.motivoRevision()
        );

        revision.setFechaGeneracion(
                LocalDateTime.now()
        );

        revision =
                presupuestoRevisionRepository
                        .save(revision);

        List<RepartoPresupuestoResponse> reparto =
                calcularReparto(
                        comunidadId,
                        request.anio()
                );

        for (
                RepartoPresupuestoResponse elemento
                : reparto
        ) {
            BigDecimal importeAnualRevision =
                    request
                            .importeRevision()
                            .multiply(
                                    elemento
                                            .getImporteAnual()
                            )
                            .divide(
                                    totalPresupuesto,
                                    2,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal importeMensualRevision =
                    importeAnualRevision.divide(
                            new BigDecimal(
                                    mesesAplicacion
                            ),
                            2,
                            RoundingMode.HALF_UP
                    );

            CuotaPresupuesto cuota =
                    new CuotaPresupuesto();

            cuota.setComunidadId(
                    comunidadId
            );

            cuota.setVecinoId(
                    elemento.getVecinoId()
            );

            cuota.setAnio(
                    request.anio()
            );

            cuota.setMesInicio(
                    request.mesInicio()
            );

            cuota.setMesFin(
                    request.mesFin()
            );

            cuota.setVersion(
                    nuevaVersion
            );

            cuota.setRevisionId(
                    revision.getId()
            );

            cuota.setMotivoRevision(
                    request.motivoRevision()
            );

            cuota.setDescripcion(
                    "Revisión presupuesto "
                            + request.anio()
                            + " meses "
                            + request.mesInicio()
                            + "-"
                            + request.mesFin()
            );

            cuota.setCoeficiente(
                    elemento.getCoeficiente()
            );

            cuota.setImporteAnual(
                    importeAnualRevision
            );

            cuota.setImporteMensual(
                    importeMensualRevision
            );

            cuota.setEstado(
                    "BORRADOR"
            );

            cuota.setFechaGeneracion(
                    LocalDateTime.now()
            );

            cuotaPresupuestoRepository
                    .save(cuota);
        }

        return new GeneracionCuotasResponse(
                comunidadId,
                request.anio(),
                reparto.size(),
                "Borrador de revisión de cuotas "
                        + "generado correctamente"
        );
    }

    @GetMapping("/comunidad/{comunidadId}/revisiones")
    public List<PresupuestoRevisionResponse> listarRevisiones(
            @PathVariable Long comunidadId,
            @RequestParam Integer anio,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        return presupuestoRevisionRepository
                .findByComunidadIdAndAnioOrderByVersionAsc(
                        comunidadId,
                        anio
                )
                .stream()
                .map(revision ->
                        new PresupuestoRevisionResponse(
                                revision.getId(),
                                revision.getComunidadId(),
                                revision.getAnio(),
                                revision.getVersion(),
                                revision.getMesInicio(),
                                revision.getMesFin(),
                                revision.getImporteRevision(),
                                revision.getEstado(),
                                revision.getMotivoRevision(),
                                revision.getFechaGeneracion()
                        )
                )
                .toList();
    }

    @PostMapping("/revisiones/{revisionId}/aprobar")
    public GeneracionCuotasResponse aprobarRevision(
            @PathVariable Long revisionId,
            Authentication authentication
    ) {
        PresupuestoRevision revision =
                obtenerRevision(
                        revisionId
                );

        validarAcceso(
                authentication,
                revision.getComunidadId()
        );

        List<CuotaPresupuesto> cuotas =
                cuotaPresupuestoRepository
                        .findByRevisionIdOrderByIdAsc(
                                revisionId
                        );

        if (cuotas.isEmpty()) {
            throw new IllegalStateException(
                    "No existen cuotas asociadas "
                            + "a la revisión "
                            + revisionId
            );
        }

        for (CuotaPresupuesto cuota : cuotas) {
            cuota.setEstado(
                    "APROBADA"
            );

            cuotaPresupuestoRepository
                    .save(cuota);
        }

        revision.setEstado(
                "APROBADA"
        );

        presupuestoRevisionRepository
                .save(revision);

        return new GeneracionCuotasResponse(
                revision.getComunidadId(),
                revision.getAnio(),
                cuotas.size(),
                "Revisión presupuestaria "
                        + "aprobada correctamente"
        );
    }

    @DeleteMapping("/revisiones/{revisionId}")
    public void eliminarRevision(
            @PathVariable Long revisionId,
            Authentication authentication
    ) {
        PresupuestoRevision revision =
                obtenerRevision(
                        revisionId
                );

        validarAcceso(
                authentication,
                revision.getComunidadId()
        );

        if (
                !"BORRADOR".equalsIgnoreCase(
                        revision.getEstado()
                )
        ) {
            throw new IllegalStateException(
                    "Solo se pueden eliminar revisiones "
                            + "en estado BORRADOR."
            );
        }

        cuotaPresupuestoRepository
                .deleteByRevisionId(
                        revisionId
                );

        presupuestoRevisionRepository
                .deleteById(
                        revisionId
                );
    }

    private void validarAcceso(
            Authentication authentication,
            Long comunidadId
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );
    }

    private List<Presupuesto> obtenerPresupuestos(
            Long comunidadId,
            Integer anio
    ) {
        if (anio != null) {
            return presupuestoRepository
                    .findByComunidadIdAndAnioOrderByCuentaCodigoAsc(
                            comunidadId,
                            anio
                    );
        }

        return presupuestoRepository
                .findByComunidadIdOrderByCuentaCodigoAsc(
                        comunidadId
                );
    }

    private BigDecimal calcularTotalPresupuesto(
            Long comunidadId,
            Integer anio
    ) {
        return obtenerPresupuestos(
                comunidadId,
                anio
        )
                .stream()
                .map(Presupuesto::getImporte)
                .filter(importe ->
                        importe != null
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private List<RepartoPresupuestoResponse> calcularReparto(
            Long comunidadId,
            int anio
    ) {
        BigDecimal total =
                calcularTotalPresupuesto(
                        comunidadId,
                        anio
                );

        List<Vecino> vecinos =
                vecinoRepository
                        .findByComunidadIdAndActivoTrueOrderByViviendaAscNombreAsc(
                                comunidadId
                        );

        String metodoReparto =
                configuracionRepartoRepository
                        .findByComunidadId(
                                comunidadId
                        )
                        .map(configuracion ->
                                configuracion
                                        .getMetodoReparto()
                        )
                        .orElse(
                                "COEFICIENTE"
                        );

        BigDecimal importeIgualitario =
                BigDecimal.ZERO;

        if (
                "IGUALITARIO".equalsIgnoreCase(
                        metodoReparto
                )
                        && !vecinos.isEmpty()
        ) {
            importeIgualitario =
                    total.divide(
                            new BigDecimal(
                                    vecinos.size()
                            ),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        BigDecimal importeIgualitarioFinal =
                importeIgualitario;

        return vecinos.stream()
                .map(vecino -> {

                    BigDecimal coeficiente =
                            vecino.getCoeficiente()
                                    == null
                                    ? BigDecimal.ZERO
                                    : vecino
                                    .getCoeficiente();

                    BigDecimal importeAnual;

                    if (
                            "IGUALITARIO"
                                    .equalsIgnoreCase(
                                            metodoReparto
                                    )
                    ) {
                        importeAnual =
                                importeIgualitarioFinal;
                    } else {
                        importeAnual =
                                total
                                        .multiply(
                                                coeficiente
                                        )
                                        .divide(
                                                new BigDecimal(
                                                        "100"
                                                ),
                                                2,
                                                RoundingMode.HALF_UP
                                        );
                    }

                    BigDecimal importeMensual =
                            importeAnual.divide(
                                    new BigDecimal(
                                            "12"
                                    ),
                                    2,
                                    RoundingMode.HALF_UP
                            );

                    return new RepartoPresupuestoResponse(
                            vecino.getId(),
                            vecino.getNombre(),
                            vecino.getVivienda(),
                            coeficiente,
                            importeAnual,
                            importeMensual
                    );
                })
                .toList();
    }

    private PresupuestoRevision obtenerRevision(
            Long revisionId
    ) {
        return presupuestoRevisionRepository
                .findById(
                        revisionId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la revisión "
                                        + "presupuestaria con id "
                                        + revisionId
                        )
                );
    }
}