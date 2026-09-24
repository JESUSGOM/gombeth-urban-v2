package com.gombeth.urban.controller;

import com.gombeth.urban.dto.FacturaOcrResultado;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.FacturaOcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FacturaOcrControllerTest {

    private FacturaOcrService facturaOcrService;
    private AccesoComunidadService accesoComunidadService;
    private Authentication authentication;
    private FacturaOcrController controller;

    @BeforeEach
    void setUp() {

        facturaOcrService =
                mock(
                        FacturaOcrService.class
                );

        accesoComunidadService =
                mock(
                        AccesoComunidadService.class
                );

        authentication =
                mock(
                        Authentication.class
                );

        controller =
                new FacturaOcrController(
                        facturaOcrService,
                        accesoComunidadService
                );
    }

    @Test
    void analizaFacturaYDevuelveLosDatosPropuestos() {

        Long comunidadId = 33L;

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        "%PDF-1.7 prueba".getBytes()
                );

        FacturaOcrResultado esperado =
                new FacturaOcrResultado(
                        "ATENCO ENERGIA SL",
                        LocalDate.of(
                                2026,
                                9,
                                15
                        ),
                        new BigDecimal(
                                "217.71"
                        ),
                        "FAT-2026-054412",
                        false,
                        List.of()
                );

        when(
                facturaOcrService.analizarFactura(
                        fichero
                )
        ).thenReturn(
                esperado
        );

        FacturaOcrResultado resultado =
                controller.analizarFactura(
                        comunidadId,
                        fichero,
                        authentication
                );

        assertEquals(
                esperado,
                resultado
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                comunidadId
        );

        verify(
                facturaOcrService
        ).analizarFactura(
                fichero
        );
    }

    @Test
    void rechazaComunidadNoValidaSinEjecutarOcr() {

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        "%PDF-1.7 prueba".getBytes()
                );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.analizarFactura(
                                        0L,
                                        fichero,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verifyNoInteractions(
                accesoComunidadService,
                facturaOcrService
        );
    }

    @Test
    void noEjecutaOcrSiUsuarioNoTieneAccesoALaComunidad() {

        Long comunidadId = 99L;

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.pdf",
                        "application/pdf",
                        "%PDF-1.7 prueba".getBytes()
                );

        doThrow(
                new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tiene permisos para acceder a esta comunidad."
                )
        ).when(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                comunidadId
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.analizarFactura(
                                        comunidadId,
                                        fichero,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        verify(
                facturaOcrService,
                never()
        ).analizarFactura(
                any()
        );
    }

    @Test
    void convierteErrorDeFacturaEnBadRequest() {

        Long comunidadId = 33L;

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "file",
                        "factura.txt",
                        "text/plain",
                        "no es pdf".getBytes()
                );

        when(
                facturaOcrService.analizarFactura(
                        fichero
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "El fichero debe ser un PDF."
                )
        );

        ResponseStatusException error =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                controller.analizarFactura(
                                        comunidadId,
                                        fichero,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                comunidadId
        );
    }
}