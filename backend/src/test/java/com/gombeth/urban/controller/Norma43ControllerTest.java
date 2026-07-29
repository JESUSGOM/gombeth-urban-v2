package com.gombeth.urban.controller;

import com.gombeth.urban.dto.Norma43PrevisualizacionResponse;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ConciliacionBancariaService;
import com.gombeth.urban.service.Norma43Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Norma43ControllerTest {

    @Mock
    private Norma43Service norma43Service;

    @Mock
    private ConciliacionBancariaService
            conciliacionBancariaService;

    @Mock
    private AccesoComunidadService
            accesoComunidadService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private Norma43Controller controller;

    @Test
    void previsualizarResumeElFicheroSinImportarlo() throws Exception {
        Long comunidadId = 18L;

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "fichero",
                        "extracto-julio.txt",
                        "text/plain",
                        "CONTENIDO NORMA 43".getBytes(
                                StandardCharsets.ISO_8859_1
                        )
                );

        MovimientoBancario debe = crearMovimiento(
                LocalDate.of(2026, 7, 2),
                "1",
                new BigDecimal("25.50"),
                "RECIBO DEVUELTO"
        );

        MovimientoBancario haber = crearMovimiento(
                LocalDate.of(2026, 7, 5),
                "2",
                new BigDecimal("120.00"),
                "INGRESO CUOTAS"
        );

        when(
                norma43Service.previsualizarContenido(
                        comunidadId,
                        "CONTENIDO NORMA 43"
                )
        ).thenReturn(
                List.of(debe, haber)
        );

        Norma43PrevisualizacionResponse respuesta =
                controller.previsualizar(
                        comunidadId,
                        fichero,
                        authentication
                );

        assertEquals(comunidadId, respuesta.comunidadId());
        assertEquals(
                "extracto-julio.txt",
                respuesta.nombreFichero()
        );
        assertEquals(2, respuesta.numeroMovimientos());
        assertEquals(
                new BigDecimal("25.50"),
                respuesta.totalDebe()
        );
        assertEquals(
                new BigDecimal("120.00"),
                respuesta.totalHaber()
        );
        assertEquals(
                LocalDate.of(2026, 7, 2),
                respuesta.fechaInicial()
        );
        assertEquals(
                LocalDate.of(2026, 7, 5),
                respuesta.fechaFinal()
        );
        assertEquals("DEBE", respuesta.movimientos().get(0).tipo());
        assertEquals("HABER", respuesta.movimientos().get(1).tipo());

        verify(accesoComunidadService).validarAcceso(
                authentication,
                comunidadId
        );

        verify(norma43Service).previsualizarContenido(
                comunidadId,
                "CONTENIDO NORMA 43"
        );

        verify(norma43Service, never()).importarContenido(
                comunidadId,
                "CONTENIDO NORMA 43"
        );

        verifyNoInteractions(conciliacionBancariaService);
    }

    @Test
    void previsualizarRechazaUnFicheroVacio() {
        Long comunidadId = 18L;

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "fichero",
                        "vacio.txt",
                        "text/plain",
                        new byte[0]
                );

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.previsualizar(
                                comunidadId,
                                fichero,
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                excepcion.getStatusCode()
        );

        verify(accesoComunidadService).validarAcceso(
                authentication,
                comunidadId
        );

        verifyNoInteractions(
                norma43Service,
                conciliacionBancariaService
        );
    }

    @Test
    void previsualizarNoLeeElFicheroSinAccesoALaComunidad() {
        Long comunidadId = 99L;

        MockMultipartFile fichero =
                new MockMultipartFile(
                        "fichero",
                        "extracto.txt",
                        "text/plain",
                        "CONTENIDO".getBytes(
                                StandardCharsets.ISO_8859_1
                        )
                );

        doThrow(
                new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tiene permisos para acceder a esta comunidad."
                )
        ).when(accesoComunidadService)
                .validarAcceso(
                        authentication,
                        comunidadId
                );

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> controller.previsualizar(
                                comunidadId,
                                fichero,
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                excepcion.getStatusCode()
        );

        verify(norma43Service, never())
                .previsualizarContenido(
                        org.mockito.ArgumentMatchers.anyLong(),
                        anyString()
                );

        verifyNoInteractions(conciliacionBancariaService);
    }

    private MovimientoBancario crearMovimiento(
            LocalDate fecha,
            String signo,
            BigDecimal importe,
            String concepto
    ) {
        MovimientoBancario movimiento =
                new MovimientoBancario();

        movimiento.setComunidadId(18L);
        movimiento.setFechaOperacion(fecha);
        movimiento.setFechaValor(fecha);
        movimiento.setSigno(signo);
        movimiento.setImporte(importe);
        movimiento.setConcepto(concepto);
        movimiento.setConceptoCompleto(concepto);
        movimiento.setReferenciaBancaria("REFERENCIA");
        movimiento.setDocumentoExtra("DOCUMENTO");

        return movimiento;
    }
}
