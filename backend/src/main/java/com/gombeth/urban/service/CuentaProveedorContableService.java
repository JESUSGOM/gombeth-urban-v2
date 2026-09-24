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

        /*
         * 1. Intentamos reutilizar una cuenta 410 existente
         *    cuyo nombre corresponda al proveedor.
         *
         * Esto permite aprovechar las cuentas creadas por
         * el programa antiguo.
         */
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
            ) {
                return cuenta;
            }
        }

        /*
         * 2. Calculamos exactamente el mismo código que
         *    utiliza el programa antiguo:
         *
         * 410 + 5 cifras obtenidas del hashCode del nombre.
         */
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

        /*
         * 3. Si todavía no existe, se crea de forma
         *    compatible con el programa antiguo.
         */
        CuentaContable nuevaCuenta =
                new CuentaContable(
                        codigo,
                        "PROV: " + nombre,
                        TipoCuenta.PASIVO,
                        comunidad
                );

        return cuentaContableRepository.save(
                nuevaCuenta
        );
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
}