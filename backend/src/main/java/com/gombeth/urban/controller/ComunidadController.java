package com.gombeth.urban.controller;

import com.gombeth.urban.dto.CoeficienteVecinoDetalleResponse;
import com.gombeth.urban.dto.CoeficientesResumenResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.VecinoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/comunidades")
public class ComunidadController {

    private final ComunidadRepository repository;
    private final VecinoRepository vecinoRepository;

    public ComunidadController(
            ComunidadRepository repository,
            VecinoRepository vecinoRepository
    ) {
        this.repository = repository;
        this.vecinoRepository = vecinoRepository;
    }

    @GetMapping
    public Page<Comunidad> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long administradorId,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        if (usuarioId != null) {
            return repository.findByUsuarioId(usuarioId, pageable);
        }

        if (administradorId != null) {
            return repository.findByAdministradorId(administradorId, pageable);
        }

        return repository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public Comunidad obtenerPorId(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long administradorId
    ) {
        validarAcceso(id, usuarioId, administradorId);

        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comunidad no encontrada con ID: " + id
                ));
    }

    @PutMapping("/{id}")
    public Comunidad actualizar(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long administradorId,
            @RequestBody Comunidad comunidadActualizada
    ) {
        validarAcceso(id, usuarioId, administradorId);

        Comunidad comunidad = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comunidad no encontrada con ID: " + id
                ));

        comunidad.setNombre(limpiarEspacios(comunidadActualizada.getNombre()));
        comunidad.setNifCif(normalizarTextoSimple(comunidadActualizada.getNifCif()));
        comunidad.setDireccion(limpiarEspacios(comunidadActualizada.getDireccion()));
        comunidad.setCodigoPostal(limpiarEspacios(comunidadActualizada.getCodigoPostal()));
        comunidad.setPoblacion(limpiarEspacios(comunidadActualizada.getPoblacion()));
        comunidad.setProvincia(limpiarEspacios(comunidadActualizada.getProvincia()));
        comunidad.setPaiscod(normalizarTextoSimple(comunidadActualizada.getPaiscod()));
        comunidad.setIban(normalizarIban(comunidadActualizada.getIban()));
        comunidad.setBic(normalizarTextoSimple(comunidadActualizada.getBic()));
        comunidad.setIdentificadorAcreedor(
                normalizarTextoSimple(comunidadActualizada.getIdentificadorAcreedor())
        );
        comunidad.setSufijo(normalizarTextoSimple(comunidadActualizada.getSufijo()));

        validarDatosComunidad(comunidad);

        return repository.save(comunidad);
    }

    @GetMapping("/{id}/coeficientes/resumen")
    public CoeficientesResumenResponse resumenCoeficientes(
            @PathVariable Long id
    ) {
        BigDecimal total = vecinoRepository
                .sumarCoeficientesActivosPorComunidad(id);

        long numeroPropietarios = vecinoRepository
                .countByComunidadIdAndActivo(id, true);

        BigDecimal cien = new BigDecimal("100.0000");

        boolean correcto = total.compareTo(cien) == 0;

        String mensaje = correcto
                ? "Los coeficientes activos suman 100."
                : "Los coeficientes activos no suman 100.";

        return new CoeficientesResumenResponse(
                id,
                total,
                correcto,
                numeroPropietarios,
                mensaje
        );
    }

    @GetMapping("/{id}/coeficientes/detalle")
    public List<CoeficienteVecinoDetalleResponse> detalleCoeficientes(
            @PathVariable Long id
    ) {
        return vecinoRepository.detalleCoeficientesPorComunidad(id);
    }

    private void validarAcceso(
            Long comunidadId,
            Long usuarioId,
            Long administradorId
    ) {
        if (usuarioId != null) {
            boolean existe = repository.existsByIdAndUsuarioId(comunidadId, usuarioId);
            if (!existe) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tiene permisos para acceder a esta comunidad."
                );
            }
            return;
        }

        if (administradorId != null) {
            boolean existe = repository.existsByIdAndAdministradorId(comunidadId, administradorId);
            if (!existe) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tiene permisos para acceder a esta comunidad."
                );
            }
        }
    }

    private void validarDatosComunidad(Comunidad comunidad) {
        if (comunidad.getNombre() == null || comunidad.getNombre().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de la comunidad es obligatorio."
            );
        }

        String cif = comunidad.getNifCif();

        if (cif != null && !cif.isBlank() && !validarCif(cif)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CIF/NIF inválido."
            );
        }
    }

    private String normalizarTextoSimple(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim().toUpperCase(Locale.ROOT);

        return limpio.isBlank() ? null : limpio;
    }

    private String normalizarIban(String iban) {
        if (iban == null) {
            return null;
        }

        String limpio = iban
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        return limpio.isBlank() ? null : limpio;
    }

    private String limpiarEspacios(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();

        return limpio.isBlank() ? null : limpio;
    }

    private boolean validarCif(String cif) {
        if (cif == null || cif.length() != 9) {
            return false;
        }

        cif = cif.toUpperCase(Locale.ROOT);
        String letras = "ABCDEFGHJNPQRSUVW";

        if (!letras.contains(cif.substring(0, 1))) {
            return false;
        }

        try {
            String digitos = cif.substring(1, 8);

            int sumaPares = 0;
            for (int i = 1; i < digitos.length(); i += 2) {
                sumaPares += Character.getNumericValue(digitos.charAt(i));
            }

            int sumaImpares = 0;
            for (int i = 0; i < digitos.length(); i += 2) {
                int doble = Character.getNumericValue(digitos.charAt(i)) * 2;
                sumaImpares += (doble > 9) ? (doble - 9) : doble;
            }

            int numControl = (10 - ((sumaPares + sumaImpares) % 10)) % 10;
            char letraControl = "JABCDEFGHI".charAt(numControl);
            char ultimo = cif.charAt(8);

            return ultimo == Character.forDigit(numControl, 10)
                    || ultimo == letraControl;

        } catch (Exception e) {
            return false;
        }
    }
}
