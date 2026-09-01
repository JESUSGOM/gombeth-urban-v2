package com.gombeth.urban.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.gombeth.urban.audit.AuditLogService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaForwardConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    void rutaAngularProfundaDevuelveIndexHtml()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/conceptos/comunidad/33/"
                                        + "editar/8/detalle/adicional"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.TEXT_HTML
                        )
                )
                .andExpect(
                        content().string(
                                containsString("<app-root")
                        )
                );
    }

    @Test
    void archivoEstaticoInexistenteDevuelve404()
            throws Exception {

        mockMvc.perform(
                        get("/assets/archivo-inexistente.js")
                )
                .andExpect(
                        status().isNotFound()
                );
    }

    @Test
    @WithMockUser(
            username = "usuario-prueba",
            roles = "USER"
    )
    void apiInexistenteNoDevuelveIndexHtml()
            throws Exception {

        mockMvc.perform(
                        get("/api/ruta-inexistente")
                )
                .andExpect(
                        status().isNotFound()
                );
    }
}