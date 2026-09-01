package com.gombeth.urban.service;

import com.gombeth.urban.dto.AdministradorResumenResponse;
import com.gombeth.urban.dto.ComunidadNombreResponse;
import com.gombeth.urban.dto.RolResponse;
import com.gombeth.urban.dto.UsuarioAdministracionAltaRequest;
import com.gombeth.urban.dto.UsuarioAdministracionEdicionRequest;
import com.gombeth.urban.dto.UsuarioAdministracionResponse;
import com.gombeth.urban.entity.Administrador;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Rol;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.entity.UsuarioComunidad;
import com.gombeth.urban.repository.AdministradorRepository;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.RolRepository;
import com.gombeth.urban.repository.UsuarioComunidadRepository;
import com.gombeth.urban.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UsuarioAdministracionService {

    private final UsuarioRepository
            usuarioRepository;

    private final AdministradorRepository
            administradorRepository;

    private final ComunidadRepository
            comunidadRepository;

    private final UsuarioComunidadRepository
            usuarioComunidadRepository;

    private final RolRepository
            rolRepository;

    private final AccesoComunidadService
            accesoComunidadService;

    private final PasswordEncoder
            passwordEncoder;

    private final PasswordPolicyService
            passwordPolicyService;

    public UsuarioAdministracionService(
            UsuarioRepository usuarioRepository,
            AdministradorRepository administradorRepository,
            ComunidadRepository comunidadRepository,
            UsuarioComunidadRepository usuarioComunidadRepository,
            RolRepository rolRepository,
            AccesoComunidadService accesoComunidadService,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService
    ) {
        this.usuarioRepository =
                usuarioRepository;

        this.administradorRepository =
                administradorRepository;

        this.comunidadRepository =
                comunidadRepository;

        this.usuarioComunidadRepository =
                usuarioComunidadRepository;

        this.rolRepository =
                rolRepository;

        this.accesoComunidadService =
                accesoComunidadService;

        this.passwordEncoder =
                passwordEncoder;

        this.passwordPolicyService =
                passwordPolicyService;
    }

    public List<UsuarioAdministracionResponse>
    listarUsuarios(
            Authentication authentication
    ) {

        Usuario usuarioAutenticado =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        Long administradorId =
                obtenerAdministradorIdObligatorio(
                        usuarioAutenticado
                );

        return usuarioRepository
                .findByAdministradorIdOrderByUsernameAsc(
                        administradorId
                )
                .stream()
                .map(this::convertirResponse)
                .toList();
    }

    public UsuarioAdministracionResponse
    obtenerUsuario(
            Authentication authentication,
            Long usuarioId
    ) {

        if (usuarioId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el usuario."
            );
        }

        Usuario usuarioAutenticado =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        Long administradorId =
                obtenerAdministradorIdObligatorio(
                        usuarioAutenticado
                );

        Usuario usuario =
                obtenerUsuarioGestionable(
                        usuarioId,
                        administradorId
                );

        return convertirResponse(
                usuario
        );
    }

    @Transactional
    public UsuarioAdministracionResponse
    crearUsuario(
            Authentication authentication,
            UsuarioAdministracionAltaRequest request
    ) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar los datos del usuario."
            );
        }

        Usuario usuarioAutenticado =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        Long administradorId =
                obtenerAdministradorIdObligatorio(
                        usuarioAutenticado
                );

        validarAdministradorRequest(
                administradorId,
                request.getAdministradorId()
        );

        String username =
                normalizarUsername(
                        request.getUsername()
                );

        validarUsernameAlta(
                username
        );

        String password =
                request.getPasswordInicial();

        String errorPassword =
                passwordPolicyService.validar(
                        password
                );

        if (errorPassword != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorPassword
            );
        }

        List<Rol> roles =
                obtenerRolesValidados(
                        request.getRolIds()
                );


        List<Comunidad> comunidadesCompartidas =
                obtenerComunidadesCompartidasValidadas(
                        request.getComunidadCompartidaIds(),
                        administradorId
                );

        Usuario nuevoUsuario =
                new Usuario();

        nuevoUsuario.setUsername(
                username
        );

        nuevoUsuario.setPassword(
                passwordEncoder.encode(
                        password
                )
        );

        /*
         * El administrador se obtiene siempre de la
         * sesión autenticada. Nunca se confía en el
         * administradorId recibido desde el cliente.
         */
        nuevoUsuario.setAdministradorId(
                administradorId
        );

        nuevoUsuario.setRoles(
                new LinkedHashSet<>(
                        roles
                )
        );

        Usuario usuarioGuardado =
                usuarioRepository.save(
                        nuevoUsuario
                );

        for (
                Comunidad comunidad
                : comunidadesCompartidas
        ) {
            usuarioComunidadRepository.save(
                    new UsuarioComunidad(
                            usuarioGuardado.getId(),
                            comunidad.getId()
                    )
            );
        }

        return convertirResponse(
                usuarioGuardado
        );
    }

    @Transactional
    public UsuarioAdministracionResponse
    editarUsuario(
            Authentication authentication,
            Long usuarioId,
            UsuarioAdministracionEdicionRequest request
    ) {

        if (usuarioId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el usuario."
            );
        }

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar los datos del usuario."
            );
        }

        Usuario usuarioAutenticado =
                accesoComunidadService
                        .obtenerUsuarioAutenticado(
                                authentication
                        );

        Long administradorId =
                obtenerAdministradorIdObligatorio(
                        usuarioAutenticado
                );

        validarAdministradorRequest(
                administradorId,
                request.getAdministradorId()
        );

        Usuario usuario =
                obtenerUsuarioGestionable(
                        usuarioId,
                        administradorId
                );

        String username =
                normalizarUsername(
                        request.getUsername()
                );

        validarUsernameEdicion(
                usuario,
                username
        );

        List<Rol> roles =
                obtenerRolesValidados(
                        request.getRolIds()
                );

        validarAutoproteccionRolAdmin(
                usuarioAutenticado,
                usuario,
                roles
        );

        List<Comunidad> comunidadesCompartidas =
            obtenerComunidadesCompartidasValidadas(
                request.getComunidadCompartidaIds(),
                administradorId
        );

        usuario.setUsername(
                username
        );

        usuario.setRoles(
                new LinkedHashSet<>(
                        roles
                )
        );

        /*
         * No se modifica la contraseña.
         * Tampoco se modifica comunidades.usuario_id.
         */
        usuarioRepository.save(
                usuario
        );

        sincronizarComunidadesCompartidas(
                usuario,
                comunidadesCompartidas
        );

        return convertirResponse(
                usuario
        );
    }

    private Usuario obtenerUsuarioGestionable(
            Long usuarioId,
            Long administradorId
    ) {

        Usuario usuario =
                usuarioRepository
                        .findById(
                                usuarioId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Usuario no encontrado."
                                )
                        );

        if (
                !Objects.equals(
                        administradorId,
                        usuario.getAdministradorId()
                )
        ) {
            /*
             * Se responde como no encontrado para no
             * revelar usuarios de otro administrador.
             */
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Usuario no encontrado."
            );
        }

        return usuario;
    }

    private void validarAdministradorRequest(
            Long administradorIdAutenticado,
            Long administradorIdRequest
    ) {

        if (
                administradorIdRequest != null
                        && !Objects.equals(
                        administradorIdAutenticado,
                        administradorIdRequest
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puede gestionar usuarios para "
                            + "otro administrador."
            );
        }
    }

    private void validarUsernameAlta(
            String username
    ) {

        if (username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el nombre de usuario."
            );
        }

        if (
                usuarioRepository.existsByUsername(
                        username
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese nombre."
            );
        }
    }

    private void validarUsernameEdicion(
            Usuario usuario,
            String username
    ) {

        if (username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el nombre de usuario."
            );
        }

        Optional<Usuario> usuarioExistente =
                usuarioRepository.findByUsername(
                        username
                );

        if (
                usuarioExistente.isPresent()
                        && !Objects.equals(
                        usuarioExistente.get().getId(),
                        usuario.getId()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese nombre."
            );
        }
    }

    private void validarAutoproteccionRolAdmin(
            Usuario usuarioAutenticado,
            Usuario usuarioGestionado,
            List<Rol> rolesSolicitados
    ) {

        Long usuarioAutenticadoId =
                usuarioAutenticado.getId();

        Long usuarioGestionadoId =
                usuarioGestionado.getId();

        /*
         * La autoprotección sólo aplica cuando ambos
         * usuarios tienen un identificador persistido
         * y se trata realmente del mismo usuario.
         */
        if (
            usuarioAutenticadoId == null
                || usuarioAutenticadoId <= 0
                || usuarioGestionadoId == null
                || usuarioGestionadoId <= 0
                || !Objects.equals(
                    usuarioAutenticadoId,
                    usuarioGestionadoId
                )
        ) {
            return;
        }

        boolean conservaRolAdmin =
                rolesSolicitados
                        .stream()
                        .map(
                                Rol::getNombre
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .map(
                                String::trim
                        )
                        .anyMatch(nombre ->
                                "ADMIN".equalsIgnoreCase(
                                        nombre
                                )
                                        || "ROLE_ADMIN".equalsIgnoreCase(
                                        nombre
                                )
                        );

        if (!conservaRolAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario administrador autenticado "
                            + "no puede quitarse a sí mismo "
                            + "el rol ADMIN."
            );
        }
    }


    private List<Rol> obtenerRolesValidados(
            List<Long> ids
    ) {

        List<Long> rolIds =
                normalizarIds(
                        ids,
                        "roles"
                );

        List<Rol> roles =
                rolRepository.findAllById(
                        rolIds
                );

        if (roles.size() != rolIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Alguno de los roles indicados "
                            + "no existe."
            );
        }

        return roles;
    }

    private List<Comunidad>
    obtenerComunidadesCompartidasValidadas(
            List<Long> ids,
            Long administradorId
    ) {

        List<Long> comunidadIds =
                normalizarIds(
                        ids,
                        "comunidades"
                );

        List<Comunidad> comunidades =
                comunidadRepository.findAllById(
                        comunidadIds
                );

        boolean existeComunidadNoPermitida =
                comunidades.size()
                        != comunidadIds.size()
                        || comunidades
                        .stream()
                        .anyMatch(comunidad ->
                                !Objects.equals(
                                        administradorId,
                                        comunidad.getAdministradorId()
                                )
                        );

        if (existeComunidadNoPermitida) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Alguna de las comunidades indicadas "
                            + "no existe o no pertenece "
                            + "al administrador."
            );
        }

        return comunidades;
    }

    private void sincronizarComunidadesCompartidas(
            Usuario usuario,
            List<Comunidad> comunidadesSolicitadas
    ) {

        LinkedHashSet<Long> idsActuales =
                usuarioComunidadRepository
                        .findByUsuarioId(
                                usuario.getId()
                        )
                        .stream()
                        .map(
                                UsuarioComunidad::getComunidadId
                        )
                        .collect(
                                LinkedHashSet::new,
                                LinkedHashSet::add,
                                LinkedHashSet::addAll
                        );

        LinkedHashSet<Long> idsSolicitados =
                comunidadesSolicitadas
                        .stream()
                        .map(
                                Comunidad::getId
                        )
                        .collect(
                                LinkedHashSet::new,
                                LinkedHashSet::add,
                                LinkedHashSet::addAll
                        );

        for (Long comunidadId : idsActuales) {

            if (
                    !idsSolicitados.contains(
                            comunidadId
                    )
            ) {
                usuarioComunidadRepository
                        .deleteByUsuarioIdAndComunidadId(
                                usuario.getId(),
                                comunidadId
                        );
            }
        }

        for (Long comunidadId : idsSolicitados) {

            if (
                    !idsActuales.contains(
                            comunidadId
                    )
            ) {
                usuarioComunidadRepository.save(
                        new UsuarioComunidad(
                                usuario.getId(),
                                comunidadId
                        )
                );
            }
        }
    }

    private UsuarioAdministracionResponse
    convertirResponse(
            Usuario usuario
    ) {

        AdministradorResumenResponse administrador =
                obtenerAdministradorResponse(
                        usuario.getAdministradorId()
                );

        List<RolResponse> roles =
                usuario.getRoles()
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

        List<ComunidadNombreResponse>
                comunidadesDirectas =
                comunidadRepository
                        .findByUsuarioIdOrderByNombreAsc(
                                usuario.getId()
                        )
                        .stream()
                        .map(this::convertirComunidad)
                        .toList();

        List<Long> idsCompartidos =
                usuarioComunidadRepository
                        .findByUsuarioId(
                                usuario.getId()
                        )
                        .stream()
                        .map(
                                UsuarioComunidad::getComunidadId
                        )
                        .toList();

        List<ComunidadNombreResponse>
                comunidadesCompartidas =
                comunidadRepository
                        .findAllById(
                                idsCompartidos
                        )
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        Comunidad::getNombre,
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                        )
                        .map(this::convertirComunidad)
                        .toList();

        return new UsuarioAdministracionResponse(
                usuario.getId(),
                usuario.getUsername(),
                administrador,
                roles,
                comunidadesDirectas,
                comunidadesCompartidas
        );
    }

    private AdministradorResumenResponse
    obtenerAdministradorResponse(
            Long administradorId
    ) {

        if (administradorId == null) {
            return null;
        }

        Administrador administrador =
                administradorRepository
                        .findById(
                                administradorId
                        )
                        .orElse(null);

        if (administrador == null) {
            return null;
        }

        return new AdministradorResumenResponse(
                administrador.getId(),
                administrador.getNombre()
        );
    }

    private ComunidadNombreResponse
    convertirComunidad(
            Comunidad comunidad
    ) {
        return new ComunidadNombreResponse(
                comunidad.getId(),
                comunidad.getNombre()
        );
    }

    private Long obtenerAdministradorIdObligatorio(
            Usuario usuario
    ) {

        if (usuario.getAdministradorId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario autenticado no tiene "
                            + "administrador asociado."
            );
        }

        return usuario.getAdministradorId();
    }

    private String normalizarUsername(
            String username
    ) {
        return username == null
                ? ""
                : username.trim();
    }

    private List<Long> normalizarIds(
            List<Long> ids,
            String nombreCampo
    ) {

        if (ids == null) {
            return List.of();
        }

        if (
                ids.stream()
                        .anyMatch(
                                Objects::isNull
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La lista de " + nombreCampo
                            + " contiene un identificador vacío."
            );
        }

        return ids.stream()
                .distinct()
                .toList();
    }
}