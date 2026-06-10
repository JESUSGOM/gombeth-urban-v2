package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.repository.ComunidadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import com.gombeth.urban.dto.CoeficientesResumenResponse;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.dto.CoeficienteVecinoDetalleResponse;

import java.util.List;

import java.math.BigDecimal;

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
    public Comunidad obtenerPorId(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada con ID: " + id));
    }

    @PutMapping("/{id}")
    public Comunidad actualizar(
            @PathVariable Long id,
            @RequestBody Comunidad comunidadActualizada
    ) {

        Comunidad comunidad = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Comunidad no encontrada con ID: " + id
                        )
                );

        comunidad.setNombre(comunidadActualizada.getNombre());
        comunidad.setNifCif(comunidadActualizada.getNifCif());
        comunidad.setDireccion(comunidadActualizada.getDireccion());
        comunidad.setCodigoPostal(comunidadActualizada.getCodigoPostal());
        comunidad.setPoblacion(comunidadActualizada.getPoblacion());
        comunidad.setProvincia(comunidadActualizada.getProvincia());
        comunidad.setPaiscod(comunidadActualizada.getPaiscod());
        comunidad.setIban(comunidadActualizada.getIban());
        comunidad.setBic(comunidadActualizada.getBic());
        comunidad.setIdentificadorAcreedor(
                comunidadActualizada.getIdentificadorAcreedor()
        );
        comunidad.setSufijo(comunidadActualizada.getSufijo());

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
}