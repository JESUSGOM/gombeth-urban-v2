package com.gombeth.urban.controller;

import com.gombeth.urban.dto.GestionIncidenciaResponse;
import com.gombeth.urban.entity.GestionIncidencia;
import com.gombeth.urban.service.GestionIncidenciaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
public class GestionIncidenciaController {

    private final GestionIncidenciaService service;

    public GestionIncidenciaController(
            GestionIncidenciaService service
    ) {
        this.service = service;
    }

    @GetMapping("/comunidad/{comunidadId}")
    public List<GestionIncidenciaResponse> listar(
            @PathVariable Long comunidadId
    ) {
        return service.listarPorComunidad(
                comunidadId
        );
    }

    @GetMapping("/{id}")
    public GestionIncidenciaResponse obtener(
            @PathVariable Long id
    ) {
        return service.obtener(id);
    }

    @PostMapping
    public GestionIncidenciaResponse crear(
            @RequestBody GestionIncidencia incidencia
    ) {
        return service.guardar(incidencia);
    }

    @PutMapping("/{id}")
    public GestionIncidenciaResponse actualizar(
            @PathVariable Long id,
            @RequestBody GestionIncidencia incidencia
    ) {
        return service.actualizar(
                id,
                incidencia
        );
    }
}