package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.repository.ContabilidadGastoRepository;
import com.gombeth.urban.service.storage.GastoPdfStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Path;

@Service
public class ContabilidadGastoPdfService {

    private final ContabilidadGastoRepository
            gastoRepository;

    private final GastoPdfStorageService
            gastoPdfStorageService;

    public ContabilidadGastoPdfService(
            ContabilidadGastoRepository gastoRepository,
            GastoPdfStorageService gastoPdfStorageService
    ) {
        this.gastoRepository =
                gastoRepository;

        this.gastoPdfStorageService =
                gastoPdfStorageService;
    }

    /**
     * Asocia un PDF a un gasto que todavía no tenga
     * documento vinculado.
     *
     * Durante la convivencia evitamos reemplazar
     * silenciosamente documentos históricos.
     */
    @Transactional
    public ContabilidadGasto subirPdf(
            Long gastoId,
            MultipartFile archivo
    ) {
        ContabilidadGasto gasto =
                obtenerGastoParaActualizar(
                        gastoId
                );

        if (
                gasto.getRutaPdf() != null
                        && !gasto.getRutaPdf().isBlank()
        ) {
            throw new IllegalStateException(
                    "El gasto ya tiene un PDF asociado."
            );
        }

        String nombreArchivo =
                gastoPdfStorageService.guardarPdf(
                        archivo
                );

        /*
         * El fichero físico ya existe en este punto.
         *
         * Si posteriormente la transacción de base de datos
         * termina en rollback, eliminamos exclusivamente ese
         * fichero recién creado para no dejar documentos
         * huérfanos.
         */
        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCompletion(
                                    int status
                            ) {
                                if (
                                        status
                                                == TransactionSynchronization
                                                .STATUS_ROLLED_BACK
                                ) {
                                    gastoPdfStorageService
                                            .eliminarPdfRecienCreado(
                                                    nombreArchivo
                                            );
                                }
                            }
                        }
                );

        gasto.setRutaPdf(
                nombreArchivo
        );

        return gastoRepository.save(
                gasto
        );
    }

    /**
     * Recupera la ruta física del PDF asociado.
     *
     * Funciona tanto con documentos nuevos como con
     * nombres históricos almacenados en ruta_pdf.
     */
    @Transactional(readOnly = true)
    public Path obtenerPdf(
            Long gastoId
    ) {
        ContabilidadGasto gasto =
                obtenerGasto(
                        gastoId
                );

        return gastoPdfStorageService.obtenerPdf(
                gasto.getRutaPdf()
        );
    }

    @Transactional(readOnly = true)
    public boolean tienePdf(
            Long gastoId
    ) {
        ContabilidadGasto gasto =
                obtenerGasto(
                        gastoId
                );

        return gastoPdfStorageService.existePdf(
                gasto.getRutaPdf()
        );
    }

    /**
     * Obtiene el gasto bloqueando su fila mientras dura
     * la transacción.
     *
     * Se utiliza únicamente para operaciones que modifican
     * la asociación del PDF, evitando que dos peticiones V2
     * adjunten simultáneamente documentos al mismo gasto.
     */
    private ContabilidadGasto obtenerGastoParaActualizar(
            Long gastoId
    ) {
        if (
                gastoId == null
                        || gastoId <= 0
        ) {
            throw new IllegalArgumentException(
                    "El identificador del gasto no es válido."
            );
        }

        return gastoRepository
                .findByIdForUpdate(
                        gastoId
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el gasto "
                                        + gastoId
                        )
                );
    }

    private ContabilidadGasto obtenerGasto(
            Long gastoId
    ) {
        if (
                gastoId == null
                        || gastoId <= 0
        ) {
            throw new IllegalArgumentException(
                    "El identificador del gasto no es válido."
            );
        }

        return gastoRepository
                .findById(
                        gastoId
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el gasto "
                                        + gastoId
                        )
                );
    }
}