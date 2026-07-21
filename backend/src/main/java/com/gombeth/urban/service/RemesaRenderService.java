package com.gombeth.urban.service;

import com.gombeth.urban.dto.remesa.RemesaLineaPreparada;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RemesaRenderService {

    private final RemesaLineaConceptoRepository conceptosRepository;

    public RemesaRenderService(
            RemesaLineaConceptoRepository conceptosRepository
    ) {
        this.conceptosRepository = conceptosRepository;
    }

    public List<RemesaLineaPreparada> preparar(
            List<RemesaLinea> lineas,
            List<Vecino> vecinos
    ) {

        Map<Long, Vecino> mapaVecinos =
                vecinos.stream()
                        .collect(Collectors.toMap(
                                Vecino::getId,
                                v -> v
                        ));

        List<RemesaLineaPreparada> resultado =
                new ArrayList<>();

        for (RemesaLinea linea : lineas) {

            List<RemesaLineaConcepto> conceptos =
                    conceptosRepository
                            .findByRemesaLineaIdOrderByOrdenAsc(
                                    linea.getId()
                            );

            resultado.add(
                    new RemesaLineaPreparada(
                            linea,
                            mapaVecinos.get(linea.getVecinoId()),
                            conceptos
                    )
            );
        }

        return resultado;
    }

}