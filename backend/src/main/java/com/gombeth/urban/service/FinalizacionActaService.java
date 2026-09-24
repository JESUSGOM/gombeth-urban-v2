package com.gombeth.urban.service;

import com.gombeth.urban.entity.Acta;
import com.gombeth.urban.entity.EstadoActa;
import com.gombeth.urban.repository.ActaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FinalizacionActaService {

    private final ActaRepository actaRepository;

    private final PdfActaService pdfActaService;

    public FinalizacionActaService(
            ActaRepository actaRepository,
            PdfActaService pdfActaService
    ) {
        this.actaRepository =
                actaRepository;

        this.pdfActaService =
                pdfActaService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Acta finalizar(
            Long actaId
    ) throws Exception {

        if (
                actaId == null
                        || actaId <= 0
        ) {
            throw new IllegalArgumentException(
                    "El identificador del acta no es válido."
            );
        }

        Acta acta =
                actaRepository
                        .findByIdForUpdate(
                                actaId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No existe el acta "
                                                + actaId
                                )
                        );

        if (acta.getEstado() != EstadoActa.BORRADOR) {
            throw new IllegalStateException(
                    "Solo se pueden finalizar actas "
                            + "en estado BORRADOR."
            );
        }

        acta.setEstado(
                EstadoActa.CERRADA
        );

        acta.setTokenPresidente(
                UUID.randomUUID()
                        .toString()
        );

        /*
         * Primero exigimos que la generación y firma
         * del PDF termine correctamente.
         *
         * Si falla, no llegamos al save y la transacción
         * se marca para rollback también ante Exception.
         */
        pdfActaService.generarPdfActa(
                acta
        );

        return actaRepository.save(
                acta
        );
    }
}