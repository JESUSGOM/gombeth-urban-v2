package com.gombeth.urban.controller;

import com.gombeth.urban.dto.ReciboResponse;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuotaPresupuesto;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuotaPresupuestoRepository;
import com.gombeth.urban.repository.VecinoRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recibos")
public class ReciboController {

    private final CuotaPresupuestoRepository cuotaPresupuestoRepository;
    private final ContabilidadReciboRepository contabilidadReciboRepository;
    private final VecinoRepository vecinoRepository;

    public ReciboController(
            CuotaPresupuestoRepository cuotaPresupuestoRepository,
            ContabilidadReciboRepository contabilidadReciboRepository,
            VecinoRepository vecinoRepository
    ) {
        this.cuotaPresupuestoRepository = cuotaPresupuestoRepository;
        this.contabilidadReciboRepository = contabilidadReciboRepository;
        this.vecinoRepository = vecinoRepository;
    }

    @PostMapping("/generar-desde-cuotas")
    public Map<String, Object> generarDesdeCuotas(
            @RequestParam Long comunidadId,
            @RequestParam Integer anio
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

            boolean yaExiste =
                    contabilidadReciboRepository
                            .existsByCuotaPresupuestoId(
                                    cuota.getId()
                            );

            if (yaExiste) {
                omitidos++;
                continue;
            }

            ContabilidadRecibo recibo =
                    new ContabilidadRecibo();

            recibo.setComunidadId(cuota.getComunidadId());
            recibo.setVecinoId(cuota.getVecinoId());
            recibo.setCuotaPresupuestoId(cuota.getId());
            recibo.setFechaEmision(LocalDate.now());

            recibo.setImporte(
                    cuota.getImporteMensual() == null
                            ? BigDecimal.ZERO
                            : cuota.getImporteMensual()
            );

            recibo.setPagadoAcumulado(BigDecimal.ZERO);
            recibo.setEstado("PENDIENTE");

            recibo.setTipoRemesa(
                    cuota.getRevisionId() == null
                            ? "ORDINARIA"
                            : "REVISION"
            );

            recibo.setConcepto(construirConcepto(cuota));

            recibo.setEtiquetaExtra(
                    "V" + cuota.getVersion()
                            + " "
                            + cuota.getMesInicio()
                            + "-"
                            + cuota.getMesFin()
            );

            contabilidadReciboRepository.save(recibo);

            generados++;
        }

        return Map.of(
                "comunidadId", comunidadId,
                "anio", anio,
                "recibosGenerados", generados,
                "recibosOmitidos", omitidos,
                "mensaje", "Proceso de generación de recibos finalizado"
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

    @GetMapping
    public List<ReciboResponse> listarRecibos(
            @RequestParam Long comunidadId
    ) {
        return contabilidadReciboRepository
                .findByComunidadIdOrderByFechaEmisionDescIdDesc(
                        comunidadId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ReciboResponse toResponse(
            ContabilidadRecibo recibo
    ) {
        Vecino vecino =
                vecinoRepository
                        .findById(recibo.getVecinoId())
                        .orElse(null);

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