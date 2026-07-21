package com.gombeth.urban.service.contabilidad.icac;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RepartoGastosService {

    // aquí luego conectaremos con vecinos + coeficientes reales

    public void repartir(Long comunidadId, Long gastoId, BigDecimal importe) {

        List<PropietarioCoeficiente> propietarios =
                obtenerPropietarios(comunidadId);

        BigDecimal totalCoeficiente = propietarios.stream()
                .map(PropietarioCoeficiente::getCoeficiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (PropietarioCoeficiente p : propietarios) {

            BigDecimal porcentaje =
                    p.getCoeficiente().divide(totalCoeficiente, 6, BigDecimal.ROUND_HALF_UP);

            BigDecimal importePropietario =
                    importe.multiply(porcentaje);

            generarRecibo(p.getPropietarioId(), importePropietario, gastoId);
        }
    }

    private void generarRecibo(Long propietarioId,
                               BigDecimal importe,
                               Long gastoId) {

        // aquí se conecta con ReciboService real
        System.out.println("Recibo generado para propietario "
                + propietarioId + " importe: " + importe);
    }

    private List<PropietarioCoeficiente> obtenerPropietarios(Long comunidadId) {

        // MOCK temporal → luego lo conectamos a BD real
        return List.of(
                new PropietarioCoeficiente(1L, new BigDecimal("0.60")),
                new PropietarioCoeficiente(2L, new BigDecimal("0.40"))
        );
    }
}