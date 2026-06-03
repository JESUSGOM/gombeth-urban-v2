package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.repository.ComunidadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comunidades")
public class ComunidadController {

    private final ComunidadRepository repository;

    public ComunidadController(ComunidadRepository repository) {
        this.repository = repository;
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
}