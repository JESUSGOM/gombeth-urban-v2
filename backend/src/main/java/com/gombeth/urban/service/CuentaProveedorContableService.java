package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.TipoCuenta;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class CuentaProveedorContableService {

    private static final String PREFIJO_PROVEEDOR =
            "410";

    private final CuentaContableRepository
            cuentaContableRepository;

    private final ComunidadRepository
            comunidadRepository;

    public CuentaProveedorContableService(
            CuentaContableRepository cuentaContableRepository,
            ComunidadRepository comunidadRepository
    ) {
        this.cuentaContableRepository =
                cuentaContableRepository;

        this.comunidadRepository =
                comunidadRepository;
    }

    @Transactional
    public CuentaContable resolverOCrear(
            Long comunidadId,
            String nombreProveedor
    ) {
        return resolverOCrearInterno(
                comunidadId,
                nombreProveedor,
                null
        );
    }

    @Transactional
    public CuentaContable resolverOCrear(
            Long comunidadId,
            String nombreProveedor,
            String nifCif
    ) {
        return resolverOCrearInterno(
                comunidadId,
                nombreProveedor,
                nifCif
        );
    }

    private CuentaContable resolverOCrearInterno(
            Long comunidadId,
            String nombreProveedor,
            String nifCif
    ) {
        validarDatos(
                comunidadId,
                nombreProveedor
        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(
                                comunidadId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe la comunidad "
                                                + comunidadId
                                                + "."
                                )
                        );

        String nombre =
                nombreProveedor.trim();

        List<CuentaContable> cuentas =
                cuentaContableRepository
                        .findByComunidadId(
                                comunidadId
                        );

        for (CuentaContable cuenta : cuentas) {

            if (
                    esCuentaProveedor(
                            cuenta,
                            nombre
                    )
                            || esCuentaProveedorConNifConcatenado(
                            cuenta,
                            nombre,
                            nifCif
                    )
            ) {
                return cuenta;
            }
        }

        String codigo =
                generarCodigoLegacy(
                        nombre
                );

        CuentaContable cuentaMismoCodigo =
                cuentaContableRepository
                        .findFirstByComunidad_IdAndCodigoOrderByIdAsc(
                                comunidadId,
                                codigo
                        )
                        .orElse(null);

        if (cuentaMismoCodigo != null) {

            if (
                    esCuentaProveedor(
                            cuentaMismoCodigo,
                            nombre
                    )
                            || esCuentaProveedorConNifConcatenado(
                            cuentaMismoCodigo,
                            nombre,
                            nifCif
                    )
            ) {
                return cuentaMismoCodigo;
            }

            throw new IllegalStateException(
                    "El código contable "
                            + codigo
                            + " ya está utilizado por otro proveedor "
                            + "en la comunidad "
                            + comunidadId
                            + "."
            );
        }

        CuentaContable nuevaCuenta =
                new CuentaContable(
                        codigo,
                        construirNombreCuenta(
                                nombre,
                                nifCif
                        ),
                        TipoCuenta.PASIVO,
                        comunidad
                );

        return cuentaContableRepository.save(
                nuevaCuenta
        );
    }

    private String construirNombreCuenta(
            String nombreProveedor,
            String nifCif
    ) {
        StringBuilder nombre =
                new StringBuilder(
                        "PROV: "
                );

        nombre.append(
                nombreProveedor.trim()
        );

        if (
                nifCif != null
                        && !nifCif.isBlank()
        ) {
            nombre.append(
                    " CIF: "
            );

            nombre.append(
                    nifCif.trim()
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

    String generarCodigoLegacy(
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

        int numero =
                Math.abs(
                        nombreProveedor
                                .trim()
                                .hashCode()
                                % 100000
                );

        return PREFIJO_PROVEEDOR
                + String.format(
                "%05d",
                numero
        );
    }

    private boolean esCuentaProveedor(
            CuentaContable cuenta,
            String nombreProveedor
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
                normalizarNombre(
                        cuenta.getNombre()
                );

        String proveedor =
                normalizarNombre(
                        nombreProveedor
                );

        return !proveedor.isBlank()
                && proveedor.equals(
                nombreCuenta
        );
    }

    private String normalizarNombre(
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
            limpio =
                    limpio.substring(
                            0,
                            posicionCorte
                    ).trim();
        }

        String sinAcentos =
                Normalizer.normalize(
                                limpio.toUpperCase(
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

    private void validarDatos(
            Long comunidadId,
            String nombreProveedor
    ) {
        if (comunidadId == null) {
            throw new IllegalArgumentException(
                    "La comunidad es obligatoria."
            );
        }

        if (
                nombreProveedor == null
                        || nombreProveedor.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El proveedor es obligatorio."
            );
        }
    }

    private boolean esCuentaProveedorConNifConcatenado(
            CuentaContable cuenta,
            String nombreProveedor,
            String nifCif
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
                normalizarNombre(
                        cuenta.getNombre()
                );

        String proveedor =
                normalizarNombre(
                        nombreProveedor
                );

        String nif =
                normalizarSoloAlfanumerico(
                        nifCif
                );

        return !proveedor.isBlank()
                && !nif.isBlank()
                && nombreCuenta.equals(
                proveedor + nif
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
}
