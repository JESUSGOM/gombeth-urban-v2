package com.gombeth.urban.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionDatabaseGuardInitializerTest {

    @Test
    void permiteInicializarConBaseDeDesarrollo() {

        MockEnvironment environment =
                new MockEnvironment();

        environment.setProperty(
                "spring.datasource.url",
                "jdbc:mysql://localhost:3306/gombeth_urban_dev"
        );

        environment.setProperty(
                "gombeth.production.database-name",
                "sepa_1914"
        );

        environment.setProperty(
                "spring.jpa.hibernate.ddl-auto",
                "none"
        );

        GenericApplicationContext context =
                new GenericApplicationContext();

        context.setEnvironment(
                environment
        );

        ProductionDatabaseGuardInitializer initializer =
                new ProductionDatabaseGuardInitializer();

        assertDoesNotThrow(
                () -> initializer.initialize(
                        context
                )
        );
    }

    @Test
    void bloqueaInicializacionTempranaContraProduccionSinPerfilProd() {

        MockEnvironment environment =
                new MockEnvironment();

        environment.setProperty(
                "spring.datasource.url",
                "jdbc:mysql://servidor-remoto:3306/sepa_1914"
        );

        environment.setProperty(
                "gombeth.production.database-name",
                "sepa_1914"
        );

        environment.setProperty(
                "gombeth.production.confirmed",
                "true"
        );

        environment.setProperty(
                "spring.jpa.hibernate.ddl-auto",
                "none"
        );

        GenericApplicationContext context =
                new GenericApplicationContext();

        context.setEnvironment(
                environment
        );

        ProductionDatabaseGuardInitializer initializer =
                new ProductionDatabaseGuardInitializer();

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> initializer.initialize(
                                context
                        )
                );

        assertTrue(
                excepcion.getMessage().contains(
                        "perfil 'prod'"
                )
        );
    }

    @Test
    void bloqueaInicializacionTempranaContraProduccionSinConfirmacion() {

        MockEnvironment environment =
                new MockEnvironment();

        environment.setActiveProfiles(
                "prod"
        );

        environment.setProperty(
                "spring.datasource.url",
                "jdbc:mysql://servidor-remoto:3306/sepa_1914"
        );

        environment.setProperty(
                "gombeth.production.database-name",
                "sepa_1914"
        );

        environment.setProperty(
                "spring.jpa.hibernate.ddl-auto",
                "none"
        );

        GenericApplicationContext context =
                new GenericApplicationContext();

        context.setEnvironment(
                environment
        );

        ProductionDatabaseGuardInitializer initializer =
                new ProductionDatabaseGuardInitializer();

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> initializer.initialize(
                                context
                        )
                );

        assertTrue(
                excepcion.getMessage().contains(
                        "gombeth.production.confirmed=true"
                )
        );
    }
}