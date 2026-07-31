package com.gombeth.urban.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionDatabaseGuardTest {

    @Test
    void permiteUnaBaseQueNoEsProduccion() {
        MockEnvironment environment = entornoBase();
        environment.setProperty(
                "spring.datasource.url",
                "jdbc:mysql://localhost:3306/gombeth_urban_dev"
        );

        ProductionDatabaseGuard guard =
                new ProductionDatabaseGuard(environment);

        assertDoesNotThrow(
                () -> ejecutar(guard)
        );
    }

    @Test
    void bloqueaProduccionSinPerfilProd() {
        MockEnvironment environment = entornoProduccion();
        environment.setProperty(
                "gombeth.production.confirmed",
                "true"
        );

        ProductionDatabaseGuard guard =
                new ProductionDatabaseGuard(environment);

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> ejecutar(guard)
                );

        assertTrue(
                excepcion.getMessage().contains(
                        "perfil 'prod'"
                )
        );
    }

    @Test
    void bloqueaProduccionSinConfirmacionExplicita() {
        MockEnvironment environment = entornoProduccion();
        environment.setActiveProfiles("prod");

        ProductionDatabaseGuard guard =
                new ProductionDatabaseGuard(environment);

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> ejecutar(guard)
                );

        assertTrue(
                excepcion.getMessage().contains(
                        "gombeth.production.confirmed=true"
                )
        );
    }

    @Test
    void bloqueaProduccionConDdlPeligroso() {
        MockEnvironment environment = entornoProduccion();
        environment.setActiveProfiles("prod");
        environment.setProperty(
                "gombeth.production.confirmed",
                "true"
        );
        environment.setProperty(
                "spring.jpa.hibernate.ddl-auto",
                "update"
        );

        ProductionDatabaseGuard guard =
                new ProductionDatabaseGuard(environment);

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> ejecutar(guard)
                );

        assertTrue(
                excepcion.getMessage().contains(
                        "no es seguro para producción"
                )
        );
    }

    @Test
    void permiteProduccionConfirmadaConDdlNone() {
        MockEnvironment environment = entornoProduccion();
        environment.setActiveProfiles("prod");
        environment.setProperty(
                "gombeth.production.confirmed",
                "true"
        );
        environment.setProperty(
                "spring.jpa.hibernate.ddl-auto",
                "none"
        );

        ProductionDatabaseGuard guard =
                new ProductionDatabaseGuard(environment);

        assertDoesNotThrow(
                () -> ejecutar(guard)
        );
    }

    @Test
    void permiteProduccionConfirmadaConDdlValidate() {
        MockEnvironment environment = entornoProduccion();
        environment.setActiveProfiles("prod");
        environment.setProperty(
                "gombeth.production.confirmed",
                "true"
        );
        environment.setProperty(
                "spring.jpa.hibernate.ddl-auto",
                "validate"
        );

        ProductionDatabaseGuard guard =
                new ProductionDatabaseGuard(environment);

        assertDoesNotThrow(
                () -> ejecutar(guard)
        );
    }

    private MockEnvironment entornoBase() {
        MockEnvironment environment =
                new MockEnvironment();

        environment.setProperty(
                "gombeth.production.database-name",
                "sepa_1914"
        );
        environment.setProperty(
                "spring.jpa.hibernate.ddl-auto",
                "none"
        );

        return environment;
    }

    private MockEnvironment entornoProduccion() {
        MockEnvironment environment =
                entornoBase();

        environment.setProperty(
                "spring.datasource.url",
                "jdbc:mysql://servidor-remoto:3306/sepa_1914"
                        + "?useSSL=true"
        );

        return environment;
    }

    private void ejecutar(
            ProductionDatabaseGuard guard
    ) throws Exception {
        guard.run(
                new DefaultApplicationArguments(
                        new String[0]
                )
        );
    }
}