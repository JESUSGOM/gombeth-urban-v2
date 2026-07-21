package com.gombeth.urban;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class UrbanBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void generarPassword() {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println(
                encoder.encode("Probador123")
        );
    }

}
