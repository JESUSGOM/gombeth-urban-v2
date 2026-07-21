package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.FicheroGeneradoRepository;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;
import com.gombeth.urban.repository.RemesaLineaRepository;
import com.gombeth.urban.repository.VecinoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gombeth.urban.entity.ContabilidadReciboConcepto;
import com.gombeth.urban.repository.ContabilidadReciboConceptoRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class RemesaService {

    private final FicheroGeneradoRepository ficheroGeneradoRepository;
    private final RemesaLineaRepository remesaLineaRepository;
    private final RemesaLineaConceptoRepository remesaLineaConceptoRepository;
    private final VecinoRepository vecinoRepository;
    private final ContabilidadReciboConceptoRepository contabilidadReciboConceptoRepository;
    private final ContabilidadReciboRepository contabilidadReciboRepository;

    public RemesaService(
            FicheroGeneradoRepository ficheroGeneradoRepository,
            RemesaLineaRepository remesaLineaRepository,
            RemesaLineaConceptoRepository remesaLineaConceptoRepository,
            ContabilidadReciboConceptoRepository contabilidadReciboConceptoRepository,
            ContabilidadReciboRepository contabilidadReciboRepository,
            VecinoRepository vecinoRepository
    ) {
        this.ficheroGeneradoRepository = ficheroGeneradoRepository;
        this.remesaLineaRepository = remesaLineaRepository;
        this.remesaLineaConceptoRepository = remesaLineaConceptoRepository;
        this.vecinoRepository = vecinoRepository;
        this.contabilidadReciboConceptoRepository = contabilidadReciboConceptoRepository;
        this.contabilidadReciboRepository = contabilidadReciboRepository;
    }

    public FicheroGenerado crearRemesaInicial(
            Long comunidadId,
            Integer ejercicio,
            Integer mes,
            LocalDate fechaCobro,
            String origen
    ) {
        FicheroGenerado fichero = new FicheroGenerado();

        fichero.setComunidadId(comunidadId);
        fichero.setFechaCreacion(LocalDate.now());
        fichero.setFechaCobro(fechaCobro);
        fichero.setEstado("GENERADA");
        fichero.setTipoRemesa("ORDINARIA");
        fichero.setEsquemaSepa("CORE");
        fichero.setIdentificadorFichero(
                "REM-" + comunidadId + "-" + System.currentTimeMillis()
        );
        fichero.setNombreArchivo(
                "remesa_" + comunidadId + "_" + fechaCobro + ".xml"
        );
        fichero.setTotalImporte(BigDecimal.ZERO);
        fichero.setTotalDomiciliado(BigDecimal.ZERO);
        fichero.setTotalNoDomiciliado(BigDecimal.ZERO);
        fichero.setNumeroRecibos(0);
        fichero.setObservaciones(
                "Remesa ejercicio " + ejercicio +
                        ", mes " + mes +
                        ", generada desde " + origen +
                        " el " + LocalDateTime.now()
        );

        return ficheroGeneradoRepository.save(fichero);
    }

    @Transactional
    public RemesaLinea crearLineaDesdeRecibo(
            FicheroGenerado fichero,
            ContabilidadRecibo recibo
    ) {
        Vecino vecino = vecinoRepository.findById(recibo.getVecinoId())
                .orElse(null);

        boolean domiciliado = esVecinoDomiciliado(vecino);

        RemesaLinea linea = new RemesaLinea();

        linea.setRemesaId(fichero.getId());
        linea.setVecinoId(recibo.getVecinoId());
        linea.setReciboContableId(recibo.getId());
        linea.setImporte(recibo.getImporte());
        linea.setConcepto(limitarConcepto(recibo.getConcepto()));
        linea.setDomiciliado(domiciliado);
        linea.setIncluidoSepa(domiciliado);

        RemesaLinea lineaGuardada = remesaLineaRepository.save(linea);

        copiarConceptosRecibo(
                lineaGuardada,
                recibo
        );

        return lineaGuardada;
    }

    private void copiarConceptosRecibo(
            RemesaLinea linea,
            ContabilidadRecibo recibo
    ) {

        remesaLineaConceptoRepository.deleteByRemesaLineaId(
                linea.getId()
        );

        List<ContabilidadReciboConcepto> conceptos =
                contabilidadReciboConceptoRepository
                        .findByReciboIdOrderByOrdenAsc(
                                recibo.getId()
                        );

        if (conceptos.isEmpty()) {
            crearConceptoFallback(linea, recibo);
            return;
        }

        if (conceptos.size() <= 5) {
            int orden = 1;

            for (ContabilidadReciboConcepto origen : conceptos) {
                crearConceptoRemesa(
                        linea,
                        origen.getConceptoCobroId(),
                        origen.getDescripcion(),
                        origen.getImporte(),
                        orden++,
                        false
                );
            }

            return;
        }

        int orden = 1;

        for (int i = 0; i < 4; i++) {
            ContabilidadReciboConcepto origen = conceptos.get(i);

            crearConceptoRemesa(
                    linea,
                    origen.getConceptoCobroId(),
                    origen.getDescripcion(),
                    origen.getImporte(),
                    orden++,
                    false
            );
        }

        BigDecimal importeAgrupado = BigDecimal.ZERO;
        StringBuilder descripcionAgrupada = new StringBuilder();

        for (int i = 4; i < conceptos.size(); i++) {
            ContabilidadReciboConcepto origen = conceptos.get(i);

            if (origen.getImporte() != null) {
                importeAgrupado = importeAgrupado.add(origen.getImporte());
            }

            if (descripcionAgrupada.length() > 0) {
                descripcionAgrupada.append(" + ");
            }

            descripcionAgrupada.append(
                    origen.getDescripcion() == null
                            ? "Concepto"
                            : origen.getDescripcion()
            );
        }

        crearConceptoRemesa(
                linea,
                null,
                "Otros conceptos: " + descripcionAgrupada,
                importeAgrupado,
                5,
                true
        );
    }

    private void crearConceptoFallback(
            RemesaLinea linea,
            ContabilidadRecibo recibo
    ) {
        crearConceptoRemesa(
                linea,
                null,
                recibo.getConcepto(),
                recibo.getImporte(),
                1,
                false
        );
    }

    private void crearConceptoRemesa(
            RemesaLinea linea,
            Long conceptoCobroId,
            String descripcion,
            BigDecimal importe,
            Integer orden,
            Boolean agrupado
    ) {
        RemesaLineaConcepto destino =
                new RemesaLineaConcepto();

        destino.setRemesaLineaId(linea.getId());
        destino.setConceptoCobroId(conceptoCobroId);
        destino.setDescripcion(
                limitarDescripcionConcepto(descripcion)
        );
        destino.setImporte(
                importe == null ? BigDecimal.ZERO : importe
        );
        destino.setOrden(orden);
        destino.setAgrupadoEnUltimaLinea(agrupado);

        remesaLineaConceptoRepository.save(destino);
    }


    public boolean reciboYaIncluidoEnRemesa(Long reciboId) {
        return remesaLineaRepository.existsByReciboContableId(reciboId);
    }

    public boolean esReciboPendiente(ContabilidadRecibo recibo) {
        return recibo != null
                && recibo.getEstado() != null
                && "PENDIENTE".equalsIgnoreCase(recibo.getEstado());
    }

    public boolean perteneceAComunidad(
            ContabilidadRecibo recibo,
            Long comunidadId
    ) {
        return recibo != null
                && recibo.getComunidadId() != null
                && recibo.getComunidadId().equals(comunidadId);
    }

    public boolean esVecinoDomiciliado(Vecino vecino) {
        return vecino != null
                && vecino.isDomiciliado()
                && vecino.getIban() != null
                && !vecino.getIban().isBlank();
    }

    public String limitarConcepto(String concepto) {
        return limitarDescripcionConcepto(concepto);
    }

    private String limitarDescripcionConcepto(String concepto) {
        if (concepto == null || concepto.isBlank()) {
            return "Recibo comunidad";
        }

        if (concepto.length() <= 140) {
            return concepto;
        }

        return concepto.substring(0, 140);
    }

    public List<ContabilidadRecibo> eliminarRecibosYaIncluidos(
            List<ContabilidadRecibo> recibos
    ) {

        return recibos.stream()
                .filter(r -> !reciboYaIncluidoEnRemesa(r.getId()))
                .toList();

    }

    public void actualizarTotalesRemesa(
            FicheroGenerado fichero,
            BigDecimal total,
            BigDecimal totalDomiciliado,
            BigDecimal totalNoDomiciliado,
            int numeroRecibos
    ) {
        fichero.setTotalImporte(total);
        fichero.setTotalDomiciliado(totalDomiciliado);
        fichero.setTotalNoDomiciliado(totalNoDomiciliado);
        fichero.setNumeroRecibos(numeroRecibos);

        ficheroGeneradoRepository.save(fichero);
    }

    public void eliminarRemesa(FicheroGenerado fichero) {
        if (fichero != null && fichero.getId() != null) {
            ficheroGeneradoRepository.delete(fichero);
        }
    }

    public List<ContabilidadRecibo> obtenerRecibosParaRemesa(
        Long comunidadId, LocalDate fechaEmision)
    {
        return contabilidadReciboRepository
                .findByComunidadIdAndEstadoAndFechaEmisionOrderByIdAsc(
                        comunidadId,
                        "PENDIENTE",
                        fechaEmision
                );
    }
}