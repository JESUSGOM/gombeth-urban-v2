package com.gombeth.urban.service;

import com.gombeth.urban.entity.Rol;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioDetailsServiceTest {

    @Test
    void usuarioConRolAdminObtieneRoleAdmin() {

        UsuarioRepository usuarioRepository =
                mock(UsuarioRepository.class);

        Usuario usuario =
                mock(Usuario.class);

        Rol rol =
                mock(Rol.class);

        when(
                rol.getNombre()
        ).thenReturn(
                "ADMIN"
        );

        when(
                usuario.getUsername()
        ).thenReturn(
                "Administrador"
        );

        when(
                usuario.getPassword()
        ).thenReturn(
                "password-cifrada"
        );

        when(
                usuario.getRoles()
        ).thenReturn(
                Set.of(rol)
        );

        when(
                usuarioRepository.findByUsername(
                        "Administrador"
                )
        ).thenReturn(
                Optional.of(usuario)
        );

        UsuarioDetailsService service =
                new UsuarioDetailsService(
                        usuarioRepository
                );

        UserDetails resultado =
                service.loadUserByUsername(
                        "Administrador"
                );

        assertEquals(
                1,
                resultado.getAuthorities().size()
        );

        assertTrue(
                resultado.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals(
                                                "ROLE_ADMIN"
                                        )
                        )
        );
    }

    @Test
    void usuarioConRolYaNormalizadoNoDuplicaPrefijo() {

        UsuarioRepository usuarioRepository =
                mock(UsuarioRepository.class);

        Usuario usuario =
                mock(Usuario.class);

        Rol rol =
                mock(Rol.class);

        when(
                rol.getNombre()
        ).thenReturn(
                "ROLE_ADMIN"
        );

        when(
                usuario.getUsername()
        ).thenReturn(
                "Administrador"
        );

        when(
                usuario.getPassword()
        ).thenReturn(
                "password-cifrada"
        );

        when(
                usuario.getRoles()
        ).thenReturn(
                Set.of(rol)
        );

        when(
                usuarioRepository.findByUsername(
                        "Administrador"
                )
        ).thenReturn(
                Optional.of(usuario)
        );

        UsuarioDetailsService service =
                new UsuarioDetailsService(
                        usuarioRepository
                );

        UserDetails resultado =
                service.loadUserByUsername(
                        "Administrador"
                );

        assertEquals(
                1,
                resultado.getAuthorities().size()
        );

        assertTrue(
                resultado.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals(
                                                "ROLE_ADMIN"
                                        )
                        )
        );
    }

    @Test
    void usuarioSinRolesConservaRoleUserTemporalmente() {

        UsuarioRepository usuarioRepository =
                mock(UsuarioRepository.class);

        Usuario usuario =
                mock(Usuario.class);

        when(
                usuario.getUsername()
        ).thenReturn(
                "Probador"
        );

        when(
                usuario.getPassword()
        ).thenReturn(
                "password-cifrada"
        );

        when(
                usuario.getRoles()
        ).thenReturn(
                Set.of()
        );

        when(
                usuarioRepository.findByUsername(
                        "Probador"
                )
        ).thenReturn(
                Optional.of(usuario)
        );

        UsuarioDetailsService service =
                new UsuarioDetailsService(
                        usuarioRepository
                );

        UserDetails resultado =
                service.loadUserByUsername(
                        "Probador"
                );

        assertEquals(
                1,
                resultado.getAuthorities().size()
        );

        assertTrue(
                resultado.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals(
                                                "ROLE_USER"
                                        )
                        )
        );
    }
}
