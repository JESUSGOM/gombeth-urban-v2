package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.repository.ContabilidadReciboConceptoRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RegeneracionRecibosService {

    private final ContabilidadReciboRepository reciboRepository;
    private final ContabilidadReciboConceptoRepository reciboConceptoRepository;
    private final ContabilidadAsientoRepository asientoRepository;
    private final ContabilidadMovimientoRepository movimientoRepository;

    public RegeneracionRecibosService(
            ContabilidadReciboRepository reciboRepository,
            ContabilidadReciboConceptoRepository reciboConceptoRepository,
            ContabilidadAsientoRepository asientoRepository,
            ContabilidadMovimientoRepository movimientoRepository
    ) {
        this.reciboRepository = reciboRepository;
        this.reciboConceptoRepository = reciboConceptoRepository;
        this.asientoRepository = asientoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Transactional
    public int borrarRecibosPeriodo(
            Long comunidadId,
            Integer anio,
            Integer mes
    ) {
        LocalDate fechaDesde = LocalDate.of(anio, mes, 1);
        LocalDate fechaHasta = fechaDesde.withDayOfMonth(
                fechaDesde.lengthOfMonth()
        );

        List<ContabilidadRecibo> recibos =
                reciboRepository
                        .findByComunidadIdAndFechaEmisionBetweenOrderByFechaEmisionAscIdAsc(
                                comunidadId,
                                fechaDesde,
                                fechaHasta
                        );

        int borrados = 0;

        for (ContabilidadRecibo recibo : recibos) {
            borrarReciboCompleto(recibo);
            borrados++;
        }

        return borrados;
    }

    @Transactional
    public void borrarReciboCompleto(
            ContabilidadRecibo recibo
    ) {
        if (recibo == null || recibo.getId() == null) {
            return;
        }

        Long comunidadId = recibo.getComunidadId();
        Long reciboId = recibo.getId();

        reciboConceptoRepository.deleteByReciboId(reciboId);

        asientoRepository
                .findByComunidadIdAndOrigenAndOrigenId(
                        comunidadId,
                        "RECIBO_EMITIDO",
                        reciboId
                )
                .ifPresent(asiento -> borrarAsientoCompleto(
                        comunidadId,
                        asiento
                ));

        reciboRepository.deleteById(reciboId);
    }

    private void borrarAsientoCompleto(
            Long comunidadId,
            ContabilidadAsiento asiento
    ) {
        if (asiento == null) {
            return;
        }

        String numeroAsiento =
                String.valueOf(asiento.getNumeroAsiento());

        movimientoRepository.deleteByComunidadIdAndNumeroAsiento(
                comunidadId,
                numeroAsiento
        );

        asientoRepository.deleteByComunidadIdAndOrigenAndOrigenId(
                comunidadId,
                asiento.getOrigen(),
                asiento.getOrigenId()
        );
    }
}