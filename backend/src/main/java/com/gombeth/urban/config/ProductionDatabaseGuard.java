package com.gombeth.urban.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
public class ProductionDatabaseGuard
        implements ApplicationRunner {

    private static final Set<String> DDL_SEGUROS =
            Set.of(
                    "none",
                    "validate"
            );

    private final Environment environment;

    public ProductionDatabaseGuard(
            Environment environment
    ) {
        this.environment = environment;
    }

    @Override
    public void run(
            ApplicationArguments args
    ) {
        String datasourceUrl =
                environment.getProperty(
                        "spring.datasource.url",
                        ""
                );

        String databaseName =
                environment.getProperty(
                        "gombeth.production.database-name",
                        "sepa_1914"
                );

        if (!esBaseDeProduccion(
                datasourceUrl,
                databaseName
        )) {
            return;
        }

        comprobarPerfilProduccion();
        comprobarConfirmacionExplicita();
        comprobarDdlSeguro();
    }

    private void comprobarPerfilProduccion() {
        boolean perfilProduccionActivo =
                Arrays.stream(
                                environment.getActiveProfiles()
                        )
                        .anyMatch(
                                profile ->
                                        "prod".equalsIgnoreCase(
                                                profile
                                        )
                        );

        if (!perfilProduccionActivo) {
            throw new IllegalStateException(
                    "ARRANQUE BLOQUEADO: la conexión apunta "
                            + "a la base compartida de producción, "
                            + "pero el perfil 'prod' no está activo."
            );
        }
    }

    private void comprobarConfirmacionExplicita() {
        boolean produccionConfirmada =
                environment.getProperty(
                        "gombeth.production.confirmed",
                        Boolean.class,
                        false
                );

        if (!produccionConfirmada) {
            throw new IllegalStateException(
                    "ARRANQUE BLOQUEADO: para utilizar la base "
                            + "compartida debe confirmarse expresamente "
                            + "gombeth.production.confirmed=true."
            );
        }
    }

    private void comprobarDdlSeguro() {
        String ddlAuto =
                environment.getProperty(
                                "spring.jpa.hibernate.ddl-auto",
                                "none"
                        )
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!DDL_SEGUROS.contains(ddlAuto)) {
            throw new IllegalStateException(
                    "ARRANQUE BLOQUEADO: "
                            + "spring.jpa.hibernate.ddl-auto="
                            + ddlAuto
                            + " no es seguro para producción."
            );
        }
    }

    private boolean esBaseDeProduccion(
            String datasourceUrl,
            String databaseName
    ) {
        if (
                datasourceUrl == null
                        || datasourceUrl.isBlank()
                        || databaseName == null
                        || databaseName.isBlank()
        ) {
            return false;
        }

        String urlNormalizada =
                datasourceUrl
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        String baseNormalizada =
                databaseName
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        int inicioParametros =
                urlNormalizada.indexOf('?');

        if (inicioParametros >= 0) {
            urlNormalizada =
                    urlNormalizada.substring(
                            0,
                            inicioParametros
                    );
        }

        return urlNormalizada.endsWith(
                "/" + baseNormalizada
        );
    }
}