package com.gombeth.urban;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = "spring.profiles.active=test"
)
class UrbanBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
