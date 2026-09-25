package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.Proveedor;
import com.gombeth.urban.entity.ProveedorComunidad;
import com.gombeth.urban.repository.AdministradorRepository;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ProveedorComunidadRepository;
import com.gombeth.urban.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ProveedorService {

    private final ProveedorRepository
            proveedorRepository;

    private final ProveedorComunidadRepository
            proveedorComunidadRepository;

    private final ComunidadRepository
            comunidadRepository;

    private final AdministradorRepository
            administradorRepository;

    private final CuentaProveedorContableService
            cuentaProveedorContableService;

    public ProveedorService(
            ProveedorRepository proveedorRepository,
            ProveedorComunidadRepository
                    proveedorComunidadRepository,
            ComunidadRepository comunidadRepository,
            AdministradorRepository administradorRepository,
            CuentaProveedorContableService cuentaProveedorContableService
    ) {
        this.proveedorRepository =
                proveedorRepository;

        this.proveedorComunidadRepository =
                proveedorComunidadRepository;

        this.comunidadRepository =
                comunidadRepository;

        this.administradorRepository =
                administradorRepository;

        this.cuentaProveedorContableService =
                cuentaProveedorContableService;
    }

    @Transactional
    public Proveedor crearProveedor(
            Long administradorId,
            String nombre,
            String nifCif,
            String telefono,
            String email,
            String observaciones
    ) {
        if (administradorId == null) {
            throw new IllegalArgumentException(
                    "El administrador es obligatorio."
            );
        }

        if (
                nombre == null
                        || nombre.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El nombre del proveedor es obligatorio."
            );
        }

        if (
                !administradorRepository.existsById(
                        administradorId
                )
        ) {
            throw new IllegalArgumentException(
                    "El administrador indicado no existe."
            );
        }

        String nifNormalizado =
                normalizarNif(
                        nifCif
                );

        if (nifNormalizado != null) {

            boolean existe =
                    proveedorRepository
                            .findByAdministradorIdAndNifCifIgnoreCase(
                                    administradorId,
                                    nifNormalizado
                            )
                            .isPresent();

            if (existe) {
                throw new IllegalStateException(
                        "Ya existe un proveedor con NIF/CIF "
                                + nifNormalizado
                                + " para este administrador."
                );
            }
        }

        Proveedor proveedor =
                new Proveedor();

        proveedor.setAdministradorId(
                administradorId
        );

        proveedor.setNombre(
                nombre.trim()
        );

        proveedor.setNifCif(
                nifNormalizado
        );

        proveedor.setTelefono(
                limpiarTexto(
                        telefono
                )
        );

        proveedor.setEmail(
                limpiarTexto(
                        email
                )
        );

        proveedor.setObservaciones(
                limpiarTexto(
                        observaciones
                )
        );

        proveedor.setActivo(
                true
        );

        return proveedorRepository.save(
                proveedor
        );
    }

    public Proveedor obtenerPorAdministrador(
            Long administradorId,
            Long proveedorId
    ) {
        if (
                administradorId == null
                        || administradorId <= 0
        ) {
            throw new IllegalArgumentException(
                    "El administrador es obligatorio."
            );
        }

        if (
                proveedorId == null
                        || proveedorId <= 0
        ) {
            throw new IllegalArgumentException(
                    "El proveedor es obligatorio."
            );
        }

        return proveedorRepository
                .findByIdAndAdministradorId(
                        proveedorId,
                        administradorId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El proveedor indicado no existe "
                                        + "para este administrador."
                        )
                );
    }

    @Transactional
    public Proveedor actualizarProveedor(
            Long administradorId,
            Long proveedorId,
            String nombre,
            String nifCif,
            String telefono,
            String email,
            String observaciones,
            Boolean activo
    ) {
        Proveedor proveedor =
                obtenerPorAdministrador(
                        administradorId,
                        proveedorId
                );

        if (
                nombre == null
                        || nombre.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El nombre del proveedor es obligatorio."
            );
        }

        String nifNormalizado =
                normalizarNif(
                        nifCif
                );

        if (nifNormalizado != null) {

            Proveedor proveedorMismoNif =
                    proveedorRepository
                            .findByAdministradorIdAndNifCifIgnoreCase(
                                    administradorId,
                                    nifNormalizado
                            )
                            .orElse(null);

            if (
                    proveedorMismoNif != null
                            && !Objects.equals(
                            proveedorMismoNif.getId(),
                            proveedorId
                    )
            ) {
                throw new IllegalStateException(
                        "Ya existe un proveedor con NIF/CIF "
                                + nifNormalizado
                                + " para este administrador."
                );
            }
        }

        proveedor.setNombre(
                nombre.trim()
        );

        proveedor.setNifCif(
                nifNormalizado
        );

        proveedor.setTelefono(
                limpiarTexto(
                        telefono
                )
        );

        proveedor.setEmail(
                limpiarTexto(
                        email
                )
        );

        proveedor.setObservaciones(
                limpiarTexto(
                        observaciones
                )
        );

        if (activo != null) {
            proveedor.setActivo(
                    activo
            );
        }

        return proveedorRepository.save(
                proveedor
        );
    }

    @Transactional
    public ProveedorComunidad asociarAComunidad(
            Long proveedorId,
            Long comunidadId
    ) {
        if (proveedorId == null) {
            throw new IllegalArgumentException(
                    "El proveedor es obligatorio."
            );
        }

        if (comunidadId == null) {
            throw new IllegalArgumentException(
                    "La comunidad es obligatoria."
            );
        }

        Proveedor proveedor =
                proveedorRepository
                        .findById(
                                proveedorId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El proveedor indicado no existe."
                                )
                        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(
                                comunidadId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La comunidad indicada no existe."
                                )
                        );

        if (
                !Objects.equals(
                        proveedor.getAdministradorId(),
                        comunidad.getAdministradorId()
                )
        ) {
            throw new IllegalStateException(
                    "El proveedor y la comunidad pertenecen "
                            + "a administradores distintos."
            );
        }

        ProveedorComunidad existente =
                proveedorComunidadRepository
                        .findByProveedorIdAndComunidadId(
                                proveedorId,
                                comunidadId
                        )
                        .orElse(null);

        if (existente != null) {

            if (
                    !Boolean.TRUE.equals(
                            existente.getActivo()
                    )
            ) {
                existente.setActivo(
                        true
                );

                return proveedorComunidadRepository.save(
                        existente
                );
            }

            return existente;
        }

        CuentaContable cuentaProveedor =
                cuentaProveedorContableService
                        .resolverOCrear(
                                comunidad.getId(),
                                proveedor.getNombre(),
                                proveedor.getNifCif()
                        );

        ProveedorComunidad asociacion =
                new ProveedorComunidad();

        asociacion.setProveedorId(
                proveedor.getId()
        );

        asociacion.setComunidadId(
                comunidad.getId()
        );

        asociacion.setCuentaContableId(
                cuentaProveedor.getId()
        );

        asociacion.setActivo(
                true
        );

        return proveedorComunidadRepository.save(
                asociacion
        );
    }

    public List<Proveedor> listarPorAdministrador(
            Long administradorId
    ) {
        if (administradorId == null) {
            throw new IllegalArgumentException(
                    "El administrador es obligatorio."
            );
        }

        return proveedorRepository
                .findByAdministradorIdAndActivoTrueOrderByNombreAsc(
                        administradorId
                );
    }

    public List<ProveedorComunidad> listarPorComunidad(
            Long comunidadId
    ) {
        if (comunidadId == null) {
            throw new IllegalArgumentException(
                    "La comunidad es obligatoria."
            );
        }

        return proveedorComunidadRepository
                .findByComunidadIdAndActivoTrueOrderByIdAsc(
                        comunidadId
                );
    }

    public Proveedor obtenerProveedorActivoPorAsociacion(
            Long comunidadId,
            Long proveedorComunidadId
    ) {
        if (
                comunidadId == null
                        || comunidadId <= 0
        ) {
            throw new IllegalArgumentException(
                    "La comunidad es obligatoria."
            );
        }

        if (
                proveedorComunidadId == null
                        || proveedorComunidadId <= 0
        ) {
            throw new IllegalArgumentException(
                    "La asociación de proveedor es obligatoria."
            );
        }

        ProveedorComunidad asociacion =
                proveedorComunidadRepository
                        .findByIdAndComunidadId(
                                proveedorComunidadId,
                                comunidadId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "La asociación de proveedor indicada "
                                                + "no pertenece a la comunidad."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        asociacion.getActivo()
                )
        ) {
            throw new IllegalArgumentException(
                    "La asociación de proveedor está inactiva."
            );
        }

        Proveedor proveedor =
                proveedorRepository
                        .findById(
                                asociacion.getProveedorId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "El proveedor asociado no existe."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        proveedor.getActivo()
                )
        ) {
            throw new IllegalArgumentException(
                    "El proveedor asociado está inactivo."
            );
        }

        return proveedor;
    }

    private String normalizarNif(
            String nifCif
    ) {
        String valor =
                limpiarTexto(
                        nifCif
                );

        if (valor == null) {
            return null;
        }

        return valor
                .replace(" ", "")
                .replace("-", "")
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private String limpiarTexto(
            String valor
    ) {
        if (valor == null) {
            return null;
        }

        String limpio =
                valor.trim();

        return limpio.isEmpty()
                ? null
                : limpio;
    }
}