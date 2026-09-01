package com.gombeth.urban.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PasswordPolicyServiceTest {

    private PasswordPolicyService service;

    @BeforeEach
    void configurar() {
        service =
                new PasswordPolicyService();
    }

    @Test
    void rechazaPasswordNula() {

        assertEquals(
                "Debe indicar una contraseña.",
                service.validar(null)
        );
    }

    @Test
    void rechazaPasswordVacia() {

        assertEquals(
                "Debe indicar una contraseña.",
                service.validar("")
        );
    }

    @Test
    void rechazaPasswordDemasiadoCorta() {

        assertEquals(
                "La contraseña debe tener "
                        + "al menos 8 caracteres.",
                service.validar(
                        "Abc123"
                )
        );
    }

    @Test
    void rechazaPasswordSuperiorA72Caracteres() {

        String password =
                "A1"
                        + "a".repeat(71);

        assertEquals(
                "La contraseña no puede "
                        + "superar los 72 caracteres.",
                service.validar(
                        password
                )
        );
    }

    @Test
    void rechazaPasswordSinMayuscula() {

        assertEquals(
                "La contraseña debe contener "
                        + "mayúsculas, minúsculas y números.",
                service.validar(
                        "prueba2026"
                )
        );
    }

    @Test
    void rechazaPasswordSinMinuscula() {

        assertEquals(
                "La contraseña debe contener "
                        + "mayúsculas, minúsculas y números.",
                service.validar(
                        "PRUEBA2026"
                )
        );
    }

    @Test
    void rechazaPasswordSinNumero() {

        assertEquals(
                "La contraseña debe contener "
                        + "mayúsculas, minúsculas y números.",
                service.validar(
                        "PruebaSegura"
                )
        );
    }

    @Test
    void aceptaPasswordValida() {

        assertNull(
                service.validar(
                        "Nueva2026"
                )
        );
    }
}