package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.UsuarioComunidadRepository;
import com.gombeth.urban.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class AccesoComunidadService {

    private final UsuarioRepository usuarioRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioComunidadRepository
            usuarioComunidadRepository;

    public AccesoComunidadService(
            UsuarioRepository usuarioRepository,
            ComunidadRepository comunidadRepository,
            UsuarioComunidadRepository
                    usuarioComunidadRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.comunidadRepository = comunidadRepository;
        this.usuarioComunidadRepository =
                usuarioComunidadRepository;
    }

    /**
     * Obtiene el usuario a partir de la sesión autenticada.
     *
     * No acepta usuarioId enviado por URL, formulario
     * ni cuerpo JSON.
     */
    public Usuario obtenerUsuarioAutenticado(
            Authentication authentication
    ) {
        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || authentication
                        instanceof AnonymousAuthenticationToken
                        || authentication.getName() == null
                        || authentication.getName().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Sesión no iniciada o caducada."
            );
        }

        return usuarioRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "El usuario autenticado ya no existe."
                        )
                );
    }

    public Long obtenerUsuarioId(
            Authentication authentication
    ) {
        return obtenerUsuarioAutenticado(
                authentication
        ).getId();
    }

    /**
     * Lista únicamente las comunidades accesibles
     * por el usuario autenticado.
     */
    public Page<Comunidad> listarComunidades(
            Authentication authentication,
            Pageable pageable
    ) {
        Long usuarioId = obtenerUsuarioId(
                authentication
        );

        return comunidadRepository
                .findAccesiblesPorUsuario(
                        usuarioId,
                        pageable
                );
    }

    /**
     * Lista ordenada para el selector global
     * de comunidad.
     */
    public List<Comunidad> listarComunidadesOrdenadas(
            Authentication authentication
    ) {
        Long usuarioId = obtenerUsuarioId(
                authentication
        );

        return comunidadRepository
                .findAccesiblesPorUsuarioOrdenadas(
                        usuarioId
                );
    }

    /**
     * Obtiene una comunidad solo cuando está asignada
     * al usuario autenticado.
     */
    public Comunidad obtenerComunidadAutorizada(
            Authentication authentication,
            Long comunidadId
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(
                authentication
        );

        Comunidad comunidad = comunidadRepository
                .findById(comunidadId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Comunidad no encontrada con ID: "
                                        + comunidadId
                        )
                );

        boolean asignacionDirecta = Objects.equals(
                comunidad.getUsuarioId(),
                usuario.getId()
        );

        boolean asignacionCompartida =
                usuarioComunidadRepository
                        .existsByUsuarioIdAndComunidadId(
                                usuario.getId(),
                                comunidadId
                        );

        if (
                !asignacionDirecta
                        && !asignacionCompartida
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para acceder "
                            + "a esta comunidad."
            );
        }

        return comunidad;
    }

    public void validarAcceso(
            Authentication authentication,
            Long comunidadId
    ) {
        obtenerComunidadAutorizada(
                authentication,
                comunidadId
        );
    }
}