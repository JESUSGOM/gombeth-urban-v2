package com.gombeth.urban.config;

import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.UsuarioRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private AccesoComunidadService accesoComunidadService;

    @Test
    void apiProtegidaSinSesionDevuelve401()
            throws Exception {

        mockMvc.perform(
                        get("/api/comunidades")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.mensaje").value(
                                "Sesión no iniciada o caducada."
                        )
                );
    }

    @Test
    void endpointCsrfPublicoEntregaToken()
            throws Exception {

        mockMvc.perform(
                        get("/api/auth/csrf")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.token").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.headerName").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.parameterName").isNotEmpty()
                );
    }

    @Test
    void escrituraSinCsrfDevuelve403()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/logout")
                                .with(
                                        user("Probador")
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void accesoCruzadoAComunidadDevuelve403()
            throws Exception {

        when(
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                any(Authentication.class),
                                eq(999L)
                        )
        ).thenThrow(
                new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Comunidad no autorizada"
                )
        );

        mockMvc.perform(
                        get("/api/comunidades/999")
                                .with(
                                        user("Probador")
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void loginCreaSesionMeLaReutilizaYLogoutLaInvalida()
            throws Exception {

        Authentication authentication =
                UsernamePasswordAuthenticationToken
                        .authenticated(
                                "Probador",
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_USER"
                                        )
                                )
                        );

        when(
                authenticationManager.authenticate(
                        any(Authentication.class)
                )
        ).thenReturn(
                authentication
        );

        Usuario usuario =
                mock(Usuario.class);

        when(
                usuario.getId()
        ).thenReturn(
                4L
        );

        when(
                usuario.getUsername()
        ).thenReturn(
                "Probador"
        );

        when(
                usuario.getAdministradorId()
        ).thenReturn(
                null
        );

        when(
                usuarioRepository.findByUsername(
                        "Probador"
                )
        ).thenReturn(
                Optional.of(usuario)
        );

        MvcResult resultadoLogin =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .with(
                                                csrf()
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "username": "Probador",
                                                  "password": "clave-prueba"
                                                }
                                                """
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andExpect(
                                jsonPath("$.ok").value(true)
                        )
                        .andExpect(
                                jsonPath("$.usuarioId").value(4)
                        )
                        .andExpect(
                                jsonPath("$.username").value(
                                        "Probador"
                                )
                        )
                        .andReturn();

        MockHttpSession session =
                (MockHttpSession)
                        resultadoLogin
                                .getRequest()
                                .getSession(false);

        assertNotNull(
                session
        );

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(session)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.ok").value(true)
                )
                .andExpect(
                        jsonPath("$.username").value(
                                "Probador"
                        )
                )
                .andExpect(
                        jsonPath("$.mensaje").value(
                                "Sesión válida"
                        )
                );

        mockMvc.perform(
                        post("/api/auth/logout")
                                .session(session)
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().isNoContent()
                );

        assertTrue(
                session.isInvalid()
        );
    }
}