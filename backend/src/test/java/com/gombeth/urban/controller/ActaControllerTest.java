package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Acta;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.EstadoActa;
import com.gombeth.urban.repository.ActaRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.PdfActaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.gombeth.urban.dto.ActaResumenResponse;
import com.gombeth.urban.dto.ActaDetalleResponse;
import com.gombeth.urban.dto.ActaGuardarRequest;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActaControllerTest {

    private ActaRepository actaRepository;

    private PdfActaService pdfActaService;

    private AccesoComunidadService accesoComunidadService;

    private Authentication authentication;

    private ActaController controller;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {

        actaRepository =
                mock(
                        ActaRepository.class
                );

        pdfActaService =
                mock(
                        PdfActaService.class
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
                new ActaController(
                        actaRepository,
                        pdfActaService,
                        accesoComunidadService
                );
    }

    @Test
    void visualizaPdfFirmadoInline()
            throws Exception {

        Acta acta =
                crearActa();

        Path pdf =
                tempDir.resolve(
                        "Acta_7_FIRMADA.pdf"
                );

        byte[] contenido =
                "%PDF-1.7 ACTA FIRMADA"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        Files.write(
                pdf,
                contenido
        );

        when(
                actaRepository.findById(
                        7L
                )
        ).thenReturn(
                Optional.of(acta)
        );

        when(
                pdfActaService.obtenerPdf(
                        acta
                )
        ).thenReturn(
                pdf
        );

        ResponseEntity<Resource> respuesta =
                controller.visualizarPdf(
                        7L,
                        authentication
                );

        assertEquals(
                HttpStatus.OK,
                respuesta.getStatusCode()
        );

        assertEquals(
                MediaType.APPLICATION_PDF,
                respuesta.getHeaders()
                        .getContentType()
        );

        assertEquals(
                contenido.length,
                respuesta.getHeaders()
                        .getContentLength()
        );

        String contentDisposition =
                respuesta.getHeaders()
                        .getFirst(
                                "Content-Disposition"
                        );

        assertNotNull(
                contentDisposition
        );

        assertTrue(
                contentDisposition.startsWith(
                        "inline"
                )
        );

        assertNotNull(
                respuesta.getBody()
        );

        assertEquals(
                "Acta_7_FIRMADA.pdf",
                respuesta.getBody()
                        .getFilename()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                18L
        );

        verify(
                pdfActaService
        ).obtenerPdf(
                acta
        );
    }

    @Test
    void noObtienePdfSiNoTieneAccesoAComunidad()
            throws Exception {

        Acta acta =
                crearActa();

        when(
                actaRepository.findById(
                        7L
                )
        ).thenReturn(
                Optional.of(acta)
        );

        doThrow(
                new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tiene permisos para acceder "
                                + "a esta comunidad."
                )
        ).when(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                18L
        );

        org.springframework.web.server.ResponseStatusException error =
                assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () ->
                                controller.visualizarPdf(
                                        7L,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        verify(
                pdfActaService,
                never()
        ).obtenerPdf(
                any()
        );
    }

    @Test
    void devuelveNotFoundSiNoExisteElPdfFirmado()
            throws Exception {

        Acta acta =
                crearActa();

        when(
                actaRepository.findById(
                        7L
                )
        ).thenReturn(
                Optional.of(acta)
        );

        when(
                pdfActaService.obtenerPdf(
                        acta
                )
        ).thenThrow(
                new IllegalStateException(
                        "No existe el PDF firmado del acta 7."
                )
        );

        org.springframework.web.server.ResponseStatusException error =
                assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () ->
                                controller.visualizarPdf(
                                        7L,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        assertEquals(
                "No existe el PDF firmado del acta 7.",
                error.getReason()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                18L
        );

        verify(
                pdfActaService
        ).obtenerPdf(
                acta
        );
    }

    @Test
    void devuelveNotFoundSiElActaNoExiste()
            throws Exception {

        when(
                actaRepository.findById(
                        999L
                )
        ).thenReturn(
                Optional.empty()
        );

        org.springframework.web.server.ResponseStatusException error =
                assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () ->
                                controller.visualizarPdf(
                                        999L,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        assertEquals(
                "No existe el acta 999",
                error.getReason()
        );

        verify(
                accesoComunidadService,
                never()
        ).validarAcceso(
                any(),
                any()
        );

        verify(
                pdfActaService,
                never()
        ).obtenerPdf(
                any()
        );
    }

    @Test
    void listaActasDeComunidadAutorizada() {

        Comunidad comunidad =
                new Comunidad();

        comunidad.setId(
                18L
        );

        comunidad.setNombre(
                "Comunidad de Propietarios Test"
        );

        Acta acta1 =
                crearActa();

        Acta acta2 =
                new Acta();

        acta2.setId(
                8L
        );

        acta2.setComunidad(
                comunidad
        );

        acta2.setTitulo(
                "Junta General Extraordinaria"
        );

        acta2.setFechaReunion(
                LocalDate.of(
                        2026,
                        9,
                        24
                )
        );

        acta2.setContenido(
                "<p>Segunda acta.</p>"
        );

        acta2.setEstado(
                EstadoActa.BORRADOR
        );

        when(
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                18L
                        )
        ).thenReturn(
                comunidad
        );

        when(
                actaRepository.findByComunidad(
                        comunidad
                )
        ).thenReturn(
                List.of(
                        acta1,
                        acta2
                )
        );

        List<ActaResumenResponse> resultado =
                controller.listarPorComunidad(
                        18L,
                        authentication
                );

        assertEquals(
                2,
                resultado.size()
        );

        assertEquals(
                7L,
                resultado.get(0).id()
        );

        assertEquals(
                18L,
                resultado.get(0).comunidadId()
        );

        assertEquals(
                "Junta General Ordinaria",
                resultado.get(0).titulo()
        );

        assertEquals(
                "CERRADA",
                resultado.get(0).estado()
        );

        assertEquals(
                8L,
                resultado.get(1).id()
        );

        assertEquals(
                "BORRADOR",
                resultado.get(1).estado()
        );

        verify(
                accesoComunidadService
        ).obtenerComunidadAutorizada(
                authentication,
                18L
        );

        verify(
                actaRepository
        ).findByComunidad(
                comunidad
        );
    }

    @Test
    void obtieneDetalleDeActaAutorizada() {

        Acta acta =
                crearActa();

        when(
                actaRepository.findById(
                        7L
                )
        ).thenReturn(
                Optional.of(acta)
        );

        ActaDetalleResponse resultado =
                controller.obtenerDetalle(
                        7L,
                        authentication
                );

        assertEquals(
                7L,
                resultado.id()
        );

        assertEquals(
                18L,
                resultado.comunidadId()
        );

        assertEquals(
                "Junta General Ordinaria",
                resultado.titulo()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        9,
                        23
                ),
                resultado.fechaReunion()
        );

        assertEquals(
                "<p>Contenido del acta.</p>",
                resultado.contenido()
        );

        assertEquals(
                "CERRADA",
                resultado.estado()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                18L
        );
    }

    @Test
    void creaActaComoBorrador() {

        Comunidad comunidad = new Comunidad();

        comunidad.setId(18L);

        comunidad.setNombre("Comunidad de Propietarios Test");

        ActaGuardarRequest request =
                new ActaGuardarRequest(
                        18L,
                        "Junta General Ordinaria 2026",
                        LocalDate.of(
                                2026,
                                10,
                                15
                        ),
                        "<p>Contenido inicial.</p>"
                );

        when(
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                18L
                        )
        ).thenReturn(
                comunidad
        );

        when(
                actaRepository.save(
                        any(Acta.class)
                )
        ).thenAnswer(invocation -> {

            Acta actaGuardada =
                    invocation.getArgument(
                            0
                    );

            actaGuardada.setId(
                    9L
            );

            return actaGuardada;
        });

        ActaDetalleResponse resultado =
                controller.crear(
                        request,
                        authentication
                );

        assertEquals(
                9L,
                resultado.id()
        );

        assertEquals(
                18L,
                resultado.comunidadId()
        );

        assertEquals(
                "Junta General Ordinaria 2026",
                resultado.titulo()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        10,
                        15
                ),
                resultado.fechaReunion()
        );

        assertEquals(
                "<p>Contenido inicial.</p>",
                resultado.contenido()
        );

        assertEquals(
                "BORRADOR",
                resultado.estado()
        );

        verify(
                accesoComunidadService
        ).obtenerComunidadAutorizada(
                authentication,
                18L
        );

        verify(
                actaRepository
        ).save(
                argThat(acta ->
                        acta.getId() == 9L
                                && acta.getComunidad() == comunidad
                                && "Junta General Ordinaria 2026"
                                .equals(
                                        acta.getTitulo()
                                )
                                && LocalDate.of(
                                2026,
                                10,
                                15
                        ).equals(
                                acta.getFechaReunion()
                        )
                                && "<p>Contenido inicial.</p>"
                                .equals(
                                        acta.getContenido()
                                )
                                && acta.getEstado()
                                == EstadoActa.BORRADOR
                )
        );
    }

    @Test
    void actualizaActaMientrasEsBorrador() {

        Acta acta =
                crearActa();

        acta.setEstado(
                EstadoActa.BORRADOR
        );

        ActaGuardarRequest request =
                new ActaGuardarRequest(
                        18L,
                        "Junta General Ordinaria modificada",
                        LocalDate.of(
                                2026,
                                10,
                                20
                        ),
                        "<p>Contenido modificado.</p>"
                );

        when(
                actaRepository.findById(
                        7L
                )
        ).thenReturn(
                Optional.of(acta)
        );

        when(
                actaRepository.save(
                        any(Acta.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(
                        0
                )
        );

        ActaDetalleResponse resultado =
                controller.actualizar(
                        7L,
                        request,
                        authentication
                );

        assertEquals(
                7L,
                resultado.id()
        );

        assertEquals(
                18L,
                resultado.comunidadId()
        );

        assertEquals(
                "Junta General Ordinaria modificada",
                resultado.titulo()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        10,
                        20
                ),
                resultado.fechaReunion()
        );

        assertEquals(
                "<p>Contenido modificado.</p>",
                resultado.contenido()
        );

        assertEquals(
                "BORRADOR",
                resultado.estado()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                18L
        );

        verify(
                actaRepository
        ).save(
                acta
        );
    }

    @Test
    void noPermiteModificarActaCerrada() {

        Acta acta =
                crearActa();

        acta.setEstado(
                EstadoActa.CERRADA
        );

        ActaGuardarRequest request =
                new ActaGuardarRequest(
                        18L,
                        "Intento de modificación",
                        LocalDate.of(
                                2026,
                                10,
                                20
                        ),
                        "<p>No debe guardarse.</p>"
                );

        when(
                actaRepository.findById(
                        7L
                )
        ).thenReturn(
                Optional.of(acta)
        );

        org.springframework.web.server.ResponseStatusException error =
                assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () ->
                                controller.actualizar(
                                        7L,
                                        request,
                                        authentication
                                )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        assertEquals(
                "Solo se pueden modificar actas en estado BORRADOR.",
                error.getReason()
        );

        verify(
                accesoComunidadService
        ).validarAcceso(
                authentication,
                18L
        );

        verify(
                actaRepository,
                never()
        ).save(
                any(Acta.class)
        );
    }

    private Acta crearActa() {

        Comunidad comunidad =
                new Comunidad();

        comunidad.setId(
                18L
        );

        comunidad.setNombre(
                "Comunidad de Propietarios Test"
        );

        Acta acta =
                new Acta();

        acta.setId(
                7L
        );

        acta.setComunidad(
                comunidad
        );

        acta.setTitulo(
                "Junta General Ordinaria"
        );

        acta.setFechaReunion(
                LocalDate.of(
                        2026,
                        9,
                        23
                )
        );

        acta.setContenido(
                "<p>Contenido del acta.</p>"
        );

        acta.setEstado(
                EstadoActa.CERRADA
        );

        return acta;
    }
}