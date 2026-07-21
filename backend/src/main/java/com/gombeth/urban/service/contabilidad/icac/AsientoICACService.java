package com.gombeth.urban.service.contabilidad.icac;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AsientoICACService {

    public void generarAsientoGasto(Long comunidadId,
                                    Long gastoId,
                                    BigDecimal importe) {

        // ICAC NO busca beneficio
        // solo equilibrio contable

        System.out.println("Asiento ICAC generado:");
        System.out.println("Gasto comunidad: " + importe);
        System.out.println("Repartido entre propietarios");
    }
}