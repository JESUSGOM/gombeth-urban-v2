package com.gombeth.urban.service;

import com.gombeth.urban.dto.ConceptoCobroDTO;
import com.gombeth.urban.entity.ConceptoCobro;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.repository.ConceptoCobroRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConceptoCobroService {

    private final ConceptoCobroRepository repository;
    private final CuentaContableRepository cuentaContableRepository;

    public ConceptoCobroService(
            ConceptoCobroRepository repository,
            CuentaContableRepository cuentaContableRepository) {
        this.repository = repository;
        this.cuentaContableRepository = cuentaContableRepository;
    }

    public List<ConceptoCobroDTO> findByComunidad(Long comunidadId) {
        return repository.findByComunidadId(comunidadId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private ConceptoCobroDTO mapToDTO(ConceptoCobro c) {

        ConceptoCobroDTO dto = new ConceptoCobroDTO();

        dto.setId(c.getId());
        dto.setDescripcion(c.getDescripcion());
        dto.setImporte(c.getImporte());
        dto.setPeriodicidad(c.getPeriodicidad());
        dto.setComunidadId(c.getComunidadId());
        dto.setActivo(c.getActivo());
        dto.setCuentaContableId(c.getCuentaContableId());

        // ==========================================
        // CUENTA CONTABLE (NOMBRE + CÓDIGO)
        // ==========================================
        if (c.getCuentaContableId() != null) {

            cuentaContableRepository.findById(c.getCuentaContableId())
                    .ifPresent(cc -> {
                        dto.setCuentaContableNombre(cc.getNombre());
                        dto.setCuentaContableCodigo(cc.getCodigo());
                    });
        }

        return dto;
    }

    public ConceptoCobro save(ConceptoCobro c) {
        return repository.save(c);
    }
}