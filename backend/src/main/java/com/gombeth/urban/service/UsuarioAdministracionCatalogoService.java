package com.gombeth.urban.service;

import com.gombeth.urban.dto.ComunidadNombreResponse;
import com.gombeth.urban.dto.RolResponse;
import com.gombeth.urban.entity.Rol;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.RolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsuarioAdministracionCatalogoService {

    private final RolRepository
            rolRepository;

    private final ComunidadRepository
            comunidadRepository;

    private final AccesoComunidadService
            accesoComunidadService;

    public UsuarioAdministracionCatalogoService(
            RolRepository rolRepository,
            ComunidadRepository comunidadRepository,
            AccesoComunidadService accesoComunidadService
    ) {
        this.rolRepository =
                rolRepository;

        this.comunidadRepository =
                comunidadRepository;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    public List<RolResponse> listarRolesDisponibles(
            Authentication authentication
    ) {

        obtenerAdministradorId(
                authentication
        );

        return rolRepository
                .findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Rol::getNombre,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                )
                .map(rol ->
                        new RolResponse(
                                rol.getId(),
                                rol.getNombre()
                        )
                )
                .toList();
    }

    public List<ComunidadNombreResponse>
    listarComunidadesAsignables(
            Authentication authentication
    ) {

        Long administradorId =
                obtenerAdministradorId(
                        authentication
                );

        return comunidadRepository
                .findByAdministradorIdOrderByNombreAsc(
                        administradorId
                )
                .stream()
                .map(comunidad ->
                        new ComunidadNombreResponse(
                                comunidad.getId(),
                                comunidad.getNombre()
                        )
                )
                .toList();
    }

    private Long obtenerAdministradorId(
            Authentication authentication
    ) {

        Usuario usuario =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        if (usuario.getAdministradorId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario autenticado no tiene "
                            + "administrador asociado."
            );
        }

        return usuario.getAdministradorId();
    }
}