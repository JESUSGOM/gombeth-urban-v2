package com.gombeth.urban.service;

import com.gombeth.urban.dto.ConceptoCobroDTO;
import com.gombeth.urban.entity.ConceptoCobro;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.repository.ConceptoCobroRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<ConceptoCobroDTO> findByComunidad(Long comunidadId) {

        List<ConceptoCobro> conceptos =
                repository.findByComunidadIdOrderByDescripcionAsc(
                        comunidadId
                );

        if (conceptos.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> cuentasContablesIds = conceptos.stream()
                .map(ConceptoCobro::getCuentaContableId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, CuentaContable> cuentasPorId;

        if (cuentasContablesIds.isEmpty()) {
            cuentasPorId = Collections.emptyMap();
        } else {
            cuentasPorId = cuentaContableRepository
                    .findAllById(cuentasContablesIds)
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    CuentaContable::getId,
                                    Function.identity()
                            )
                    );
        }

        return conceptos.stream()
                .map(concepto -> mapToDTO(
                        concepto,
                        cuentasPorId.get(
                                concepto.getCuentaContableId()
                        )
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConceptoCobroDTO findById(Long id) {

        ConceptoCobro concepto = repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No existe el concepto de cobro con ID " + id
                        )
                );

        return convertirConCuenta(concepto);
    }

    @Transactional
    public ConceptoCobroDTO create(
            ConceptoCobro conceptoRecibido) {

        conceptoRecibido.setId(null);

        validarCuentaContable(
                conceptoRecibido.getCuentaContableId(),
                conceptoRecibido.getComunidadId()
        );

        /*
         * saveAndFlush garantiza que la operación SQL se ejecute
         * antes de construir la respuesta enviada a Angular.
         */
        ConceptoCobro guardado =
                repository.saveAndFlush(conceptoRecibido);

        return convertirConCuenta(guardado);
    }

    @Transactional
    public ConceptoCobroDTO update(
            Long id,
            ConceptoCobro datosRecibidos) {

        ConceptoCobro existente = repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No existe el concepto de cobro con ID " + id
                        )
                );

        /*
         * Solo se modifican los campos editables.
         *
         * Se conservan vecinoId, mesInicio,
         * movimientoBancarioId, impuestos y demás
         * información interna del concepto.
         */
        existente.setDescripcion(
                datosRecibidos.getDescripcion()
        );

        existente.setImporte(
                datosRecibidos.getImporte()
        );

        existente.setPeriodicidad(
                datosRecibidos.getPeriodicidad()
        );

        existente.setCuentaContableId(
                datosRecibidos.getCuentaContableId()
        );

        if (datosRecibidos.getActivo() != null) {
            existente.setActivo(
                    datosRecibidos.getActivo()
            );
        }

        validarCuentaContable(
                existente.getCuentaContableId(),
                existente.getComunidadId()
        );

        ConceptoCobro guardado =
                repository.saveAndFlush(existente);

        /*
         * No devolvemos la entidad JPA directamente.
         * Devolvemos un DTO sencillo, evitando problemas
         * de serialización y respuestas que no finalizan.
         */
        return convertirConCuenta(guardado);
    }

    private ConceptoCobroDTO convertirConCuenta(
            ConceptoCobro concepto) {

        CuentaContable cuentaContable = null;

        if (concepto.getCuentaContableId() != null) {
            cuentaContable = cuentaContableRepository
                    .findById(
                            concepto.getCuentaContableId()
                    )
                    .orElse(null);
        }

        return mapToDTO(
                concepto,
                cuentaContable
        );
    }

    private ConceptoCobroDTO mapToDTO(
            ConceptoCobro concepto,
            CuentaContable cuentaContable) {

        ConceptoCobroDTO dto = new ConceptoCobroDTO();

        dto.setId(concepto.getId());
        dto.setDescripcion(concepto.getDescripcion());
        dto.setImporte(concepto.getImporte());
        dto.setPeriodicidad(concepto.getPeriodicidad());
        dto.setComunidadId(concepto.getComunidadId());
        dto.setActivo(concepto.getActivo());

        dto.setCuentaContableId(
                concepto.getCuentaContableId()
        );

        if (concepto.getCuentaContableId() == null) {
            dto.setCuentaContableCodigo("Sin cuenta");
            dto.setCuentaContableNombre("Sin cuenta");

            return dto;
        }

        if (cuentaContable == null) {
            dto.setCuentaContableCodigo(
                    "ID " + concepto.getCuentaContableId()
            );

            dto.setCuentaContableNombre(
                    "Cuenta no encontrada"
            );

            return dto;
        }

        dto.setCuentaContableCodigo(
                cuentaContable.getCodigo()
        );

        dto.setCuentaContableNombre(
                cuentaContable.getNombre()
        );

        return dto;
    }

    private void validarCuentaContable(
            Long cuentaContableId,
            Long comunidadId) {

        if (cuentaContableId == null) {
            return;
        }

        CuentaContable cuenta = cuentaContableRepository
                .findById(cuentaContableId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "La cuenta contable seleccionada no existe."
                        )
                );

        if (
                comunidadId != null &&
                        cuenta.getComunidad() != null &&
                        !Objects.equals(
                                cuenta.getComunidad().getId(),
                                comunidadId
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cuenta contable seleccionada no pertenece a la comunidad del concepto."
            );
        }
    }
}