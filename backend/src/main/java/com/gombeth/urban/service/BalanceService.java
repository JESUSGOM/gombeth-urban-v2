package com.gombeth.urban.service;

import com.gombeth.urban.dto.BalanceSumaSaldoDTO;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BalanceService {

    private final ContabilidadMovimientoRepository repo;

    public BalanceService(ContabilidadMovimientoRepository repo) {
        this.repo = repo;
    }

    public List<BalanceSumaSaldoDTO> obtenerBalance(Long comunidadId) {
        return repo.obtenerBalance(comunidadId);
    }
}