package com.gombeth.urban.controller;

import com.gombeth.urban.dto.ReciboResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuotaPresupuesto;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuotaPresupuestoRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ContabilidadAutomaticaService;
import com.gombeth.urban.service.PdfService;
import com.gombeth.urban.service.ReciboCobroService;
import com.gombeth.urban.service.ReciboEmailService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recibos")
public class ReciboController {

    private final CuotaPresupuestoRepository
            cuotaPresupuestoRepository;

    private final ContabilidadReciboRepository
            contabilidadReciboRepository;

    private final ComunidadRepository
            comunidadRepository;

    private final VecinoRepository vecinoRepository;

    private final ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    private final AccesoComunidadService
            accesoComunidadService;

    private final ReciboCobroService
            reciboCobroService;

    private final PdfService pdfService;

    private final ReciboEmailService reciboEmailService;

    public ReciboController(
            CuotaPresupuestoRepository cuotaPresupuestoRepository,
            ContabilidadReciboRepository contabilidadReciboRepository,
            ComunidadRepository comunidadRepository,
            VecinoRepository vecinoRepository,
            ContabilidadAutomaticaService contabilidadAutomaticaService,
            AccesoComunidadService accesoComunidadService,
            ReciboCobroService reciboCobroService,
            PdfService pdfService,
            ReciboEmailService reciboEmailService
    ) {
        this.cuotaPresupuestoRepository =
                cuotaPresupuestoRepository;

        this.contabilidadReciboRepository =
                contabilidadReciboRepository;

        this.comunidadRepository =
                comunidadRepository;

        this.vecinoRepository =
                vecinoRepository;

        this.contabilidadAutomaticaService =
                contabilidadAutomaticaService;

        this.accesoComunidadService =
                accesoComunidadService;

        this.reciboCobroService =
                reciboCobroService;

        this.pdfService = pdfService;

        this.reciboEmailService = reciboEmailService;
    }

    @PostMapping("/generar-desde-cuotas")
    public Map<String, Object> generarDesdeCuotas(
            @RequestParam Long comunidadId,
            @RequestParam Integer anio,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        return generarDesdeCuotasInterno(
                comunidadId,
                anio
        );
    }

    @GetMapping
    public List<ReciboResponse> listarRecibos(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        List<ContabilidadRecibo> recibos =
                contabilidadReciboRepository
                        .findByComunidadIdOrderByFechaEmisionDescIdDesc(
                                comunidadId
                        );

        Set<Long> vecinoIds =
                recibos.stream()
                        .map(
                                ContabilidadRecibo::getVecinoId
                        )
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Map<Long, Vecino> vecinosPorId =
                vecinoRepository
                        .findAllById(vecinoIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Vecino::getId,
                                        vecino -> vecino
                                )
                        );

        return recibos.stream()
                .map(recibo ->
                        toResponse(
                                recibo,
                                vecinosPorId.get(
                                        recibo.getVecinoId()
                                )
                        )
                )
                .toList();
    }

    @PostMapping("/limpiar-y-generar")
    public Map<String, Object> limpiarYGenerar(
            @RequestParam Long comunidadId,
            @RequestParam Integer mes,
            @RequestParam Integer anio,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        contabilidadReciboRepository.deletePendientesMes(
                comunidadId,
                mes,
                anio
        );

        return generarDesdeCuotasInterno(
                comunidadId,
                anio
        );
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ContabilidadRecibo recibo =
                obtenerRecibo(id);

        accesoComunidadService.validarAcceso(
                authentication,
                recibo.getComunidadId()
        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(
                                recibo.getComunidadId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Comunidad no encontrada para el recibo."
                                )
                        );

        Vecino vecino =
                vecinoRepository
                        .findById(
                                recibo.getVecinoId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Propietario no encontrado para el recibo."
                                )
                        );

        byte[] pdf = pdfService.generarReciboPdf(
                recibo,
                comunidad,
                vecino
        );

        String nombreArchivo =
                pdfService.nombreArchivoRecibo(
                        recibo,
                        vecino
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + nombreArchivo
                                + "\""
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .contentLength(pdf.length)
                .body(pdf);
    }

    @PostMapping("/{id}/enviar-email")
    public Map<String, Object> enviarEmail(
            @PathVariable Long id,
            Authentication authentication
    ) {
        ContabilidadRecibo recibo =
                obtenerRecibo(id);

        accesoComunidadService.validarAcceso(
                authentication,
                recibo.getComunidadId()
        );

        ReciboEmailService.ResultadoEnvio resultado =
                reciboEmailService.enviarRecibo(id);

        return Map.of(
                "correcto",
                true,
                "reciboId",
                resultado.reciboId(),
                "destinatario",
                resultado.destinatario(),
                "mensaje",
                resultado.mensaje()
        );
    }

    @PostMapping("/{id}/cobrar")
    public Map<String, Object> cobrarRecibo(
            @PathVariable Long id,
            @RequestParam(required = false)
            LocalDate fechaCobro,
            Authentication authentication
    ) {
        ContabilidadRecibo recibo =
                obtenerRecibo(id);

        accesoComunidadService.validarAcceso(
                authentication,
                recibo.getComunidadId()
        );

        Usuario usuario =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        ContabilidadRecibo actualizado =
                reciboCobroService
                        .cobrarManualmente(
                                recibo,
                                usuario.getId(),
                                fechaCobro
                        );

        return Map.of(
                "correcto",
                true,
                "reciboId",
                actualizado.getId(),
                "estado",
                actualizado.getEstado(),
                "fechaCobro",
                actualizado.getFechaCobroBanco(),
                "mensaje",
                "Recibo cobrado y contabilizado correctamente."
        );
    }

    @PostMapping("/{id}/anular-cobro")
    public Map<String, Object> anularCobro(
            @PathVariable Long id,
            @RequestParam(required = false)
            LocalDate fechaAnulacion,
            Authentication authentication
    ) {
        ContabilidadRecibo recibo =
                obtenerRecibo(id);

        accesoComunidadService.validarAcceso(
                authentication,
                recibo.getComunidadId()
        );

        Usuario usuario =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        ContabilidadRecibo actualizado =
                reciboCobroService
                        .anularCobroManual(
                                recibo,
                                usuario.getId(),
                                fechaAnulacion
                        );

        return Map.of(
                "correcto",
                true,
                "reciboId",
                actualizado.getId(),
                "estado",
                actualizado.getEstado(),
                "mensaje",
                "Cobro anulado y asiento inverso generado correctamente."
        );
    }

    private ContabilidadRecibo obtenerRecibo(
            Long id
    ) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del recibo es obligatorio."
            );
        }

        return contabilidadReciboRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Recibo no encontrado con ID: "
                                        + id
                        )
                );
    }

    private Map<String, Object> generarDesdeCuotasInterno(
            Long comunidadId,
            Integer anio
    ) {
        List<CuotaPresupuesto> cuotas =
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                anio,
                                "APROBADA"
                        );

        int generados = 0;
        int omitidos = 0;

        for (CuotaPresupuesto cuota : cuotas) {

            if (contabilidadReciboRepository
                    .existsByCuotaPresupuestoId(
                            cuota.getId()
                    )) {
                omitidos++;
                continue;
            }

            ContabilidadRecibo recibo =
                    new ContabilidadRecibo();

            recibo.setComunidadId(
                    cuota.getComunidadId()
            );

            recibo.setVecinoId(
                    cuota.getVecinoId()
            );

            recibo.setCuotaPresupuestoId(
                    cuota.getId()
            );

            recibo.setFechaEmision(
                    LocalDate.now()
            );

            recibo.setImporte(
                    cuota.getImporteMensual() != null
                            ? cuota.getImporteMensual()
                            : BigDecimal.ZERO
            );

            recibo.setPagadoAcumulado(
                    BigDecimal.ZERO
            );

            recibo.setEstado(
                    "PENDIENTE"
            );

            recibo.setTipoRemesa(
                    cuota.getRevisionId() == null
                            ? "ORDINARIA"
                            : "REVISION"
            );

            recibo.setConcepto(
                    construirConcepto(cuota)
            );

            recibo.setEtiquetaExtra(
                    "V"
                            + cuota.getVersion()
                            + " "
                            + cuota.getMesInicio()
                            + "-"
                            + cuota.getMesFin()
            );

            contabilidadReciboRepository.save(
                    recibo
            );

            contabilidadAutomaticaService
                    .registrarDevengoRecibo(
                            recibo
                    );

            generados++;
        }

        return Map.of(
                "comunidadId",
                comunidadId,
                "anio",
                anio,
                "recibosGenerados",
                generados,
                "recibosOmitidos",
                omitidos,
                "mensaje",
                "Proceso de generación de recibos finalizado"
        );
    }

    private String construirConcepto(
            CuotaPresupuesto cuota
    ) {
        return cuota.getDescripcion()
                + " - "
                + cuota.getAnio()
                + " - V"
                + cuota.getVersion();
    }

    private ReciboResponse toResponse(
            ContabilidadRecibo recibo,
            Vecino vecino
    ) {
        String nombreVecino =
                vecino != null
                        ? vecino.getNombre()
                        : "";

        String vivienda =
                vecino != null
                        ? vecino.getVivienda()
                        : "";

        return new ReciboResponse(
                recibo.getId(),
                recibo.getFechaEmision(),
                recibo.getVecinoId(),
                nombreVecino,
                vivienda,
                recibo.getConcepto(),
                recibo.getImporte(),
                recibo.getEstado(),
                recibo.getTipoRemesa(),
                recibo.getEtiquetaExtra()
        );
    }
}