package com.gombeth.urban.controller;

import com.gombeth.urban.dto.ComunidadActivaDTO;
import com.gombeth.urban.service.AccesoComunidadService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioContextoController {

    private final AccesoComunidadService
            accesoComunidadService;

    public UsuarioContextoController(
            AccesoComunidadService
                    accesoComunidadService
    ) {
        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * El usuario ya no se recibe mediante usuarioId.
     * Se obtiene directamente de Spring Security.
     *
     * Los parámetros adicionales que todavía envíe
     * Angular serán ignorados.
     */
    @GetMapping("/mis-comunidades")
    public List<ComunidadActivaDTO> misComunidades(
            Authentication authentication
    ) {
        return accesoComunidadService
                .listarComunidadesOrdenadas(
                        authentication
                )
                .stream()
                .map(comunidad ->
                        new ComunidadActivaDTO(
                                comunidad.getId(),
                                comunidad.getNombre()
                        )
                )
                .toList();
    }
}