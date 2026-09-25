package com.gombeth.urban;

import com.gombeth.urban.config.ProductionDatabaseGuardInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UrbanBackendApplication {

    public static void main(String[] args) {
        crearAplicacion().run(args);
    }

    static SpringApplication crearAplicacion() {

        SpringApplication application =
                new SpringApplication(
                        UrbanBackendApplication.class
                );

        application.addInitializers(
                new ProductionDatabaseGuardInitializer()
        );

        return application;
    }
}