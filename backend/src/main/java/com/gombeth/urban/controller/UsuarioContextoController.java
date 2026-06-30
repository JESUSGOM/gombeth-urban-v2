package com.gombeth.urban.controller;

import com.gombeth.urban.dto.ComunidadActivaDTO;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.repository.ComunidadRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuario")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioContextoController {

    private final ComunidadRepository comunidadRepository;

    public UsuarioContextoController(
            ComunidadRepository comunidadRepository
    ) {
        this.comunidadRepository = comunidadRepository;
    }

    @GetMapping("/mis-comunidades")
    public List<ComunidadActivaDTO> misComunidades(
            @RequestParam Long usuarioId
    ) {
        return comunidadRepository
                .findByUsuarioId(usuarioId, Pageable.unpaged())
                .getContent()
                .stream()
                .map(c -> new ComunidadActivaDTO(
                        c.getId(),
                        c.getNombre()
                ))
                .toList();
    }
}