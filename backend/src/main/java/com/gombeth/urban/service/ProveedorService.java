package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.Proveedor;
import com.gombeth.urban.entity.ProveedorComunidad;
import com.gombeth.urban.entity.TipoCuenta;
import com.gombeth.urban.repository.AdministradorRepository;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import com.gombeth.urban.repository.ProveedorComunidadRepository;
import com.gombeth.urban.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ProveedorService {

    private static final String PREFIJO_PROVEEDOR =
            "410";

    private final ProveedorRepository
            proveedorRepository;

    private final ProveedorComunidadRepository
            proveedorComunidadRepository;

    private final ComunidadRepository
            comunidadRepository;

    private final CuentaContableRepository
            cuentaContableRepository;

    private final AdministradorRepository
            administradorRepository;

    public ProveedorService(
            ProveedorRepository proveedorRepository,
            ProveedorComunidadRepository
                    proveedorComunidadRepository,
            ComunidadRepository comunidadRepository,
            CuentaContableRepository cuentaContableRepository,
            AdministradorRepository administradorRepository
    ) {
        this.proveedorRepository =
                proveedorRepository;

        this.proveedorComunidadRepository =
                proveedorComunidadRepository;

        this.comunidadRepository =
                comunidadRepository;

        this.cuentaContableRepository =
                cuentaContableRepository;

        this.administradorRepository =
                administradorRepository;
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
                obtenerOCrearCuentaProveedorCompatible(
                        proveedor,
                        comunidad
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

    private CuentaContable obtenerOCrearCuentaProveedorCompatible(
            Proveedor proveedor,
            Comunidad comunidad
    ) {
        if (
                proveedor == null
                        || proveedor.getNombre() == null
                        || proveedor.getNombre().isBlank()
        ) {
            throw new IllegalStateException(
                    "El proveedor no tiene un nombre válido."
            );
        }

        /*
         * PRIMERA OPCIÓN:
         *
         * Reutilizar una cuenta 410 histórica que ya pertenezca
         * a este proveedor en esta comunidad.
         *
         * Esto evita duplicar las cuentas creadas previamente
         * por el programa antiguo.
         */
        List<CuentaContable> cuentasComunidad =
                cuentaContableRepository
                        .findByComunidadId(
                                comunidad.getId()
                        );

        for (CuentaContable cuenta : cuentasComunidad) {

            if (
                    esCuentaDelProveedor(
                            cuenta,
                            proveedor
                    )
            ) {
                return cuenta;
            }
        }

        /*
         * SEGUNDA OPCIÓN:
         *
         * Utilizamos exactamente el mismo algoritmo que utiliza
         * el programa antiguo para crear las cuentas 410.
         *
         *     410 + hashCode(nombre) módulo 100000
         *
         * De esta forma ambas aplicaciones generan el mismo
         * código cuando reciben exactamente el mismo nombre.
         */
        String codigoLegacy =
                generarCodigoProveedorLegacy(
                        proveedor.getNombre()
                );

        CuentaContable cuentaMismoCodigo =
                cuentaContableRepository
                        .findFirstByComunidad_IdAndCodigoOrderByIdAsc(
                                comunidad.getId(),
                                codigoLegacy
                        )
                        .orElse(null);

        if (cuentaMismoCodigo != null) {

            if (
                    esCuentaDelProveedor(
                            cuentaMismoCodigo,
                            proveedor
                    )
            ) {
                return cuentaMismoCodigo;
            }

            /*
             * Protección adicional.
             *
             * El algoritmo histórico utiliza solamente 5 cifras
             * derivadas del hash, por lo que teóricamente podría
             * producirse una colisión entre dos proveedores.
             *
             * No reutilizamos silenciosamente una cuenta de otro
             * proveedor.
             */
            throw new IllegalStateException(
                    "El código contable "
                            + codigoLegacy
                            + " ya está utilizado por otro proveedor "
                            + "en la comunidad "
                            + comunidad.getId()
                            + "."
            );
        }

        CuentaContable nuevaCuenta =
                new CuentaContable(
                        codigoLegacy,
                        construirNombreCuenta(
                                proveedor
                        ),
                        TipoCuenta.PASIVO,
                        comunidad
                );

        return cuentaContableRepository.save(
                nuevaCuenta
        );
    }

    private String generarCodigoProveedorLegacy(
            String nombreProveedor
    ) {
        if (
                nombreProveedor == null
                        || nombreProveedor.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El nombre del proveedor es obligatorio."
            );
        }

        String nombre =
                nombreProveedor.trim();

        int numero =
                Math.abs(
                        nombre.hashCode()
                                % 100000
                );

        return PREFIJO_PROVEEDOR
                + String.format(
                "%05d",
                numero
        );
    }

    private boolean esCuentaDelProveedor(
            CuentaContable cuenta,
            Proveedor proveedor
    ) {
        if (
                cuenta == null
                        || cuenta.getCodigo() == null
                        || !cuenta.getCodigo()
                        .startsWith(
                                PREFIJO_PROVEEDOR
                        )
                        || cuenta.getNombre() == null
        ) {
            return false;
        }

        String nombreCuenta =
                normalizarNombreContable(
                        cuenta.getNombre()
                );

        String nombreProveedor =
                normalizarNombreContable(
                        proveedor.getNombre()
                );

        if (
                !nombreProveedor.isBlank()
                        && nombreCuenta.equals(
                        nombreProveedor
                )
        ) {
            return true;
        }

        /*
         * Algunos proveedores históricos tienen el NIF/CIF
         * incorporado directamente al texto del nombre.
         *
         * Ejemplo:
         *
         * MIGUEL VILLAR RAMOS 11951496Y
         */
        String nif =
                normalizarSoloAlfanumerico(
                        proveedor.getNifCif()
                );

        return !nombreProveedor.isBlank()
                && !nif.isBlank()
                && nombreCuenta.equals(
                nombreProveedor + nif
        );
    }

    private String normalizarNombreContable(
            String valor
    ) {
        if (valor == null) {
            return "";
        }

        String limpio =
                valor.trim();

        if (
                limpio.length() >= 5
                        && limpio.regionMatches(
                        true,
                        0,
                        "PROV:",
                        0,
                        5
                )
        ) {
            limpio =
                    limpio.substring(
                            5
                    ).trim();
        }

        String mayusculas =
                limpio.toUpperCase(
                        Locale.ROOT
                );

        int posicionCif =
                mayusculas.indexOf(
                        " CIF:"
                );

        int posicionNif =
                mayusculas.indexOf(
                        " NIF:"
                );

        int posicionCorte =
                -1;

        if (posicionCif >= 0) {
            posicionCorte =
                    posicionCif;
        }

        if (
                posicionNif >= 0
                        && (
                        posicionCorte < 0
                                || posicionNif < posicionCorte
                )
        ) {
            posicionCorte =
                    posicionNif;
        }

        if (posicionCorte >= 0) {
            mayusculas =
                    mayusculas.substring(
                            0,
                            posicionCorte
                    );
        }

        return normalizarSoloAlfanumerico(
                mayusculas
        );
    }

    private String normalizarSoloAlfanumerico(
            String valor
    ) {
        if (valor == null) {
            return "";
        }

        String sinAcentos =
                Normalizer.normalize(
                                valor.toUpperCase(
                                        Locale.ROOT
                                ),
                                Normalizer.Form.NFD
                        )
                        .replaceAll(
                                "\\p{M}+",
                                ""
                        );

        return sinAcentos.replaceAll(
                "[^A-Z0-9]",
                ""
        );
    }

    private String construirNombreCuenta(
            Proveedor proveedor
    ) {
        StringBuilder nombre =
                new StringBuilder(
                        "PROV: "
                );

        nombre.append(
                proveedor.getNombre()
        );

        if (
                proveedor.getNifCif() != null
                        && !proveedor.getNifCif().isBlank()
        ) {
            nombre.append(
                    " CIF: "
            );

            nombre.append(
                    proveedor.getNifCif()
            );
        }

        /*
         * contabilidad_cuentas.nombre utiliza VARCHAR(255).
         */
        if (nombre.length() > 255) {
            return nombre
                    .substring(
                            0,
                            255
                    );
        }

        return nombre.toString();
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