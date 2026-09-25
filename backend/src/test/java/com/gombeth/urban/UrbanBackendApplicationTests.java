package com.gombeth.urban;

import com.gombeth.urban.config.ProductionDatabaseGuardInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = "spring.profiles.active=test"
)
class UrbanBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void registraGuardiaTempranaAntesDelArranque() {

        SpringApplication application =
                UrbanBackendApplication.crearAplicacion();

        boolean guardiaRegistrada =
                application
                        .getInitializers()
                        .stream()
                        .anyMatch(
                                ProductionDatabaseGuardInitializer.class
                                        ::isInstance
                        );

        assertTrue(
                guardiaRegistrada
        );
    }
}