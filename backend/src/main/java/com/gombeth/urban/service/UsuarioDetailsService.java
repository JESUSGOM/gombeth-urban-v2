package com.gombeth.urban.service;

import com.gombeth.urban.entity.Rol;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private static final String ROLE_USER = "ROLE_USER";

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        String usernameNormalizado = username == null
                ? ""
                : username.trim();

        Usuario usuario = usuarioRepository
                .findByUsername(usernameNormalizado)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario o contraseña incorrectos."
                        )
                );

        Set<GrantedAuthority> authorities =
                obtenerAuthorities(usuario);

        return User
                .withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(authorities)
                .build();
    }

    private Set<GrantedAuthority> obtenerAuthorities(
            Usuario usuario
    ) {

        Set<GrantedAuthority> authorities =
                new LinkedHashSet<>();

        if (usuario.getRoles() != null) {

            for (Rol rol : usuario.getRoles()) {

                String authority =
                        normalizarAuthority(rol);

                if (authority != null) {
                    authorities.add(
                            new SimpleGrantedAuthority(
                                    authority
                            )
                    );
                }
            }
        }

        /*
         * Compatibilidad temporal:
         *
         * actualmente existen usuarios que todavía
         * no tienen ninguna fila en usuario_roles.
         *
         * Mientras se completa la administración de
         * usuarios y roles, esos usuarios conservan
         * ROLE_USER para no perder el acceso actual.
         */
        if (authorities.isEmpty()) {
            authorities.add(
                    new SimpleGrantedAuthority(
                            ROLE_USER
                    )
            );
        }

        return authorities;
    }

    private String normalizarAuthority(
            Rol rol
    ) {

        if (rol == null
                || rol.getNombre() == null
                || rol.getNombre().isBlank()) {
            return null;
        }

        String nombre = rol
                .getNombre()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (nombre.startsWith("ROLE_")) {
            return nombre;
        }

        return "ROLE_" + nombre;
    }
}
