package com.gombeth.urban.controller;

import com.gombeth.urban.dto.CuotaPresupuestoResponse;
import com.gombeth.urban.dto.GeneracionCuotasResponse;
import com.gombeth.urban.dto.PresupuestoAltaRequest;
import com.gombeth.urban.dto.PresupuestoResponse;
import com.gombeth.urban.dto.PresupuestoRevisionResponse;
import com.gombeth.urban.dto.RepartoPresupuestoResponse;
import com.gombeth.urban.dto.RevisionCuotasRequest;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.CuotaPresupuesto;
import com.gombeth.urban.entity.Presupuesto;
import com.gombeth.urban.entity.PresupuestoRevision;
import com.gombeth.urban.entity.PresupuestoRepartoConfiguracion;
import com.gombeth.urban.entity.PresupuestoRepartoVecino;
import com.gombeth.urban.entity.TipoCuenta;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadConfiguracionRepartoRepository;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {

    private final PresupuestoRepository presupuestoRepository;

    private final ComunidadRepository comunidadRepository;

    private final CuentaContableRepository cuentaContableRepository;

    private final VecinoRepository vecinoRepository;

    private final CuotaPresupuestoRepository cuotaPresupuestoRepository;

    private final ComunidadConfiguracionRepartoRepository
            configuracionRepartoRepository;

    private final PresupuestoRevisionRepository
            presupuestoRevisionRepository;

    private final PresupuestoRepartoConfiguracionRepository
            presupuestoRepartoConfiguracionRepository;

    private final PresupuestoRepartoVecinoRepository
            presupuestoRepartoVecinoRepository;

    private final ContabilidadReciboRepository
            contabilidadReciboRepository;

    private final GeneracionReciboConceptosService
            generacionReciboConceptosService;

    private final ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    private final RegeneracionRecibosService
            regeneracionRecibosService;

    private final AccesoComunidadService
            accesoComunidadService;

    public PresupuestoController(
            PresupuestoRepository presupuestoRepository,
            ComunidadRepository comunidadRepository,
            CuentaContableRepository cuentaContableRepository,
            VecinoRepository vecinoRepository,
            CuotaPresupuestoRepository cuotaPresupuestoRepository,
            ComunidadConfiguracionRepartoRepository
                    configuracionRepartoRepository,
            PresupuestoRevisionRepository
                    presupuestoRevisionRepository,
            PresupuestoRepartoConfiguracionRepository
                    presupuestoRepartoConfiguracionRepository,
            PresupuestoRepartoVecinoRepository
                    presupuestoRepartoVecinoRepository,
            ContabilidadReciboRepository
                    contabilidadReciboRepository,
            GeneracionReciboConceptosService
                    generacionReciboConceptosService,
            ContabilidadAutomaticaService
                    contabilidadAutomaticaService,
            RegeneracionRecibosService
                    regeneracionRecibosService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.presupuestoRepository =
                presupuestoRepository;

        this.comunidadRepository =
                comunidadRepository;

        this.cuentaContableRepository =
                cuentaContableRepository;

        this.vecinoRepository =
                vecinoRepository;

        this.cuotaPresupuestoRepository =
                cuotaPresupuestoRepository;

        this.configuracionRepartoRepository =
                configuracionRepartoRepository;

        this.presupuestoRevisionRepository =
                presupuestoRevisionRepository;

        this.presupuestoRepartoConfiguracionRepository =
                presupuestoRepartoConfiguracionRepository;

        this.presupuestoRepartoVecinoRepository =
                presupuestoRepartoVecinoRepository;

        this.contabilidadReciboRepository =
                contabilidadReciboRepository;

        this.generacionReciboConceptosService =
                generacionReciboConceptosService;

        this.contabilidadAutomaticaService =
                contabilidadAutomaticaService;

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

        String metodoLegacy =
                obtenerMetodoRepartoLegacy(
                        comunidadId
                );

        return obtenerPresupuestos(
                comunidadId,
                anio
        )
                .stream()
                .map(presupuesto ->
                        convertirPresupuestoResponse(
                                presupuesto,
                                metodoLegacy
                        )
                )
                .toList();
    }

    @PostMapping("/comunidad/{comunidadId}")
    @Transactional
    public PresupuestoResponse crearPartida(
            @PathVariable Long comunidadId,
            @RequestBody PresupuestoAltaRequest request,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        DatosPartidaValidados datos =
                validarDatosPartida(
                        comunidadId,
                        request,
                        null
                );

        Presupuesto presupuesto =
                new Presupuesto(
                        datos.comunidad(),
                        datos.cuenta(),
                        request.anio(),
                        request.importe().setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                );

        Presupuesto guardado =
                presupuestoRepository.save(
                        presupuesto
                );

        guardarConfiguracionPartida(
                guardado.getId(),
                datos.metodoReparto(),
                datos.aplicaTodos(),
                datos.vecinoIds()
        );

        return convertirPresupuestoResponse(
                guardado,
                datos.metodoReparto()
        );
    }

    @PutMapping("/partidas/{presupuestoId}")
    @Transactional
    public PresupuestoResponse modificarPartida(
            @PathVariable Long presupuestoId,
            @RequestBody PresupuestoAltaRequest request,
            Authentication authentication
    ) {
        Presupuesto presupuesto =
                presupuestoRepository
                        .findById(presupuestoId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No existe la partida presupuestaria "
                                                + presupuestoId
                                )
                        );

        Long comunidadId =
                presupuesto
                        .getComunidad()
                        .getId();

        validarAcceso(
                authentication,
                comunidadId
        );

        DatosPartidaValidados datos =
                validarDatosPartida(
                        comunidadId,
                        request,
                        presupuestoId
                );

        presupuesto.setCuenta(
                datos.cuenta()
        );

        presupuesto.setAnio(
                request.anio()
        );

        presupuesto.setImporte(
                request.importe().setScale(
                        2,
                        RoundingMode.HALF_UP
                )
        );

        Presupuesto guardado =
                presupuestoRepository.save(
                        presupuesto
                );

        guardarConfiguracionPartida(
                guardado.getId(),
                datos.metodoReparto(),
                datos.aplicaTodos(),
                datos.vecinoIds()
        );

        return convertirPresupuestoResponse(
                guardado,
                datos.metodoReparto()
        );
    }

    @DeleteMapping("/partidas/{presupuestoId}")
    @Transactional
    public void eliminarPartida(
            @PathVariable Long presupuestoId,
            Authentication authentication
    ) {
        Presupuesto presupuesto =
                presupuestoRepository
                        .findById(presupuestoId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No existe la partida presupuestaria "
                                                + presupuestoId
                                )
                        );

        Long comunidadId =
                presupuesto
                        .getComunidad()
                        .getId();

        Integer anio =
                presupuesto.getAnio();

        validarAcceso(
                authentication,
                comunidadId
        );

        List<PresupuestoRevision> revisiones =
                presupuestoRevisionRepository
                        .findByComunidadIdAndAnioOrderByVersionAsc(
                                comunidadId,
                                anio
                        );

        if (!revisiones.isEmpty()) {
            throw new IllegalStateException(
                    "No se puede eliminar la partida porque existen "
                            + "revisiones presupuestarias para "
                            + anio
                            + ". Elimine primero las revisiones en "
                            + "borrador. Las revisiones aprobadas "
                            + "impiden modificar el presupuesto base."
            );
        }

        List<CuotaPresupuesto> cuotas =
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioOrderByIdAsc(
                                comunidadId,
                                anio
                        );

        boolean existenCuotasNoBorrador =
                cuotas.stream()
                        .anyMatch(cuota ->
                                !"BORRADOR".equalsIgnoreCase(
                                        cuota.getEstado()
                                )
                        );

        if (existenCuotasNoBorrador) {
            throw new IllegalStateException(
                    "No se puede eliminar la partida porque existen "
                            + "cuotas aprobadas o consolidadas para "
                            + anio
                            + "."
            );
        }

        if (!cuotas.isEmpty()) {
            /*
             * Las cuotas BORRADOR son datos derivados del presupuesto.
             * Al cambiar las partidas dejan de ser válidas y se eliminan
             * para obligar a generar un nuevo borrador coherente.
             */
            cuotaPresupuestoRepository.deleteAll(
                    cuotas
            );

            cuotaPresupuestoRepository.flush();
        }

        presupuestoRepartoVecinoRepository
                .deleteByPresupuestoId(
                        presupuestoId
                );

        presupuestoRepartoVecinoRepository.flush();

        presupuestoRepartoConfiguracionRepository
                .findByPresupuestoId(
                        presupuestoId
                )
                .ifPresent(configuracion -> {
                    presupuestoRepartoConfiguracionRepository
                            .delete(configuracion);

                    presupuestoRepartoConfiguracionRepository
                            .flush();
                });

        presupuestoRepository.delete(
                presupuesto
        );
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

    @PostMapping("/comunidad/{comunidadId}/generar-borrador-cuotas")
    @Transactional
    public GeneracionCuotasResponse generarBorradorCuotas(
            @PathVariable Long comunidadId,
            @RequestParam Integer anio,
            Authentication authentication
    ) {
        validarAcceso(
                authentication,
                comunidadId
        );

        if (anio == null || anio < 2000 || anio > 2100) {
            throw new IllegalArgumentException(
                    "El año del presupuesto no es válido."
            );
        }

        BigDecimal totalPresupuesto =
                calcularTotalPresupuesto(
                        comunidadId,
                        anio
                );

        if (totalPresupuesto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "No se puede generar el borrador "
                            + "porque el presupuesto total es cero."
            );
        }

        List<CuotaPresupuesto> cuotasExistentes =
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioOrderByIdAsc(
                                comunidadId,
                                anio
                        );

        boolean regenerandoBorrador =
                !cuotasExistentes.isEmpty();

        if (regenerandoBorrador) {
            boolean existenCuotasNoBorrador =
                    cuotasExistentes.stream()
                            .anyMatch(cuota ->
                                    !"BORRADOR".equalsIgnoreCase(
                                            cuota.getEstado()
                                    )
                            );

            if (existenCuotasNoBorrador) {
                throw new IllegalStateException(
                        "No se puede regenerar el borrador porque "
                                + "existen cuotas que ya no están en "
                                + "estado BORRADOR."
                );
            }

            boolean existenRevisiones =
                    cuotasExistentes.stream()
                            .anyMatch(cuota ->
                                    cuota.getRevisionId() != null
                                            || (
                                            cuota.getVersion() != null
                                                    && cuota.getVersion() > 1
                                    )
                            );

            if (existenRevisiones) {
                throw new IllegalStateException(
                        "No se puede regenerar el presupuesto inicial "
                                + "porque existen revisiones "
                                + "presupuestarias para ese año."
                );
            }
        }

        List<RepartoPresupuestoResponse> reparto =
                calcularReparto(
                        comunidadId,
                        anio
                );

        if (reparto.isEmpty()) {
            throw new IllegalStateException(
                    "No existen propietarios activos para generar cuotas."
            );
        }

        if (regenerandoBorrador) {
            cuotaPresupuestoRepository.deleteAll(
                    cuotasExistentes
            );

            /*
             * Elimina físicamente los borradores anteriores antes
             * de insertar los recalculados. Toda la operación queda
             * dentro de la misma transacción.
             */
            cuotaPresupuestoRepository.flush();
        }

        int generadas = 0;

        for (RepartoPresupuestoResponse elemento : reparto) {
            if (
                    elemento.getImporteAnual() == null
                            || elemento.getImporteAnual()
                            .compareTo(BigDecimal.ZERO) <= 0
            ) {
                continue;
            }

            CuotaPresupuesto cuota =
                    new CuotaPresupuesto();

            cuota.setComunidadId(
                    comunidadId
            );

            cuota.setVecinoId(
                    elemento.getVecinoId()
            );

            cuota.setAnio(
                    anio
            );

            cuota.setMesInicio(
                    1
            );

            cuota.setMesFin(
                    12
            );

            cuota.setVersion(
                    1
            );

            cuota.setRevisionId(
                    null
            );

            cuota.setMotivoRevision(
                    "Presupuesto inicial"
            );

            cuota.setDescripcion(
                    "Cuota presupuesto "
                            + anio
            );

            cuota.setCoeficiente(
                    elemento.getCoeficiente()
            );

            cuota.setImporteAnual(
                    elemento.getImporteAnual()
            );

            cuota.setImporteMensual(
                    elemento.getImporteMensual()
            );

            cuota.setEstado(
                    "BORRADOR"
            );

            cuota.setFechaGeneracion(
                    LocalDateTime.now()
            );

            cuotaPresupuestoRepository.save(
                    cuota
            );

            generadas++;
        }

        return new GeneracionCuotasResponse(
                comunidadId,
                anio,
                generadas,
                regenerandoBorrador
                        ? "Borrador de cuotas regenerado correctamente"
                        : "Borrador de cuotas generado correctamente"
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

            if (!cuotaAplicaAlMes(cuota, mes)) {
                continue;
            }

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

            contabilidadAutomaticaService
                    .registrarDevengoRecibo(
                            reciboGuardado
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
            if (
                    elemento.getImporteAnual() == null
                            || elemento.getImporteAnual()
                            .compareTo(BigDecimal.ZERO) <= 0
            ) {
                continue;
            }

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
        List<Presupuesto> partidas =
                presupuestoRepository
                        .findByComunidadIdAndAnioOrderByCuentaCodigoAsc(
                                comunidadId,
                                anio
                        );

        List<Vecino> vecinos =
                vecinoRepository
                        .findByComunidadIdAndActivoTrueOrderByViviendaAscNombreAsc(
                                comunidadId
                        );

        Map<Long, Vecino> vecinosPorId =
                vecinos.stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        Vecino::getId,
                                        vecino -> vecino,
                                        (a, b) -> a,
                                        LinkedHashMap::new
                                )
                        );

        Map<Long, BigDecimal> importesPorVecino =
                new LinkedHashMap<>();

        for (Vecino vecino : vecinos) {
            importesPorVecino.put(
                    vecino.getId(),
                    BigDecimal.ZERO.setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
            );
        }

        String metodoLegacy =
                obtenerMetodoRepartoLegacy(
                        comunidadId
                );

        for (Presupuesto partida : partidas) {
            PresupuestoRepartoConfiguracion configuracion =
                    presupuestoRepartoConfiguracionRepository
                            .findByPresupuestoId(
                                    partida.getId()
                            )
                            .orElse(null);

            String metodo =
                    configuracion == null
                            ? metodoLegacy
                            : normalizarMetodoReparto(
                                    configuracion.getMetodoReparto(),
                                    metodoLegacy
                            );

            boolean aplicaTodos =
                    configuracion == null
                            || Boolean.TRUE.equals(
                            configuracion.getAplicaTodos()
                    );

            List<Vecino> afectados;

            if (aplicaTodos) {
                afectados =
                        new ArrayList<>(
                                vecinos
                        );
            } else {
                Set<Long> idsAfectados =
                        presupuestoRepartoVecinoRepository
                                .findByPresupuestoIdOrderByVecinoIdAsc(
                                        partida.getId()
                                )
                                .stream()
                                .map(
                                        PresupuestoRepartoVecino::getVecinoId
                                )
                                .collect(
                                        java.util.stream.Collectors.toCollection(
                                                HashSet::new
                                        )
                                );

                afectados =
                        vecinos.stream()
                                .filter(vecino ->
                                        idsAfectados.contains(
                                                vecino.getId()
                                        )
                                )
                                .toList();
            }

            if (afectados.isEmpty()) {
                throw new IllegalStateException(
                        "La partida "
                                + descripcionPartida(partida)
                                + " no tiene propietarios activos afectados."
                );
            }

            Map<Long, BigDecimal> repartoPartida =
                    repartirPartida(
                            partida,
                            afectados,
                            metodo
                    );

            for (Map.Entry<Long, BigDecimal> entrada
                 : repartoPartida.entrySet()) {
                if (!vecinosPorId.containsKey(entrada.getKey())) {
                    continue;
                }

                importesPorVecino.merge(
                        entrada.getKey(),
                        entrada.getValue(),
                        BigDecimal::add
                );
            }
        }

        return vecinos.stream()
                .map(vecino -> {
                    BigDecimal coeficiente =
                            coeficienteVecino(
                                    vecino
                            );

                    BigDecimal importeAnual =
                            importesPorVecino
                                    .getOrDefault(
                                            vecino.getId(),
                                            BigDecimal.ZERO
                                    )
                                    .setScale(
                                            2,
                                            RoundingMode.HALF_UP
                                    );

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

    private Map<Long, BigDecimal> repartirPartida(
            Presupuesto partida,
            List<Vecino> afectados,
            String metodo
    ) {
        BigDecimal importe =
                partida.getImporte() == null
                        ? BigDecimal.ZERO
                        : partida.getImporte().setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        Map<Long, BigDecimal> resultado =
                new LinkedHashMap<>();

        BigDecimal acumulado =
                BigDecimal.ZERO.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal totalCoeficientes =
                BigDecimal.ZERO;

        if (
                "COEFICIENTE".equalsIgnoreCase(
                        metodo
                )
        ) {
            totalCoeficientes =
                    afectados.stream()
                            .map(
                                    this::coeficienteVecino
                            )
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            if (
                    totalCoeficientes.compareTo(
                            BigDecimal.ZERO
                    ) <= 0
            ) {
                throw new IllegalStateException(
                        "La partida "
                                + descripcionPartida(partida)
                                + " se reparte por coeficiente, "
                                + "pero los propietarios afectados "
                                + "no tienen coeficientes positivos."
                );
            }
        }

        for (int i = 0; i < afectados.size(); i++) {
            Vecino vecino =
                    afectados.get(i);

            BigDecimal importeVecino;

            if (i == afectados.size() - 1) {
                importeVecino =
                        importe.subtract(
                                acumulado
                        );
            } else if (
                    "PARTES_IGUALES".equalsIgnoreCase(
                            metodo
                    )
            ) {
                importeVecino =
                        importe.divide(
                                BigDecimal.valueOf(
                                        afectados.size()
                                ),
                                2,
                                RoundingMode.HALF_UP
                        );
            } else {
                importeVecino =
                        importe
                                .multiply(
                                        coeficienteVecino(
                                                vecino
                                        )
                                )
                                .divide(
                                        totalCoeficientes,
                                        2,
                                        RoundingMode.HALF_UP
                                );
            }

            importeVecino =
                    importeVecino.setScale(
                            2,
                            RoundingMode.HALF_UP
                    );

            resultado.put(
                    vecino.getId(),
                    importeVecino
            );

            acumulado =
                    acumulado.add(
                            importeVecino
                    );
        }

        return resultado;
    }

    private BigDecimal coeficienteVecino(
            Vecino vecino
    ) {
        return vecino.getCoeficiente() == null
                ? BigDecimal.ZERO
                : vecino.getCoeficiente();
    }

    private String descripcionPartida(
            Presupuesto presupuesto
    ) {
        if (
                presupuesto.getCuenta() != null
                        && presupuesto.getCuenta().getNombre() != null
        ) {
            return "'"
                    + presupuesto.getCuenta().getNombre()
                    + "'";
        }

        return "#"
                + presupuesto.getId();
    }

    private PresupuestoResponse convertirPresupuestoResponse(
            Presupuesto presupuesto,
            String metodoLegacy
    ) {
        PresupuestoRepartoConfiguracion configuracion =
                presupuestoRepartoConfiguracionRepository
                        .findByPresupuestoId(
                                presupuesto.getId()
                        )
                        .orElse(null);

        String metodo =
                configuracion == null
                        ? metodoLegacy
                        : normalizarMetodoReparto(
                                configuracion.getMetodoReparto(),
                                metodoLegacy
                        );

        boolean aplicaTodos =
                configuracion == null
                        || Boolean.TRUE.equals(
                        configuracion.getAplicaTodos()
                );

        List<Long> vecinoIds =
                aplicaTodos
                        ? List.of()
                        : presupuestoRepartoVecinoRepository
                        .findByPresupuestoIdOrderByVecinoIdAsc(
                                presupuesto.getId()
                        )
                        .stream()
                        .map(
                                PresupuestoRepartoVecino::getVecinoId
                        )
                        .toList();

        return new PresupuestoResponse(
                presupuesto.getId(),
                presupuesto.getCuenta().getId(),
                presupuesto.getCuenta().getCodigo(),
                presupuesto.getCuenta().getNombre(),
                presupuesto.getAnio(),
                presupuesto.getImporte(),
                metodo,
                aplicaTodos,
                vecinoIds,
                aplicaTodos
                        ? null
                        : vecinoIds.size()
        );
    }

    private DatosPartidaValidados validarDatosPartida(
            Long comunidadId,
            PresupuestoAltaRequest request,
            Long presupuestoIdActual
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Debe informar los datos de la partida presupuestaria."
            );
        }

        if (request.cuentaId() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una cuenta contable."
            );
        }

        if (request.anio() == null
                || request.anio() < 2000
                || request.anio() > 2100) {
            throw new IllegalArgumentException(
                    "El año del presupuesto no es válido."
            );
        }

        if (request.importe() == null
                || request.importe().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El importe debe ser mayor que cero."
            );
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No existe la comunidad "
                                                + comunidadId
                                )
                        );

        CuentaContable cuenta =
                cuentaContableRepository
                        .findByIdAndComunidad_Id(
                                request.cuentaId(),
                                comunidadId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La cuenta contable seleccionada "
                                                + "no pertenece a la comunidad."
                                )
                        );

        boolean esCuentaGasto =
                cuenta.getTipo() == TipoCuenta.GASTO;

        boolean esFondoReserva =
                "10200000".equals(
                        cuenta.getCodigo()
                );

        if (!esCuentaGasto && !esFondoReserva) {
            throw new IllegalArgumentException(
                    "Solo se pueden presupuestar cuentas de gasto "
                            + "o la cuenta 10200000 Fondo de Reserva."
            );
        }

        boolean existeDuplicada;

        if (presupuestoIdActual == null) {
            existeDuplicada =
                    presupuestoRepository
                            .existsByComunidad_IdAndCuenta_IdAndAnio(
                                    comunidadId,
                                    request.cuentaId(),
                                    request.anio()
                            );
        } else {
            existeDuplicada =
                    presupuestoRepository
                            .existsByComunidad_IdAndCuenta_IdAndAnioAndIdNot(
                                    comunidadId,
                                    request.cuentaId(),
                                    request.anio(),
                                    presupuestoIdActual
                            );
        }

        if (existeDuplicada) {
            throw new IllegalStateException(
                    "Ya existe una partida para esa cuenta y año."
            );
        }

        String metodoReparto =
                normalizarMetodoReparto(
                        request.metodoReparto(),
                        obtenerMetodoRepartoLegacy(
                                comunidadId
                        )
                );

        boolean aplicaTodos =
                request.aplicaTodos() == null
                        || Boolean.TRUE.equals(
                        request.aplicaTodos()
                );

        List<Long> vecinoIds =
                request.vecinoIds() == null
                        ? List.of()
                        : request.vecinoIds()
                        .stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        if (!aplicaTodos && vecinoIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe seleccionar al menos un propietario "
                            + "afectado por la partida."
            );
        }

        if (!aplicaTodos) {
            List<Vecino> vecinos =
                    vecinoRepository
                            .findAllById(
                                    vecinoIds
                            );

            Map<Long, Vecino> porId =
                    vecinos.stream()
                            .collect(
                                    java.util.stream.Collectors.toMap(
                                            Vecino::getId,
                                            vecino -> vecino
                                    )
                            );

            for (Long vecinoId : vecinoIds) {
                Vecino vecino =
                        porId.get(
                                vecinoId
                        );

                if (
                        vecino == null
                                || !Objects.equals(
                                comunidadId,
                                vecino.getComunidadId()
                        )
                                || !vecino.isActivo()
                ) {
                    throw new IllegalArgumentException(
                            "El propietario "
                                    + vecinoId
                                    + " no pertenece a la comunidad "
                                    + "o no está activo."
                    );
                }
            }
        }

        return new DatosPartidaValidados(
                comunidad,
                cuenta,
                metodoReparto,
                aplicaTodos,
                vecinoIds
        );
    }

    private void guardarConfiguracionPartida(
            Long presupuestoId,
            String metodoReparto,
            boolean aplicaTodos,
            List<Long> vecinoIds
    ) {
        PresupuestoRepartoConfiguracion configuracion =
                presupuestoRepartoConfiguracionRepository
                        .findByPresupuestoId(
                                presupuestoId
                        )
                        .orElseGet(
                                PresupuestoRepartoConfiguracion::new
                        );

        configuracion.setPresupuestoId(
                presupuestoId
        );

        configuracion.setMetodoReparto(
                metodoReparto
        );

        configuracion.setAplicaTodos(
                aplicaTodos
        );

        presupuestoRepartoConfiguracionRepository.save(
                configuracion
        );

        presupuestoRepartoVecinoRepository
                .deleteByPresupuestoId(
                        presupuestoId
                );

        /*
         * La eliminacion derivada queda pendiente en el contexto JPA.
         * Debe ejecutarse antes de volver a insertar las relaciones,
         * porque la tabla tiene una restriccion UNIQUE
         * (presupuesto_id, vecino_id).
         */
        presupuestoRepartoVecinoRepository.flush();

        if (aplicaTodos) {
            return;
        }

        for (Long vecinoId : vecinoIds) {
            PresupuestoRepartoVecino relacion =
                    new PresupuestoRepartoVecino();

            relacion.setPresupuestoId(
                    presupuestoId
            );

            relacion.setVecinoId(
                    vecinoId
            );

            presupuestoRepartoVecinoRepository.save(
                    relacion
            );
        }
    }

    private String obtenerMetodoRepartoLegacy(
            Long comunidadId
    ) {
        return configuracionRepartoRepository
                .findByComunidadId(
                        comunidadId
                )
                .map(configuracion ->
                        normalizarMetodoReparto(
                                configuracion.getMetodoReparto(),
                                "COEFICIENTE"
                        )
                )
                .orElse(
                        "COEFICIENTE"
                );
    }

    private String normalizarMetodoReparto(
            String metodo,
            String defecto
    ) {
        String valor =
                metodo == null
                        ? ""
                        : metodo.trim().toUpperCase();

        if (
                "PARTES_IGUALES".equals(valor)
                        || "IGUALITARIO".equals(valor)
        ) {
            return "PARTES_IGUALES";
        }

        if ("COEFICIENTE".equals(valor)) {
            return "COEFICIENTE";
        }

        if (defecto == null || defecto.isBlank()) {
            return "COEFICIENTE";
        }

        return normalizarMetodoReparto(
                defecto,
                "COEFICIENTE"
        );
    }

    private record DatosPartidaValidados(
            Comunidad comunidad,
            CuentaContable cuenta,
            String metodoReparto,
            boolean aplicaTodos,
            List<Long> vecinoIds
    ) {
    }

    private boolean cuotaAplicaAlMes(
            CuotaPresupuesto cuota,
            Integer mes
    ) {
        int mesInicio =
                cuota.getMesInicio() == null
                        ? 1
                        : cuota.getMesInicio();

        int mesFin =
                cuota.getMesFin() == null
                        ? 12
                        : cuota.getMesFin();

        return mes >= mesInicio
                && mes <= mesFin;
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