package com.gombeth.urban.dto.remesa;

import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.entity.Vecino;

import java.util.List;

public class RemesaLineaPreparada {

    private final RemesaLinea linea;
    private final Vecino vecino;
    private final List<RemesaLineaConcepto> conceptos;

    public RemesaLineaPreparada(
            RemesaLinea linea,
            Vecino vecino,
            List<RemesaLineaConcepto> conceptos
    ) {
        this.linea = linea;
        this.vecino = vecino;
        this.conceptos = conceptos;
    }

    public RemesaLinea getLinea() {
        return linea;
    }

    public Vecino getVecino() {
        return vecino;
    }

    public List<RemesaLineaConcepto> getConceptos() {
        return conceptos;
    }
}