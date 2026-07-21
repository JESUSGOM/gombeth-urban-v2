package com.gombeth.urban.service;

import com.gombeth.urban.entity.ConceptoCobro;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.ContabilidadReciboConcepto;
import com.gombeth.urban.entity.CuotaPresupuesto;
import com.gombeth.urban.repository.ConceptoCobroRepository;
import com.gombeth.urban.repository.ContabilidadReciboConceptoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeneracionReciboConceptosService {

    private final ConceptoCobroRepository conceptoCobroRepository;
    private final ContabilidadReciboConceptoRepository reciboConceptoRepository;

    public GeneracionReciboConceptosService(
            ConceptoCobroRepository conceptoCobroRepository,
            ContabilidadReciboConceptoRepository reciboConceptoRepository
    ) {
        this.conceptoCobroRepository = conceptoCobroRepository;
        this.reciboConceptoRepository = reciboConceptoRepository;
    }

    @Transactional
    public void generarConceptosDesdeCuota(
            ContabilidadRecibo recibo,
            CuotaPresupuesto cuota,
            Integer mes
    ) {
        if (recibo == null || recibo.getId() == null || cuota == null) {
            return;
        }

        reciboConceptoRepository.deleteByReciboId(recibo.getId());

        List<ConceptoCobro> conceptos =
                conceptoCobroRepository
                        .findByComunidadIdAndVecinoIdAndActivoTrueOrderByDescripcionAsc(
                                cuota.getComunidadId(),
                                cuota.getVecinoId()
                        )
                        .stream()
                        .filter(c -> correspondeMes(c, mes))
                        .filter(c -> c.getImporte() != null)
                        .filter(c -> c.getImporte().compareTo(BigDecimal.ZERO) > 0)
                        .toList();

        System.out.println("Recibo " + recibo.getId() + " -> vecino " + cuota.getVecinoId()
                + " conceptos encontrados: "
                + conceptos.size());

        if (conceptos.isEmpty()) {
            crearLineaFallback(recibo, cuota);
            return;
        }

        BigDecimal totalRecibo =
                recibo.getImporte() == null
                        ? BigDecimal.ZERO
                        : recibo.getImporte();

        BigDecimal totalConceptos =
                conceptos.stream()
                        .map(ConceptoCobro::getImporte)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BigDecimal> importesFinales =
                ajustarImportesAlTotal(
                        conceptos,
                        totalConceptos,
                        totalRecibo
                );

        int orden = 1;

        for (int i = 0; i < conceptos.size(); i++) {

            ConceptoCobro concepto = conceptos.get(i);

            System.out.println(concepto.getDescripcion() + " -> " + concepto.getImporte());

            ContabilidadReciboConcepto linea =
                    new ContabilidadReciboConcepto();

            linea.setReciboId(recibo.getId());
            linea.setConceptoCobroId(concepto.getId());
            linea.setDescripcion(limitar(concepto.getDescripcion(), 255));
            linea.setImporte(importesFinales.get(i));
            linea.setOrden(orden++);

            reciboConceptoRepository.save(linea);
        }
    }

    private List<BigDecimal> ajustarImportesAlTotal(
            List<ConceptoCobro> conceptos,
            BigDecimal totalConceptos,
            BigDecimal totalRecibo
    ) {
        List<BigDecimal> resultado = new ArrayList<>();

        if (conceptos.isEmpty()) {
            return resultado;
        }

        if (totalConceptos.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal importeIgual =
                    totalRecibo.divide(
                            BigDecimal.valueOf(conceptos.size()),
                            2,
                            RoundingMode.HALF_UP
                    );

            BigDecimal acumulado = BigDecimal.ZERO;

            for (int i = 0; i < conceptos.size(); i++) {
                if (i == conceptos.size() - 1) {
                    resultado.add(totalRecibo.subtract(acumulado));
                } else {
                    resultado.add(importeIgual);
                    acumulado = acumulado.add(importeIgual);
                }
            }

            return resultado;
        }

        BigDecimal acumulado = BigDecimal.ZERO;

        for (int i = 0; i < conceptos.size(); i++) {

            if (i == conceptos.size() - 1) {
                resultado.add(totalRecibo.subtract(acumulado));
                break;
            }

            BigDecimal proporcion =
                    conceptos.get(i)
                            .getImporte()
                            .divide(totalConceptos, 8, RoundingMode.HALF_UP);

            BigDecimal importe =
                    totalRecibo
                            .multiply(proporcion)
                            .setScale(2, RoundingMode.HALF_UP);

            resultado.add(importe);
            acumulado = acumulado.add(importe);
        }

        return resultado;
    }

    private void crearLineaFallback(
            ContabilidadRecibo recibo,
            CuotaPresupuesto cuota
    ) {
        ContabilidadReciboConcepto linea =
                new ContabilidadReciboConcepto();

        linea.setReciboId(recibo.getId());
        linea.setConceptoCobroId(null);
        linea.setDescripcion(limitar(cuota.getDescripcion(), 255));
        linea.setImporte(
                recibo.getImporte() == null
                        ? BigDecimal.ZERO
                        : recibo.getImporte()
        );
        linea.setOrden(1);

        reciboConceptoRepository.save(linea);
    }

    private boolean correspondeMes(
            ConceptoCobro concepto,
            Integer mes
    ) {
        if (mes == null || mes < 1 || mes > 12) {
            return true;
        }

        String periodicidad =
                concepto.getPeriodicidad() == null
                        ? "MENSUAL"
                        : concepto.getPeriodicidad().trim().toUpperCase();

        Integer mesInicio =
                concepto.getMesInicio() == null
                        ? 1
                        : concepto.getMesInicio();

        if (mes < mesInicio) {
            return false;
        }

        int diferencia = mes - mesInicio;

        return switch (periodicidad) {
            case "MENSUAL" -> true;
            case "BIMESTRAL" -> diferencia % 2 == 0;
            case "TRIMESTRAL" -> diferencia % 3 == 0;
            case "CUATRIMESTRAL" -> diferencia % 4 == 0;
            case "SEMESTRAL" -> diferencia % 6 == 0;
            case "ANUAL" -> mes.equals(mesInicio);
            case "PUNTUAL" -> mes.equals(mesInicio);
            default -> true;
        };
    }

    private String limitar(
            String texto,
            int max
    ) {
        if (texto == null || texto.isBlank()) {
            return "Recibo comunidad";
        }

        String limpio = texto.trim();

        if (limpio.length() <= max) {
            return limpio;
        }

        return limpio.substring(0, max);
    }
}