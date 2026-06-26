package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.FicheroGeneradoRepository;
import com.gombeth.urban.repository.RemesaLineaRepository;
import com.gombeth.urban.repository.VecinoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class RemesaService {

    private final FicheroGeneradoRepository ficheroGeneradoRepository;
    private final RemesaLineaRepository remesaLineaRepository;
    private final VecinoRepository vecinoRepository;

    public RemesaService(
            FicheroGeneradoRepository ficheroGeneradoRepository,
            RemesaLineaRepository remesaLineaRepository,
            VecinoRepository vecinoRepository
    ) {
        this.ficheroGeneradoRepository = ficheroGeneradoRepository;
        this.remesaLineaRepository = remesaLineaRepository;
        this.vecinoRepository = vecinoRepository;
    }

    public FicheroGenerado crearRemesaInicial(
            Long comunidadId,
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
                "Remesa CORE generada desde " + origen + " el " + LocalDateTime.now()
        );

        return ficheroGeneradoRepository.save(fichero);
    }

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

        return remesaLineaRepository.save(linea);
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
        if (concepto == null || concepto.isBlank()) {
            return "Recibo comunidad";
        }

        if (concepto.length() <= 140) {
            return concepto;
        }

        return concepto.substring(0, 140);
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
}