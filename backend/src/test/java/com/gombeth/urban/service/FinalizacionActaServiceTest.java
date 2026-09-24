package com.gombeth.urban.service;

import com.gombeth.urban.entity.Acta;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.EstadoActa;
import com.gombeth.urban.repository.ActaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FinalizacionActaServiceTest {

    private ActaRepository actaRepository;

    private PdfActaService pdfActaService;

    private FinalizacionActaService service;

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

        service =
                new FinalizacionActaService(
                        actaRepository,
                        pdfActaService
                );
    }

    @Test
    void finalizaBorradorGenerandoPdfFirmadoAntesDeGuardar()
            throws Exception {

        Acta acta =
                crearActaBorrador();

        when(
                actaRepository.findByIdForUpdate(
                        7L
                )
        ).thenReturn(
                Optional.of(acta)
        );

        when(
                pdfActaService.generarPdfActa(
                        any(Acta.class)
                )
        ).thenReturn(
                "C:\\sepa1914\\ficheros\\actas"
                        + "\\comunidad_18"
                        + "\\Acta_7_FIRMADA.pdf"
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

        Acta resultado =
                service.finalizar(
                        7L
                );

        assertSame(
                acta,
                resultado
        );

        assertEquals(
                EstadoActa.CERRADA,
                resultado.getEstado()
        );

        assertNotNull(
                resultado.getTokenPresidente()
        );

        assertFalse(
                resultado.getTokenPresidente()
                        .isBlank()
        );

        verify(
                actaRepository
        ).findByIdForUpdate(
                7L
        );

        verify(
                pdfActaService
        ).generarPdfActa(
                argThat(actaPdf ->
                        actaPdf.getId().equals(
                                7L
                        )
                                && actaPdf.getEstado()
                                == EstadoActa.CERRADA
                )
        );

        verify(
                actaRepository
        ).save(
                acta
        );
    }

    private Acta crearActaBorrador() {

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
                EstadoActa.BORRADOR
        );

        return acta;
    }
}