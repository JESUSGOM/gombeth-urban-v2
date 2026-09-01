package com.gombeth.urban.service;

import com.gombeth.urban.dto.GastoGuardarRequest;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.repository.ContabilidadGastoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ContabilidadGastoService {

    private final ContabilidadGastoRepository gastoRepository;

    public ContabilidadGastoService(
            ContabilidadGastoRepository gastoRepository
    ) {
        this.gastoRepository = gastoRepository;
    }

    public List<ContabilidadGasto> listarPorComunidad(
            Long comunidadId
    ) {
        return gastoRepository
                .findByComunidadIdOrderByFechaFacturaDescIdDesc(
                        comunidadId
                );
    }

    @Transactional
    public ContabilidadGasto crear(
            GastoGuardarRequest request
    ) {
        validarDatos(
                request
        );

        ContabilidadGasto gasto =
                new ContabilidadGasto();

        copiarDatosEditables(
                request,
                gasto
        );

        gasto.setPagado(
                false
        );

        gasto.setFechaPago(
                null
        );

        gasto.setNumeroAsiento(
                null
        );

        gasto.setRutaPdf(
                null
        );

        return gastoRepository.save(
                gasto
        );
    }

    @Transactional
    public ContabilidadGasto actualizar(
            Long gastoId,
            GastoGuardarRequest request
    ) {
        validarDatos(
                request
        );

        ContabilidadGasto gasto =
                findById(
                        gastoId
                );

        if (
                Boolean.TRUE.equals(
                        gasto.getPagado()
                )
        ) {
            throw new IllegalStateException(
                    "No se puede editar un gasto ya pagado. "
                            + "Primero debe deshacerse el pago."
            );
        }

        if (
                gasto.getNumeroAsiento() != null
                        && !gasto.getNumeroAsiento().isBlank()
        ) {
            throw new IllegalStateException(
                    "No se puede editar un gasto ya contabilizado "
                            + "hasta implementar su reversión "
                            + "contable segura."
            );
        }

        if (
                !gasto.getComunidadId()
                        .equals(
                                request.comunidadId()
                        )
        ) {
            throw new IllegalArgumentException(
                    "No se permite cambiar la comunidad "
                            + "de un gasto existente."
            );
        }

        copiarDatosEditables(
                request,
                gasto
        );

        return gastoRepository.save(
                gasto
        );
    }

    public ContabilidadGasto findById(
            Long id
    ) {
        return gastoRepository.findById(
                        id
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el gasto "
                                        + id
                        )
                );
    }

    private void validarDatos(
            GastoGuardarRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Los datos del gasto son obligatorios."
            );
        }

        if (request.comunidadId() == null) {
            throw new IllegalArgumentException(
                    "La comunidad es obligatoria."
            );
        }

        if (request.fechaFactura() == null) {
            throw new IllegalArgumentException(
                    "La fecha de factura es obligatoria."
            );
        }

        if (
                request.importeTotal() == null
                        || request.importeTotal()
                        .compareTo(
                                BigDecimal.ZERO
                        ) <= 0
        ) {
            throw new IllegalArgumentException(
                    "El importe del gasto debe ser mayor que cero."
            );
        }

        if (
                request.proveedor() == null
                        || request.proveedor()
                        .trim()
                        .isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "El proveedor es obligatorio."
            );
        }

        if (
                request.concepto() == null
                        || request.concepto()
                        .trim()
                        .isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "El concepto es obligatorio."
            );
        }
    }

    private void copiarDatosEditables(
            GastoGuardarRequest request,
            ContabilidadGasto gasto
    ) {
        gasto.setComunidadId(
                request.comunidadId()
        );

        gasto.setConcepto(
                limpiar(
                        request.concepto()
                )
        );

        gasto.setFechaFactura(
                request.fechaFactura()
        );

        gasto.setImporteTotal(
                request.importeTotal()
        );

        gasto.setNumeroFactura(
                limpiar(
                        request.numeroFactura()
                )
        );

        gasto.setProveedor(
                limpiar(
                        request.proveedor()
                )
        );

        gasto.setCuentaGastoId(
                request.cuentaGastoId()
        );
    }

    private String limpiar(
            String valor
    ) {
        if (valor == null) {
            return null;
        }

        String limpio =
                valor.trim();

        return limpio.isEmpty()
                ? null
                : limpio;
    }
}