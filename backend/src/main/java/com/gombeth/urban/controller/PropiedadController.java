package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Propiedad;
import com.gombeth.urban.repository.PropiedadRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/propiedades")
public class PropiedadController {

    private final PropiedadRepository repository;

    public PropiedadController(PropiedadRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/vecino/{vecinoId}")
    public List<Propiedad> listarPorVecino(@PathVariable Long vecinoId) {
        return repository.findByVecinoId(vecinoId);
    }

    @GetMapping("/comunidad/{comunidadId}")
    public List<Propiedad> listarPorComunidad(@PathVariable Long comunidadId) {
        return repository.findByComunidadId(comunidadId);
    }

    @GetMapping("/{id}")
    public Propiedad obtenerPorId(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
    }

    @PostMapping
    public Propiedad crear(@RequestBody Propiedad propiedad) {
        if (propiedad.getActivo() == null) {
            propiedad.setActivo(true);
        }

        if (propiedad.getTipo() == null || propiedad.getTipo().isBlank()) {
            propiedad.setTipo("VIVIENDA");
        }

        if (propiedad.getCoeficiente() == null) {
            propiedad.setCoeficiente(BigDecimal.ZERO);
        }

        return repository.save(propiedad);
    }

    @PutMapping("/{id}")
    public Propiedad actualizar(
            @PathVariable Long id,
            @RequestBody Propiedad datos
    ) {
        Propiedad propiedad = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));

        propiedad.setComunidadId(datos.getComunidadId());
        propiedad.setVecinoId(datos.getVecinoId());
        propiedad.setReferencia(datos.getReferencia());
        propiedad.setTipo(datos.getTipo());
        propiedad.setDireccion(datos.getDireccion());
        propiedad.setCoeficiente(datos.getCoeficiente());
        propiedad.setActivo(datos.getActivo());
        propiedad.setNotas(datos.getNotas());

        return repository.save(propiedad);
    }

    @DeleteMapping("/{id}")
    public void darDeBaja(@PathVariable Long id) {
        Propiedad propiedad = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));

        propiedad.setActivo(false);
        repository.save(propiedad);
    }
}