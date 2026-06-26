package com.gombeth.urban.controller;

import com.gombeth.urban.dto.LoginRequest;
import com.gombeth.urban.dto.LoginResponse;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.UsuarioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(
            UsuarioRepository usuarioRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.usuarioRepository = usuarioRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (usuario == null) {
            return new LoginResponse(false, null, null, null, null, "Usuario no encontrado");
        }

        boolean passwordOk = passwordEncoder.matches(
                request.getPassword(),
                usuario.getPassword()
        );

        if (!passwordOk) {
            return new LoginResponse(false, null, null, null, null, "Contraseña incorrecta");
        }

        String administradorNombre = null;

        if (usuario.getAdministradorId() != null) {
            administradorNombre = jdbcTemplate.queryForObject(
                    "SELECT nombre FROM administradores WHERE id = ?",
                    String.class,
                    usuario.getAdministradorId()
            );
        }

        return new LoginResponse(
                true,
                usuario.getId(),
                usuario.getUsername(),
                usuario.getAdministradorId(),
                administradorNombre,
                "Login correcto"
        );
    }
}