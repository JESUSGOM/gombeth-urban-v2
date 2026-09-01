package com.gombeth.urban.service;

import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaEstado;
import com.gombeth.urban.entity.RemesaEvento;
import com.gombeth.urban.entity.RemesaEventoTipo;
import com.gombeth.urban.repository.FicheroGeneradoRepository;
import com.gombeth.urban.repository.RemesaEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class RemesaSeguimientoService {

    private final FicheroGeneradoRepository
            ficheroGeneradoRepository;

    private final RemesaEventoRepository
            remesaEventoRepository;

    public RemesaSeguimientoService(
            FicheroGeneradoRepository
                    ficheroGeneradoRepository,
            RemesaEventoRepository
                    remesaEventoRepository
    ) {
        this.ficheroGeneradoRepository =
                ficheroGeneradoRepository;

        this.remesaEventoRepository =
                remesaEventoRepository;
    }

    @Transactional
    public FicheroGenerado cambiarEstado(
            FicheroGenerado remesa,
            RemesaEstado nuevoEstado,
            Long usuarioId,
            RemesaEventoTipo tipoEvento,
            String formato,
            String nombreArchivo,
            String detalle
    ) {
        validarRemesa(remesa);

        Objects.requireNonNull(
                nuevoEstado,
                "El nuevo estado es obligatorio."
        );

        Objects.requireNonNull(
                tipoEvento,
                "El tipo de evento es obligatorio."
        );

        RemesaEstado estadoAnterior =
                RemesaEstado.desde(
                        remesa.getEstado()
                );

        if (!estadoAnterior.puedeCambiarA(
                nuevoEstado
        )) {
            throw new IllegalStateException(
                    "No se puede cambiar una remesa "
                            + "del estado "
                            + estadoAnterior
                            + " al estado "
                            + nuevoEstado
                            + "."
            );
        }

        remesa.setEstado(
                nuevoEstado.name()
        );

        FicheroGenerado remesaGuardada =
                ficheroGeneradoRepository.save(
                        remesa
                );

        registrarEventoInterno(
                remesaGuardada,
                usuarioId,
                tipoEvento,
                estadoAnterior.name(),
                nuevoEstado.name(),
                formato,
                nombreArchivo,
                detalle
        );

        return remesaGuardada;
    }

    @Transactional(readOnly = true)
    public List<RemesaEvento> obtenerEventos(
            Long remesaId
    ) {
        Objects.requireNonNull(
                remesaId,
                "El identificador de la remesa es obligatorio."
        );

        return remesaEventoRepository
                .findByRemesaIdOrderByFechaEventoAscIdAsc(
                        remesaId
                );
    }

    @Transactional
    public RemesaEvento registrarEvento(
            FicheroGenerado remesa,
            Long usuarioId,
            RemesaEventoTipo tipoEvento,
            String formato,
            String nombreArchivo,
            String detalle
    ) {
        validarRemesa(remesa);

        Objects.requireNonNull(
                tipoEvento,
                "El tipo de evento es obligatorio."
        );

        return registrarEventoInterno(
                remesa,
                usuarioId,
                tipoEvento,
                null,
                null,
                formato,
                nombreArchivo,
                detalle
        );
    }

    private RemesaEvento registrarEventoInterno(
            FicheroGenerado remesa,
            Long usuarioId,
            RemesaEventoTipo tipoEvento,
            String estadoAnterior,
            String estadoNuevo,
            String formato,
            String nombreArchivo,
            String detalle
    ) {
        RemesaEvento evento =
                new RemesaEvento();

        evento.setRemesaId(
                remesa.getId()
        );

        evento.setComunidadId(
                remesa.getComunidadId()
        );

        evento.setUsuarioId(
                usuarioId
        );

        evento.setTipoEvento(
                tipoEvento
        );

        evento.setEstadoAnterior(
                estadoAnterior
        );

        evento.setEstadoNuevo(
                estadoNuevo
        );

        evento.setFormato(
                normalizarFormato(formato)
        );

        evento.setNombreArchivo(
                limitar(
                        nombreArchivo,
                        255
                )
        );

        evento.setFechaEvento(
                LocalDateTime.now()
        );

        evento.setDetalle(
                limitar(
                        detalle,
                        500
                )
        );

        return remesaEventoRepository.save(
                evento
        );
    }

    private void validarRemesa(
            FicheroGenerado remesa
    ) {
        Objects.requireNonNull(
                remesa,
                "La remesa es obligatoria."
        );

        if (remesa.getId() == null) {
            throw new IllegalArgumentException(
                    "La remesa debe estar guardada."
            );
        }

        if (remesa.getComunidadId() == null) {
            throw new IllegalArgumentException(
                    "La comunidad de la remesa "
                            + "es obligatoria."
            );
        }
    }

    private String normalizarFormato(
            String formato
    ) {
        if (formato == null
                || formato.isBlank()) {
            return null;
        }

        return limitar(
                formato.trim()
                        .toUpperCase(Locale.ROOT),
                10
        );
    }

    private String limitar(
            String valor,
            int longitudMaxima
    ) {
        if (valor == null) {
            return null;
        }

        String valorLimpio =
                valor.trim();

        if (valorLimpio.isEmpty()) {
            return null;
        }

        if (valorLimpio.length()
                <= longitudMaxima) {
            return valorLimpio;
        }

        return valorLimpio.substring(
                0,
                longitudMaxima
        );
    }
}