package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.repository.ContabilidadGastoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContabilidadGastoService {

    private final ContabilidadGastoRepository gastoRepository;

    public ContabilidadGastoService(
            ContabilidadGastoRepository gastoRepository
    ) {
        this.gastoRepository = gastoRepository;
    }

    public List<ContabilidadGasto> listarPorComunidad(Long comunidadId) {
        return gastoRepository.findByComunidadIdOrderByFechaFacturaDescIdDesc(
                comunidadId
        );
    }

    public ContabilidadGasto crear(ContabilidadGasto gasto) {
        if (gasto.getComunidadId() == null) {
            throw new IllegalArgumentException("La comunidad es obligatoria.");
        }

        if (gasto.getPagado() == null) {
            gasto.setPagado(false);
        }

        return gastoRepository.save(gasto);
    }

    public ContabilidadGasto findById(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalStateException("No existe el gasto " + id)
                );
    }
}