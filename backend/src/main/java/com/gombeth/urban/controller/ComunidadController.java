package com.gombeth.urban.controller;

import com.gombeth.urban.dto.CoeficienteVecinoDetalleResponse;
import com.gombeth.urban.dto.CoeficientesResumenResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.QrCodeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/comunidades")
public class ComunidadController {

    private static final String
            FORMULARIO_PUBLICO_INCIDENCIAS =
            "https://jfgb.es/incidenciacomunidad/";

    private final ComunidadRepository repository;

    private final VecinoRepository vecinoRepository;

    private final QrCodeService qrCodeService;

    private final AccesoComunidadService
            accesoComunidadService;

    public ComunidadController(
            ComunidadRepository repository,
            VecinoRepository vecinoRepository,
            QrCodeService qrCodeService,
            AccesoComunidadService
                    accesoComunidadService
    ) {
        this.repository = repository;
        this.vecinoRepository = vecinoRepository;
        this.qrCodeService = qrCodeService;
        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * usuarioId y administradorId ya no se reciben.
     *
     * Aunque Angular todavía los envíe temporalmente
     * como query parameters, Spring los ignorará.
     */
    @GetMapping
    public Page<Comunidad> listar(
            Authentication authentication,
            @PageableDefault(
                    size = 10,
                    sort = "nombre",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        return accesoComunidadService
                .listarComunidades(
                        authentication,
                        pageable
                );
    }

    @GetMapping("/{id}")
    public Comunidad obtenerPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return accesoComunidadService
                .obtenerComunidadAutorizada(
                        authentication,
                        id
                );
    }

    @GetMapping(
            value = "/{id}/qr",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public ResponseEntity<byte[]> generarQrIncidencias(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Comunidad comunidad =
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                id
                        );

        String tokenQr = obtenerOCrearTokenQr(
                comunidad
        );

        String tokenCodificado = URLEncoder.encode(
                tokenQr,
                StandardCharsets.UTF_8
        );

        String urlFormulario =
                FORMULARIO_PUBLICO_INCIDENCIAS
                        + "?t="
                        + tokenCodificado;

        byte[] imagenQr =
                qrCodeService.generarQrPng(
                        urlFormulario
                );

        return ResponseEntity.ok()
                .contentType(
                        MediaType.IMAGE_PNG
                )
                .cacheControl(
                        CacheControl.noStore()
                )
                .header(
                        HttpHeaders.PRAGMA,
                        "no-cache"
                )
                .header(
                        HttpHeaders.EXPIRES,
                        "0"
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                                + "qr-comunidad-"
                                + id
                                + ".png\""
                )
                .body(imagenQr);
    }

    @PutMapping("/{id}")
    public Comunidad actualizar(
            @PathVariable Long id,
            @RequestBody Comunidad comunidadActualizada,
            Authentication authentication
    ) {
        Comunidad comunidad =
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                id
                        );

        comunidad.setNombre(
                limpiarEspacios(
                        comunidadActualizada.getNombre()
                )
        );

        comunidad.setNifCif(
                normalizarTextoSimple(
                        comunidadActualizada.getNifCif()
                )
        );

        comunidad.setDireccion(
                limpiarEspacios(
                        comunidadActualizada.getDireccion()
                )
        );

        comunidad.setCodigoPostal(
                limpiarEspacios(
                        comunidadActualizada
                                .getCodigoPostal()
                )
        );

        comunidad.setPoblacion(
                limpiarEspacios(
                        comunidadActualizada.getPoblacion()
                )
        );

        comunidad.setProvincia(
                limpiarEspacios(
                        comunidadActualizada.getProvincia()
                )
        );

        comunidad.setPaiscod(
                normalizarTextoSimple(
                        comunidadActualizada.getPaiscod()
                )
        );

        comunidad.setIban(
                normalizarIban(
                        comunidadActualizada.getIban()
                )
        );

        comunidad.setBic(
                normalizarTextoSimple(
                        comunidadActualizada.getBic()
                )
        );

        comunidad.setIdentificadorAcreedor(
                normalizarTextoSimple(
                        comunidadActualizada
                                .getIdentificadorAcreedor()
                )
        );

        comunidad.setSufijo(
                normalizarTextoSimple(
                        comunidadActualizada.getSufijo()
                )
        );

        validarDatosComunidad(comunidad);

        return repository.save(comunidad);
    }

    @GetMapping("/{id}/coeficientes/resumen")
    public CoeficientesResumenResponse
    resumenCoeficientes(
            @PathVariable Long id,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                id
        );

        BigDecimal total = vecinoRepository
                .sumarCoeficientesActivosPorComunidad(
                        id
                );

        long numeroPropietarios =
                vecinoRepository
                        .countByComunidadIdAndActivo(
                                id,
                                true
                        );

        BigDecimal cien =
                new BigDecimal("100.0000");

        boolean correcto =
                total.compareTo(cien) == 0;

        String mensaje = correcto
                ? "Los coeficientes activos suman 100."
                : "Los coeficientes activos "
                + "no suman 100.";

        return new CoeficientesResumenResponse(
                id,
                total,
                correcto,
                numeroPropietarios,
                mensaje
        );
    }

    @GetMapping("/{id}/coeficientes/detalle")
    public List<CoeficienteVecinoDetalleResponse>
    detalleCoeficientes(
            @PathVariable Long id,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                id
        );

        return vecinoRepository
                .detalleCoeficientesPorComunidad(
                        id
                );
    }

    private String obtenerOCrearTokenQr(
            Comunidad comunidad
    ) {
        String tokenActual =
                comunidad.getTokenQr();

        if (
                tokenActual != null
                        && !tokenActual.isBlank()
        ) {
            return tokenActual;
        }

        String nuevoToken = UUID.randomUUID()
                .toString()
                .replace("-", "");

        comunidad.setTokenQr(nuevoToken);

        Comunidad comunidadGuardada =
                repository.save(comunidad);

        return comunidadGuardada.getTokenQr();
    }

    private void validarDatosComunidad(
            Comunidad comunidad
    ) {
        if (
                comunidad.getNombre() == null
                        || comunidad.getNombre().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de la comunidad "
                            + "es obligatorio."
            );
        }

        String cif = comunidad.getNifCif();

        if (
                cif != null
                        && !cif.isBlank()
                        && !validarCif(cif)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CIF/NIF inválido."
            );
        }
    }

    private String normalizarTextoSimple(
            String valor
    ) {
        if (valor == null) {
            return null;
        }

        String limpio = valor
                .trim()
                .toUpperCase(Locale.ROOT);

        return limpio.isBlank()
                ? null
                : limpio;
    }

    private String normalizarIban(
            String iban
    ) {
        if (iban == null) {
            return null;
        }

        String limpio = iban
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        return limpio.isBlank()
                ? null
                : limpio;
    }

    private String limpiarEspacios(
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

    private boolean validarCif(
            String cif
    ) {
        if (
                cif == null
                        || cif.length() != 9
        ) {
            return false;
        }

        cif = cif.toUpperCase(
                Locale.ROOT
        );

        String letras =
                "ABCDEFGHJNPQRSUVW";

        if (
                !letras.contains(
                        cif.substring(0, 1)
                )
        ) {
            return false;
        }

        try {
            String digitos =
                    cif.substring(1, 8);

            int sumaPares = 0;

            for (
                    int i = 1;
                    i < digitos.length();
                    i += 2
            ) {
                sumaPares +=
                        Character.getNumericValue(
                                digitos.charAt(i)
                        );
            }

            int sumaImpares = 0;

            for (
                    int i = 0;
                    i < digitos.length();
                    i += 2
            ) {
                int doble =
                        Character.getNumericValue(
                                digitos.charAt(i)
                        ) * 2;

                sumaImpares += doble > 9
                        ? doble - 9
                        : doble;
            }

            int numeroControl =
                    (
                            10
                                    - (
                                    (
                                            sumaPares
                                                    + sumaImpares
                                    ) % 10
                            )
                    ) % 10;

            char letraControl =
                    "JABCDEFGHI".charAt(
                            numeroControl
                    );

            char ultimo = cif.charAt(8);

            return ultimo
                    == Character.forDigit(
                    numeroControl,
                    10
            )
                    || ultimo == letraControl;

        } catch (Exception excepcion) {
            return false;
        }
    }
}