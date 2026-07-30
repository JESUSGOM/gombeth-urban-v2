package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ContabilidadAsientoService {

    private static final String ESTADO_ANULADO =
            "ANULADO";

    private static final String ESTADO_CONFIRMADO =
            "CONFIRMADO";

    private final ContabilidadAsientoRepository
            asientoRepository;

    public ContabilidadAsientoService(
            ContabilidadAsientoRepository asientoRepository
    ) {
        this.asientoRepository =
                asientoRepository;
    }

    @Transactional
    public ContabilidadAsiento crearAsientoAutomatico(
            Long comunidadId,
            LocalDate fecha,
            String concepto,
            String origen,
            Long origenId,
            Long usuarioId
    ) {
        if (comunidadId == null) {
            throw new IllegalArgumentException(
                    "La comunidad es obligatoria."
            );
        }

        LocalDate fechaAsiento =
                fecha != null
                        ? fecha
                        : LocalDate.now();

        Optional<ContabilidadAsiento> ultimoAsiento =
                buscarUltimoAsientoOrigen(
                        comunidadId,
                        origen,
                        origenId
                );

        if (
                ultimoAsiento.isPresent()
                        && !ESTADO_ANULADO.equals(
                        ultimoAsiento.get().getEstado()
                )
        ) {
            return ultimoAsiento.get();
        }

        Integer ejercicio =
                fechaAsiento.getYear();

        Long siguienteNumero =
                asientoRepository
                        .findTopByComunidadIdAndEjercicioOrderByNumeroAsientoDesc(
                                comunidadId,
                                ejercicio
                        )
                        .map(ultimo ->
                                ultimo.getNumeroAsiento() + 1
                        )
                        .orElse(1L);

        ContabilidadAsiento asiento =
                new ContabilidadAsiento();

        asiento.setComunidadId(
                comunidadId
        );

        asiento.setEjercicio(
                ejercicio
        );

        asiento.setNumeroAsiento(
                siguienteNumero
        );

        asiento.setFecha(
                fechaAsiento
        );

        asiento.setConcepto(
                concepto
        );

        asiento.setOrigen(
                origen
        );

        asiento.setOrigenId(
                origenId
        );

        asiento.setUsuarioId(
                usuarioId
        );

        asiento.setEstado(
                ESTADO_CONFIRMADO
        );

        return asientoRepository.save(
                asiento
        );
    }

    private Optional<ContabilidadAsiento>
    buscarUltimoAsientoOrigen(
            Long comunidadId,
            String origen,
            Long origenId
    ) {
        if (
                origen == null
                        || origenId == null
        ) {
            return Optional.empty();
        }

        return asientoRepository
                .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                        comunidadId,
                        origen,
                        origenId
                );
    }
}