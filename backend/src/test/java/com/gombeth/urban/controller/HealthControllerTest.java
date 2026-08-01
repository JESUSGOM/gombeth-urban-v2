package com.gombeth.urban.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HealthControllerTest {

    private final HealthController controller =
            new HealthController();

    @Test
    void healthDevuelveSoloInformacionPublica() {

        Map<String, String> respuesta =
                controller.health();

        assertEquals(
                Set.of(
                        "status",
                        "app"
                ),
                respuesta.keySet()
        );

        assertEquals(
                "OK",
                respuesta.get("status")
        );

        assertEquals(
                "Gombeth Urban Backend",
                respuesta.get("app")
        );

        String contenido =
                respuesta.toString()
                        .toLowerCase();

        assertFalse(
                contenido.contains("jdbc")
        );

        assertFalse(
                contenido.contains("password")
        );

        assertFalse(
                contenido.contains("username")
        );

        assertFalse(
                contenido.contains("database")
        );

        assertFalse(
                contenido.contains("profile")
        );

        assertFalse(
                contenido.contains("host")
        );
    }
}