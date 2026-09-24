package com.gombeth.urban.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActasPropertiesTest {

    @Test
    void resuelveConfiguracionLocalDeActas() throws IOException {

        Properties applicationProperties =
                cargar(Path.of(
                        "src",
                        "main",
                        "resources",
                        "application.properties"
                ));

        Properties localProperties =
                cargar(Path.of(
                        "config",
                        "application-local.properties"
                ));

        MockEnvironment environment =
                new MockEnvironment();

        environment.getPropertySources().addLast(
                new PropertiesPropertySource(
                        "application.properties",
                        applicationProperties
                )
        );

        environment.getPropertySources().addFirst(
                new PropertiesPropertySource(
                        "application-local.properties",
                        localProperties
                )
        );

        assertEquals(
                "C:/sepa1914/ficheros/actas",
                environment.getProperty(
                        "app.storage.actas-path"
                )
        );

        assertEquals(
                "C:/sepa1914/certificados/CertificadoJesus.p12",
                environment.getProperty(
                        "gombeth.actas.certificado-path"
                )
        );

        assertEquals(
                "",
                environment.getProperty(
                        "gombeth.actas.certificado-password"
                )
        );
    }

    private Properties cargar(Path ruta)
            throws IOException {

        Properties properties = new Properties();

        try (InputStream input =
                     Files.newInputStream(ruta)) {

            properties.load(input);
        }

        return properties;
    }
}