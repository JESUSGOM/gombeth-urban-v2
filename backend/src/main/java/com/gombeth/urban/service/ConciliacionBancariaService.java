package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ConciliacionBancariaService {

    private final MovimientoBancarioRepository movimientoBancarioRepository;
    private final ContabilidadReciboRepository reciboRepository;
    private final ContabilidadAutomaticaService contabilidadAutomaticaService;

    public ConciliacionBancariaService(
            MovimientoBancarioRepository movimientoBancarioRepository,
            ContabilidadReciboRepository reciboRepository,
            ContabilidadAutomaticaService contabilidadAutomaticaService
    ) {
        this.movimientoBancarioRepository = movimientoBancarioRepository;
        this.reciboRepository = reciboRepository;
        this.contabilidadAutomaticaService = contabilidadAutomaticaService;
    }

    public int conciliarAutomaticamenteComunidad(Long comunidadId) {

        List<MovimientoBancario> movimientos =
                movimientoBancarioRepository.findByComunidadIdOrderByFechaOperacionAscIdAsc(
                        comunidadId
                );

        int conciliados = 0;

        for (MovimientoBancario movimiento : movimientos) {

            if (Boolean.TRUE.equals(movimiento.getConciliado())) {
                continue;
            }

            boolean conciliado = intentarConciliarMovimiento(movimiento);

            if (conciliado) {
                conciliados++;
            }
        }

        return conciliados;
    }

    public boolean intentarConciliarMovimiento(MovimientoBancario movimiento) {

        if (movimiento == null || movimiento.getComunidadId() == null) {
            return false;
        }

        if (Boolean.TRUE.equals(movimiento.getConciliado())) {
            return false;
        }

        if (movimiento.getImporte() == null ||
                movimiento.getImporte().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        List<ContabilidadRecibo> candidatos =
                reciboRepository.findByComunidadIdAndEstado(
                        movimiento.getComunidadId(),
                        "PENDIENTE"
                );

        List<ContabilidadRecibo> mismoImporte =
                candidatos.stream()
                        .filter(recibo -> recibo.getImporte() != null)
                        .filter(recibo -> recibo.getImporte().compareTo(movimiento.getImporte()) == 0)
                        .toList();

        if (mismoImporte.size() != 1) {
            return false;
        }

        ContabilidadRecibo recibo = mismoImporte.get(0);

        recibo.setEstado("COBRADO");
        recibo.setFechaCobroBanco(movimiento.getFechaOperacion());
        recibo.setMovimientoBancarioId(movimiento.getId());
        recibo.setPagadoAcumulado(recibo.getImporte());

        reciboRepository.save(recibo);

        contabilidadAutomaticaService.registrarCobroRecibo(
                recibo,
                movimiento
        );

        movimiento.setConciliado(true);
        movimiento.setProcesado(true);

        movimientoBancarioRepository.save(movimiento);

        return true;
    }
}