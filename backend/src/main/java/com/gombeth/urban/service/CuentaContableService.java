package com.gombeth.urban.service;

import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CuentaContableService {

    private final CuentaContableRepository repository;

    public CuentaContableService(CuentaContableRepository repository) {
        this.repository = repository;
    }

    public List<CuentaContable> findByComunidad(Long comunidadId) {
        return repository.findByComunidadId(comunidadId);
    }

    public CuentaContable save(CuentaContable cuenta) {
        return repository.save(cuenta);
    }
}