package com.gombeth.urban.service;

import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaEstado;
import com.gombeth.urban.entity.RemesaEvento;
import com.gombeth.urban.entity.RemesaEventoTipo;
import com.gombeth.urban.repository.FicheroGeneradoRepository;
import com.gombeth.urban.repository.RemesaEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemesaSeguimientoServiceTest {

    @Mock
    private FicheroGeneradoRepository
            ficheroGeneradoRepository;

    @Mock
    private RemesaEventoRepository
            remesaEventoRepository;

    @Mock
    private FicheroGenerado remesa;

    @InjectMocks
    private RemesaSeguimientoService service;

    @Test
    void cambiaDeGeneradaAValidadaYRegistraUsuario() {

        prepararRemesaConEstado(
                "GENERADA"
        );

        when(
                ficheroGeneradoRepository.save(
                        remesa
                )
        ).thenReturn(
                remesa
        );

        when(
                remesaEventoRepository.save(
                        any(RemesaEvento.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        FicheroGenerado resultado =
                service.cambiarEstado(
                        remesa,
                        RemesaEstado.VALIDADA,
                        4L,
                        RemesaEventoTipo
                                .VALIDACION_CORRECTA,
                        null,
                        null,
                        "Validación SEPA correcta."
                );

        assertSame(
                remesa,
                resultado
        );

        verify(remesa).setEstado(
                "VALIDADA"
        );

        ArgumentCaptor<RemesaEvento> captor =
                ArgumentCaptor.forClass(
                        RemesaEvento.class
                );

        verify(remesaEventoRepository)
                .save(
                        captor.capture()
                );

        RemesaEvento evento =
                captor.getValue();

        assertEquals(
                70L,
                evento.getRemesaId()
        );

        assertEquals(
                33L,
                evento.getComunidadId()
        );

        assertEquals(
                4L,
                evento.getUsuarioId()
        );

        assertEquals(
                "GENERADA",
                evento.getEstadoAnterior()
        );

        assertEquals(
                "VALIDADA",
                evento.getEstadoNuevo()
        );

        assertEquals(
                RemesaEventoTipo
                        .VALIDACION_CORRECTA,
                evento.getTipoEvento()
        );

        assertNotNull(
                evento.getFechaEvento()
        );
    }

    @Test
    void impidePresentarUnaRemesaNoValidada() {

        prepararRemesaConEstado(
                "GENERADA"
        );

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.cambiarEstado(
                                        remesa,
                                        RemesaEstado
                                                .PRESENTADA,
                                        4L,
                                        RemesaEventoTipo
                                                .PRESENTADA,
                                        "XML",
                                        "remesa.xml",
                                        "Presentación bancaria."
                                )
                );

        assertEquals(
                "No se puede cambiar una remesa "
                        + "del estado GENERADA "
                        + "al estado PRESENTADA.",
                excepcion.getMessage()
        );

        verify(
                ficheroGeneradoRepository,
                never()
        ).save(
                any()
        );

        verify(
                remesaEventoRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void impidePresentarDosVecesLaMismaRemesa() {

        prepararRemesaConEstado(
                "PRESENTADA"
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.cambiarEstado(
                                remesa,
                                RemesaEstado
                                        .PRESENTADA,
                                4L,
                                RemesaEventoTipo
                                        .PRESENTADA,
                                "XML",
                                "remesa.xml",
                                "Segunda presentación."
                        )
        );

        verify(
                ficheroGeneradoRepository,
                never()
        ).save(
                any()
        );

        verify(
                remesaEventoRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void registraUnaDescargaXmlSinCambiarEstado() {

        prepararRemesaGuardada();

        when(
                remesaEventoRepository.save(
                        any(RemesaEvento.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        RemesaEvento evento =
                service.registrarEvento(
                        remesa,
                        4L,
                        RemesaEventoTipo
                                .XML_DESCARGADO,
                        "xml",
                        "remesa-70.xml",
                        "Descarga solicitada "
                                + "desde el listado."
                );

        assertEquals(
                "XML",
                evento.getFormato()
        );

        assertEquals(
                "remesa-70.xml",
                evento.getNombreArchivo()
        );

        assertEquals(
                4L,
                evento.getUsuarioId()
        );

        assertEquals(
                RemesaEventoTipo
                        .XML_DESCARGADO,
                evento.getTipoEvento()
        );

        verify(
                ficheroGeneradoRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void rechazaEstadosDesconocidos() {

        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                RemesaEstado.desde(
                                        "ENVIADA"
                                )
                );

        assertEquals(
                "Estado de remesa no reconocido: "
                        + "ENVIADA",
                excepcion.getMessage()
        );
    }

    private void prepararRemesaConEstado(
            String estado
    ) {
        prepararRemesaGuardada();

        when(
                remesa.getEstado()
        ).thenReturn(
                estado
        );
    }

    private void prepararRemesaGuardada() {

        when(
                remesa.getId()
        ).thenReturn(
                70L
        );

        when(
                remesa.getComunidadId()
        ).thenReturn(
                33L
        );
    }
}