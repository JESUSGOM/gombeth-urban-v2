package com.gombeth.urban.service;

import com.gombeth.urban.dto.presentador.CuentaPresentadorRequest;
import com.gombeth.urban.dto.presentador.CuentaPresentadorResponse;
import com.gombeth.urban.entity.CuentaPresentador;
import com.gombeth.urban.repository.CuentaPresentadorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CuentaPresentadorService {

    private final CuentaPresentadorRepository repository;

    public CuentaPresentadorService(
            CuentaPresentadorRepository repository
    ) {
        this.repository = repository;
    }

    public List<CuentaPresentadorResponse> listar(
            Long administradorId
    ) {
        validarAdministradorId(administradorId);

        return repository
                .findByAdministradorIdOrderByAliasAsc(
                        administradorId
                )
                .stream()
                .map(this::convertir)
                .toList();
    }

    public List<CuentaPresentadorResponse> listarActivas(
            Long administradorId
    ) {
        validarAdministradorId(administradorId);

        return repository
                .findByAdministradorIdAndActivaTrueOrderByAliasAsc(
                        administradorId
                )
                .stream()
                .map(this::convertir)
                .toList();
    }

    public CuentaPresentadorResponse obtener(
            Long administradorId,
            Long cuentaId
    ) {
        return convertir(
                obtenerEntidadPropia(
                        administradorId,
                        cuentaId
                )
        );
    }

    public CuentaPresentador obtenerActivaPropia(
            Long administradorId,
            Long cuentaId
    ) {
        CuentaPresentador cuenta =
                obtenerEntidadPropia(
                        administradorId,
                        cuentaId
                );

        if (!cuenta.isActiva()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cuenta presentadora seleccionada "
                            + "no está activa."
            );
        }

        return cuenta;
    }

    @Transactional
    public CuentaPresentadorResponse crear(
            Long administradorId,
            CuentaPresentadorRequest request
    ) {
        validarAdministradorId(administradorId);

        CuentaPresentador cuenta =
                new CuentaPresentador();

        cuenta.setAdministradorId(
                administradorId
        );

        aplicarDatos(
                cuenta,
                request,
                true
        );

        return convertir(
                repository.save(cuenta)
        );
    }

    @Transactional
    public CuentaPresentadorResponse actualizar(
            Long administradorId,
            Long cuentaId,
            CuentaPresentadorRequest request
    ) {
        CuentaPresentador cuenta =
                obtenerEntidadPropia(
                        administradorId,
                        cuentaId
                );

        aplicarDatos(
                cuenta,
                request,
                false
        );

        return convertir(
                repository.save(cuenta)
        );
    }

    @Transactional
    public void eliminar(
            Long administradorId,
            Long cuentaId
    ) {
        CuentaPresentador cuenta =
                obtenerEntidadPropia(
                        administradorId,
                        cuentaId
                );

        repository.delete(cuenta);
    }

    private CuentaPresentador obtenerEntidadPropia(
            Long administradorId,
            Long cuentaId
    ) {
        validarAdministradorId(administradorId);

        if (cuentaId == null || cuentaId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar una cuenta presentadora válida."
            );
        }

        return repository
                .findByIdAndAdministradorId(
                        cuentaId,
                        administradorId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Cuenta presentadora no encontrada."
                        )
                );
    }

    private void aplicarDatos(
            CuentaPresentador cuenta,
            CuentaPresentadorRequest request,
            boolean nueva
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos de la cuenta presentadora "
                            + "no pueden estar vacíos."
            );
        }

        String alias =
                normalizarTexto(request.getAlias());

        if (alias == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el alias "
                            + "de la cuenta presentadora."
            );
        }

        validarLongitud(
                alias,
                100,
                "El alias"
        );

        String identificador =
                normalizarCodigo(
                        request.getIdentificadorPresentador()
                );

        if (identificador == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el identificador "
                            + "SEPA del presentador."
            );
        }

        validarLongitud(
                identificador,
                35,
                "El identificador SEPA"
        );

        String banco =
                normalizarTexto(request.getBanco());

        validarLongitud(
                banco,
                100,
                "El banco"
        );

        String nifCif =
                normalizarCodigo(request.getNifCif());

        validarLongitud(
                nifCif,
                20,
                "El NIF o CIF"
        );

        String sufijo =
                normalizarCodigo(request.getSufijo());

        if (
                sufijo != null
                        && sufijo.length() != 3
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El sufijo debe contener exactamente "
                            + "3 caracteres."
            );
        }

        String iban =
                normalizarCodigo(request.getIban());

        validarLongitud(
                iban,
                34,
                "El IBAN"
        );

        String bic =
                normalizarCodigo(request.getBic());

        if (
                bic != null
                        && bic.length() != 8
                        && bic.length() != 11
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El BIC debe contener 8 u 11 caracteres."
            );
        }

        cuenta.setAlias(alias);
        cuenta.setBanco(banco);
        cuenta.setIdentificadorPresentador(
                identificador
        );
        cuenta.setNifCif(nifCif);
        cuenta.setSufijo(sufijo);
        cuenta.setIban(iban);
        cuenta.setBic(bic);
        cuenta.setObservaciones(
                normalizarObservaciones(
                        request.getObservaciones()
                )
        );

        if (request.getActiva() != null) {
            cuenta.setActiva(
                    request.getActiva()
            );
        } else if (nueva) {
            cuenta.setActiva(true);
        }
    }

    private CuentaPresentadorResponse convertir(
            CuentaPresentador cuenta
    ) {
        return new CuentaPresentadorResponse(
                cuenta.getId(),
                cuenta.getAlias(),
                cuenta.getBanco(),
                cuenta.getIdentificadorPresentador(),
                cuenta.getNifCif(),
                cuenta.getSufijo(),
                cuenta.getIban(),
                cuenta.getBic(),
                cuenta.isActiva(),
                cuenta.getObservaciones()
        );
    }

    private void validarAdministradorId(
            Long administradorId
    ) {
        if (
                administradorId == null
                        || administradorId <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario autenticado no tiene "
                            + "administrador asociado."
            );
        }
    }

    private void validarLongitud(
            String valor,
            int longitudMaxima,
            String campo
    ) {
        if (
                valor != null
                        && valor.length() > longitudMaxima
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    campo
                            + " no puede superar "
                            + longitudMaxima
                            + " caracteres."
            );
        }
    }

    private String normalizarTexto(
            String valor
    ) {
        if (valor == null) {
            return null;
        }

        String limpio =
                valor
                        .trim()
                        .replaceAll("\\s+", " ");

        return limpio.isBlank()
                ? null
                : limpio;
    }

    private String normalizarCodigo(
            String valor
    ) {
        if (valor == null) {
            return null;
        }

        String limpio =
                valor
                        .replaceAll("\\s+", "")
                        .trim()
                        .toUpperCase(Locale.ROOT);

        return limpio.isBlank()
                ? null
                : limpio;
    }

    private String normalizarObservaciones(
            String valor
    ) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();

        return limpio.isBlank()
                ? null
                : limpio;
    }
}