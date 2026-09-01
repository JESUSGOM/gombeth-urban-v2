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

import java.util.List;

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
    void presentaUnaRemesaConFicheroGeneradoYRegistraUsuario() {

        prepararRemesaConEstado(
                "FICHERO_GENERADO"
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
                        RemesaEstado.PRESENTADA,
                        4L,
                        RemesaEventoTipo.PRESENTADA,
                        null,
                        null,
                        "Remesa marcada manualmente como presentada al banco."
                );

        assertSame(
                remesa,
                resultado
        );

        verify(remesa).setEstado(
                "PRESENTADA"
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
                "FICHERO_GENERADO",
                evento.getEstadoAnterior()
        );

        assertEquals(
                "PRESENTADA",
                evento.getEstadoNuevo()
        );

        assertEquals(
                RemesaEventoTipo.PRESENTADA,
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
    void anulaUnaRemesaConFicheroGeneradoYRegistraUsuario() {

        prepararRemesaConEstado(
                "FICHERO_GENERADO"
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
                        RemesaEstado.ANULADA,
                        4L,
                        RemesaEventoTipo.ANULADA,
                        null,
                        null,
                        "Remesa anulada manualmente desde Gombeth Urban."
                );

        assertSame(
                remesa,
                resultado
        );

        verify(remesa).setEstado(
                "ANULADA"
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
                "FICHERO_GENERADO",
                evento.getEstadoAnterior()
        );

        assertEquals(
                "ANULADA",
                evento.getEstadoNuevo()
        );

        assertEquals(
                RemesaEventoTipo.ANULADA,
                evento.getTipoEvento()
        );

        assertNotNull(
                evento.getFechaEvento()
        );
    }

    @Test
    void impideAnularUnaRemesaYaPresentada() {

        prepararRemesaConEstado(
                "PRESENTADA"
        );

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                service.cambiarEstado(
                                        remesa,
                                        RemesaEstado.ANULADA,
                                        4L,
                                        RemesaEventoTipo.ANULADA,
                                        null,
                                        null,
                                        "Intento de anulación."
                                )
                );

        assertEquals(
                "No se puede cambiar una remesa "
                        + "del estado PRESENTADA "
                        + "al estado ANULADA.",
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
    void obtieneEventosDeLaRemesaEnOrdenCronologico() {

        RemesaEvento evento1 =
                new RemesaEvento();

        RemesaEvento evento2 =
                new RemesaEvento();

        List<RemesaEvento> eventos =
                List.of(
                        evento1,
                        evento2
                );

        when(
                remesaEventoRepository
                        .findByRemesaIdOrderByFechaEventoAscIdAsc(
                                70L
                        )
        ).thenReturn(
                eventos
        );

        List<RemesaEvento> resultado =
                service.obtenerEventos(
                        70L
                );

        assertSame(
                eventos,
                resultado
        );

        verify(
                remesaEventoRepository
        ).findByRemesaIdOrderByFechaEventoAscIdAsc(
                70L
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