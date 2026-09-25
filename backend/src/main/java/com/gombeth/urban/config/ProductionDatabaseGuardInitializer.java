package com.gombeth.urban.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class ProductionDatabaseGuardInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(
            ConfigurableApplicationContext applicationContext
    ) {

        ProductionDatabaseGuard guard =
                new ProductionDatabaseGuard(
                        applicationContext.getEnvironment()
                );

        guard.comprobarSeguridad();
    }
}