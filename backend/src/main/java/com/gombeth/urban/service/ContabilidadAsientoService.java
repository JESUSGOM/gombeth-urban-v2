package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ContabilidadAsientoService {

    private final ContabilidadAsientoRepository asientoRepository;

    public ContabilidadAsientoService(
            ContabilidadAsientoRepository asientoRepository
    ) {
        this.asientoRepository = asientoRepository;
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
            throw new IllegalArgumentException("La comunidad es obligatoria.");
        }

        if (fecha == null) {
            fecha = LocalDate.now();
        }

        if (origen != null && origenId != null) {
            var existente = asientoRepository.findByComunidadIdAndOrigenAndOrigenId(
                    comunidadId,
                    origen,
                    origenId
            );

            if (existente.isPresent()) {
                return existente.get();
            }
        }

        Integer ejercicio = fecha.getYear();

        Long siguienteNumero = asientoRepository
                .findTopByComunidadIdAndEjercicioOrderByNumeroAsientoDesc(
                        comunidadId,
                        ejercicio
                )
                .map(ultimo -> ultimo.getNumeroAsiento() + 1)
                .orElse(1L);

        ContabilidadAsiento asiento = new ContabilidadAsiento();
        asiento.setComunidadId(comunidadId);
        asiento.setEjercicio(ejercicio);
        asiento.setNumeroAsiento(siguienteNumero);
        asiento.setFecha(fecha);
        asiento.setConcepto(concepto);
        asiento.setOrigen(origen);
        asiento.setOrigenId(origenId);
        asiento.setUsuarioId(usuarioId);
        asiento.setEstado("CONFIRMADO");

        return asientoRepository.save(asiento);
    }
}