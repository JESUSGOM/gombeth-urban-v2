package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CuentaContableService {

    private final CuentaContableRepository repository;
    private final ComunidadRepository comunidadRepository;

    public CuentaContableService(
            CuentaContableRepository repository,
            ComunidadRepository comunidadRepository
    ) {
        this.repository = repository;
        this.comunidadRepository = comunidadRepository;
    }

    public List<CuentaContable> findByComunidad(Long comunidadId) {
        return repository.findByComunidadId(comunidadId);
    }

    /**
     * Devuelve un catálogo global, sin duplicados por código.
     *
     * Cuando un código ya existe en la comunidad actual, se devuelve
     * precisamente la cuenta de esa comunidad. De ese modo, al editar
     * un concepto ya asignado, Angular puede mantener seleccionado su
     * cuenta_contable_id real.
     *
     * Para los códigos que todavía no existen en la comunidad actual
     * se utiliza como referencia una cuenta equivalente de cualquier
     * otra comunidad.
     */
    public List<CuentaContable> findCatalogoGlobalParaComunidad(
            Long comunidadId
    ) {
        Map<String, CuentaContable> porCodigo =
                new TreeMap<>();

        for (CuentaContable cuenta : repository.findAll()) {
            String clave = normalizarCodigo(
                    cuenta.getCodigo()
            );

            if (!clave.isEmpty()) {
                porCodigo.putIfAbsent(
                        clave,
                        cuenta
                );
            }
        }

        /*
         * Las cuentas propias de la comunidad tienen prioridad
         * sobre las referencias procedentes de otras comunidades.
         */
        for (CuentaContable cuenta :
                repository.findByComunidadId(comunidadId)) {

            String clave = normalizarCodigo(
                    cuenta.getCodigo()
            );

            if (!clave.isEmpty()) {
                porCodigo.put(
                        clave,
                        cuenta
                );
            }
        }

        return new ArrayList<>(
                porCodigo.values()
        );
    }

    /**
     * Convierte el ID seleccionado en el catálogo global en el ID
     * de la cuenta equivalente perteneciente a la comunidad actual.
     *
     * Si el código todavía no existe en dicha comunidad, crea una
     * nueva CuentaContable copiando código, nombre y tipo.
     */
    @Transactional
    public Long resolverCuentaIdParaComunidad(
            Long comunidadId,
            Long cuentaReferenciaId
    ) {
        if (cuentaReferenciaId == null) {
            return null;
        }

        if (comunidadId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad es obligatoria para asignar "
                            + "una cuenta contable."
            );
        }

        CuentaContable referencia =
                repository.findById(
                                cuentaReferenciaId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "La cuenta contable "
                                                + "seleccionada no existe."
                                )
                        );

        String codigo =
                referencia.getCodigo() == null
                        ? ""
                        : referencia.getCodigo().trim();

        if (codigo.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cuenta contable seleccionada "
                            + "no tiene código."
            );
        }

        return repository
                .findFirstByComunidad_IdAndCodigoOrderByIdAsc(
                        comunidadId,
                        codigo
                )
                .map(CuentaContable::getId)
                .orElseGet(() ->
                        crearCuentaEquivalente(
                                comunidadId,
                                referencia
                        )
                );
    }

    public CuentaContable save(CuentaContable cuenta) {
        return repository.save(cuenta);
    }

    private Long crearCuentaEquivalente(
            Long comunidadId,
            CuentaContable referencia
    ) {
        Comunidad comunidad =
                comunidadRepository.findById(
                                comunidadId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "La comunidad indicada "
                                                + "no existe."
                                )
                        );

        CuentaContable nueva =
                new CuentaContable(
                        referencia.getCodigo(),
                        referencia.getNombre(),
                        referencia.getTipo(),
                        comunidad
                );

        return repository.save(
                nueva
        ).getId();
    }

    private String normalizarCodigo(
            String codigo
    ) {
        if (codigo == null) {
            return "";
        }

        return codigo
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
