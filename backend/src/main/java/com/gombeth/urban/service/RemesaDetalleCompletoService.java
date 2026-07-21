package com.gombeth.urban.service;

import com.gombeth.urban.dto.remesa.RemesaDetalleCompletoResponse;
import com.gombeth.urban.dto.remesa.RemesaLineaConceptoDetalleResponse;
import com.gombeth.urban.dto.remesa.RemesaLineaDetalleResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.FicheroGeneradoRepository;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;
import com.gombeth.urban.repository.RemesaLineaRepository;
import com.gombeth.urban.repository.VecinoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RemesaDetalleCompletoService {

    /*
     * Posiciones de los campos dentro de un registro
     * Norma 19-15 de 600 caracteres.
     */
    private static final int LONGITUD_MINIMA_REGISTRO_03 = 581;

    /*
     * La referencia generada contiene:
     *
     * reciboId + fecha(8) + hora(6) + númeroDato(3)
     */
    private static final int LONGITUD_SUFIJO_REFERENCIA = 17;

    private static final DateTimeFormatter FORMATO_FECHA_C19 =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Pattern PATRON_IMPORTE_CONCEPTO =
            Pattern.compile(
                    "(-?\\d+(?:[.,]\\d{2}))\\s*EUR$",
                    Pattern.CASE_INSENSITIVE
            );

    private final FicheroGeneradoRepository ficheroGeneradoRepository;
    private final ComunidadRepository comunidadRepository;
    private final RemesaLineaRepository remesaLineaRepository;
    private final RemesaLineaConceptoRepository remesaLineaConceptoRepository;
    private final VecinoRepository vecinoRepository;
    private final ContabilidadReciboRepository contabilidadReciboRepository;

    public RemesaDetalleCompletoService(
            FicheroGeneradoRepository ficheroGeneradoRepository,
            ComunidadRepository comunidadRepository,
            RemesaLineaRepository remesaLineaRepository,
            RemesaLineaConceptoRepository remesaLineaConceptoRepository,
            VecinoRepository vecinoRepository,
            ContabilidadReciboRepository contabilidadReciboRepository
    ) {
        this.ficheroGeneradoRepository = ficheroGeneradoRepository;
        this.comunidadRepository = comunidadRepository;
        this.remesaLineaRepository = remesaLineaRepository;
        this.remesaLineaConceptoRepository = remesaLineaConceptoRepository;
        this.vecinoRepository = vecinoRepository;
        this.contabilidadReciboRepository = contabilidadReciboRepository;
    }

    @Transactional(readOnly = true)
    public RemesaDetalleCompletoResponse obtenerDetalle(
            Long remesaId
    ) {

        FicheroGenerado remesa =
                ficheroGeneradoRepository
                        .findById(remesaId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "No existe la remesa con ID " + remesaId
                                )
                        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(remesa.getComunidadId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "No existe la comunidad asociada a la remesa."
                                )
                        );

        List<RemesaLinea> lineasPersistidas =
                remesaLineaRepository
                        .findByRemesaIdOrderByIdAsc(remesaId);

        boolean detalleReconstruido = false;
        String avisoDetalle = null;

        List<RemesaLineaDetalleResponse> lineasDetalle;

        if (!lineasPersistidas.isEmpty()) {

            /*
             * Remesa moderna: dispone de relaciones completas
             * en remesa_lineas.
             */
            lineasDetalle =
                    convertirLineasPersistidas(lineasPersistidas);

        } else if (
                remesa.getContenido() != null &&
                        !remesa.getContenido().isBlank()
        ) {

            /*
             * Remesa histórica: se reconstruye el detalle
             * leyendo los registros 03 del C19 almacenado.
             */
            lineasDetalle =
                    reconstruirDesdeC19(remesa.getContenido());

            detalleReconstruido = true;

            avisoDetalle =
                    "Esta remesa es histórica y no tenía líneas "
                            + "relacionales guardadas. El detalle se ha "
                            + "reconstruido automáticamente desde el "
                            + "fichero C19 almacenado.";

        } else {

            lineasDetalle =
                    Collections.emptyList();

            avisoDetalle =
                    "La remesa no dispone de líneas relacionales "
                            + "ni de contenido C19 para reconstruirlas.";
        }

        BigDecimal totalCalculado =
                sumarImportes(lineasDetalle);

        BigDecimal totalDomiciliadoCalculado =
                lineasDetalle.stream()
                        .filter(linea ->
                                Boolean.TRUE.equals(
                                        linea.domiciliado()
                                )
                        )
                        .map(RemesaLineaDetalleResponse::importe)
                        .filter(Objects::nonNull)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalNoDomiciliadoCalculado =
                lineasDetalle.stream()
                        .filter(linea ->
                                !Boolean.TRUE.equals(
                                        linea.domiciliado()
                                )
                        )
                        .map(RemesaLineaDetalleResponse::importe)
                        .filter(Objects::nonNull)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalImporte =
                remesa.getTotalImporte() != null
                        ? remesa.getTotalImporte()
                        : totalCalculado;

        BigDecimal totalDomiciliado =
                elegirTotal(
                        remesa.getTotalDomiciliado(),
                        totalDomiciliadoCalculado
                );

        BigDecimal totalNoDomiciliado =
                elegirTotal(
                        remesa.getTotalNoDomiciliado(),
                        totalNoDomiciliadoCalculado
                );

        Integer numeroRecibos =
                remesa.getNumeroRecibos();

        if (
                (numeroRecibos == null || numeroRecibos <= 0) &&
                        !lineasDetalle.isEmpty()
        ) {
            numeroRecibos =
                    lineasDetalle.size();
        }

        LocalDate fechaCobro =
                remesa.getFechaCobro();

        if (
                fechaCobro == null &&
                        detalleReconstruido
        ) {
            fechaCobro =
                    extraerFechaCobroC19(
                            remesa.getContenido()
                    ).orElse(null);
        }

        return new RemesaDetalleCompletoResponse(
                remesa.getId(),
                remesa.getComunidadId(),
                comunidad.getNombre(),
                remesa.getFechaCreacion(),
                fechaCobro,
                remesa.getEstado(),
                remesa.getEsquemaSepa(),
                totalImporte,
                totalDomiciliado,
                totalNoDomiciliado,
                numeroRecibos,
                remesa.getNombreArchivo(),
                detalleReconstruido,
                avisoDetalle,
                lineasDetalle
        );
    }

    private List<RemesaLineaDetalleResponse>
    convertirLineasPersistidas(
            List<RemesaLinea> lineas
    ) {

        return lineas.stream()
                .map(linea -> {

                    Vecino vecino =
                            vecinoRepository
                                    .findById(linea.getVecinoId())
                                    .orElse(null);

                    List<RemesaLineaConceptoDetalleResponse>
                            conceptos =
                            remesaLineaConceptoRepository
                                    .findByRemesaLineaIdOrderByOrdenAsc(
                                            linea.getId()
                                    )
                                    .stream()
                                    .map(this::convertirConcepto)
                                    .toList();

                    /*
                     * Algunas líneas antiguas tienen el concepto
                     * principal en remesa_lineas.concepto, pero no
                     * en remesa_linea_conceptos.
                     */
                    if (
                            conceptos.isEmpty() &&
                                    linea.getConcepto() != null &&
                                    !linea.getConcepto().isBlank()
                    ) {
                        conceptos =
                                List.of(
                                        new RemesaLineaConceptoDetalleResponse(
                                                -linea.getId(),
                                                linea.getConcepto(),
                                                valorOCero(
                                                        linea.getImporte()
                                                ),
                                                1,
                                                false
                                        )
                                );
                    }

                    return new RemesaLineaDetalleResponse(
                            linea.getId(),
                            linea.getVecinoId(),
                            vecino == null
                                    ? "Vecino no encontrado"
                                    : vecino.getNombre(),
                            linea.getReciboContableId(),
                            valorOCero(linea.getImporte()),
                            Boolean.TRUE.equals(
                                    linea.getDomiciliado()
                            ),
                            Boolean.TRUE.equals(
                                    linea.getIncluidoSepa()
                            ),
                            Boolean.TRUE.equals(
                                    linea.getPdfGenerado()
                            ),
                            Boolean.TRUE.equals(
                                    linea.getEmailEnviado()
                            ),
                            conceptos
                    );
                })
                .toList();
    }

    private RemesaLineaConceptoDetalleResponse
    convertirConcepto(
            RemesaLineaConcepto concepto
    ) {

        return new RemesaLineaConceptoDetalleResponse(
                concepto.getId(),
                concepto.getDescripcion(),
                concepto.getImporte(),
                concepto.getOrden(),
                concepto.getAgrupadoEnUltimaLinea()
        );
    }

    private List<RemesaLineaDetalleResponse>
    reconstruirDesdeC19(
            String contenido
    ) {

        LinkedHashMap<String, LineaHistorica>
                lineasPorReferencia =
                new LinkedHashMap<>();

        String[] registros =
                contenido.split("\\R");

        for (String registro : registros) {

            if (
                    registro == null ||
                            registro.length() <
                                    LONGITUD_MINIMA_REGISTRO_03
            ) {
                continue;
            }

            if (!registro.startsWith("03")) {
                continue;
            }

            String numeroDato =
                    subcadena(
                            registro,
                            7,
                            10
                    );

            if (
                    !"003".equals(numeroDato) &&
                            !"004".equals(numeroDato) &&
                            !"005".equals(numeroDato) &&
                            !"006".equals(numeroDato) &&
                            !"007".equals(numeroDato)
            ) {
                continue;
            }

            String referenciaAdeudo =
                    subcadena(
                            registro,
                            10,
                            45
                    ).trim();

            if (referenciaAdeudo.isBlank()) {
                continue;
            }

            LineaHistorica linea =
                    lineasPorReferencia
                            .computeIfAbsent(
                                    referenciaAdeudo,
                                    referencia ->
                                            new LineaHistorica(
                                                    extraerReciboId(
                                                            referencia
                                                    ).orElse(null),
                                                    subcadena(
                                                            registro,
                                                            118,
                                                            188
                                                    ).trim()
                                            )
                            );

            if ("003".equals(numeroDato)) {

                linea.importe =
                        extraerImporteCentimos(
                                subcadena(
                                        registro,
                                        88,
                                        99
                                )
                        );

                String titular =
                        subcadena(
                                registro,
                                118,
                                188
                        ).trim();

                if (!titular.isBlank()) {
                    linea.titular =
                            titular;
                }
            }

            String descripcionConcepto =
                    subcadena(
                            registro,
                            441,
                            581
                    ).trim();

            if (!descripcionConcepto.isBlank()) {

                linea.conceptos.add(
                        new ConceptoHistorico(
                                descripcionConcepto,
                                numeroDato
                        )
                );
            }
        }

        List<Long> reciboIds =
                lineasPorReferencia
                        .values()
                        .stream()
                        .map(linea ->
                                linea.reciboContableId
                        )
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        Map<Long, ContabilidadRecibo>
                recibosPorId;

        if (reciboIds.isEmpty()) {

            recibosPorId =
                    Collections.emptyMap();

        } else {

            recibosPorId =
                    contabilidadReciboRepository
                            .findAllById(reciboIds)
                            .stream()
                            .collect(
                                    Collectors.toMap(
                                            ContabilidadRecibo::getId,
                                            recibo -> recibo
                                    )
                            );
        }

        List<RemesaLineaDetalleResponse> resultado =
                new ArrayList<>();

        long idLineaVirtual = -1L;
        long idConceptoVirtual = -1L;

        for (
                LineaHistorica linea :
                lineasPorReferencia.values()
        ) {

            ContabilidadRecibo recibo =
                    linea.reciboContableId == null
                            ? null
                            : recibosPorId.get(
                            linea.reciboContableId
                    );

            Long vecinoId =
                    recibo == null
                            ? 0L
                            : recibo.getVecinoId();

            List<RemesaLineaConceptoDetalleResponse>
                    conceptos =
                    new ArrayList<>();

            for (
                    int indice = 0;
                    indice < linea.conceptos.size();
                    indice++
            ) {

                ConceptoHistorico concepto =
                        linea.conceptos.get(indice);

                BigDecimal importeConcepto =
                        extraerImporteConcepto(
                                concepto.descripcion
                        ).orElse(
                                indice == 0
                                        ? linea.importe
                                        : BigDecimal.ZERO
                        );

                conceptos.add(
                        new RemesaLineaConceptoDetalleResponse(
                                idConceptoVirtual--,
                                concepto.descripcion,
                                importeConcepto,
                                indice + 1,
                                "007".equals(
                                        concepto.numeroDato
                                )
                        )
                );
            }

            if (conceptos.isEmpty()) {

                conceptos.add(
                        new RemesaLineaConceptoDetalleResponse(
                                idConceptoVirtual--,
                                "Concepto no disponible "
                                        + "en el C19 histórico",
                                linea.importe,
                                1,
                                false
                        )
                );
            }

            resultado.add(
                    new RemesaLineaDetalleResponse(
                            idLineaVirtual--,
                            vecinoId,
                            linea.titular == null ||
                                    linea.titular.isBlank()
                                    ? "Titular no identificado"
                                    : linea.titular,
                            linea.reciboContableId,
                            linea.importe,
                            true,
                            true,
                            false,
                            false,
                            conceptos
                    )
            );
        }

        return resultado;
    }

    private Optional<Long> extraerReciboId(
            String referenciaAdeudo
    ) {

        if (
                referenciaAdeudo == null ||
                        referenciaAdeudo.length() <=
                                LONGITUD_SUFIJO_REFERENCIA
        ) {
            return Optional.empty();
        }

        String parteRecibo =
                referenciaAdeudo.substring(
                        0,
                        referenciaAdeudo.length() -
                                LONGITUD_SUFIJO_REFERENCIA
                ).trim();

        if (!parteRecibo.matches("\\d+")) {
            return Optional.empty();
        }

        try {

            return Optional.of(
                    Long.parseLong(parteRecibo)
            );

        } catch (NumberFormatException error) {

            return Optional.empty();
        }
    }

    private BigDecimal extraerImporteCentimos(
            String texto
    ) {

        String digitos =
                texto == null
                        ? ""
                        : texto.replaceAll(
                        "[^0-9]",
                        ""
                );

        if (digitos.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {

            return new BigDecimal(digitos)
                    .movePointLeft(2);

        } catch (NumberFormatException error) {

            return BigDecimal.ZERO;
        }
    }

    private Optional<BigDecimal>
    extraerImporteConcepto(
            String descripcion
    ) {

        if (
                descripcion == null ||
                        descripcion.isBlank()
        ) {
            return Optional.empty();
        }

        Matcher matcher =
                PATRON_IMPORTE_CONCEPTO
                        .matcher(
                                descripcion.trim()
                        );

        if (!matcher.find()) {
            return Optional.empty();
        }

        try {

            String importe =
                    matcher.group(1)
                            .replace(',', '.');

            return Optional.of(
                    new BigDecimal(importe)
            );

        } catch (NumberFormatException error) {

            return Optional.empty();
        }
    }

    private Optional<LocalDate>
    extraerFechaCobroC19(
            String contenido
    ) {

        if (
                contenido == null ||
                        contenido.isBlank()
        ) {
            return Optional.empty();
        }

        for (
                String registro :
                contenido.split("\\R")
        ) {

            if (
                    registro == null ||
                            !registro.startsWith("02") ||
                            registro.length() < 53
            ) {
                continue;
            }

            String fecha =
                    subcadena(
                            registro,
                            45,
                            53
                    );

            try {

                return Optional.of(
                        LocalDate.parse(
                                fecha,
                                FORMATO_FECHA_C19
                        )
                );

            } catch (
                    DateTimeParseException error
            ) {

                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private BigDecimal sumarImportes(
            List<RemesaLineaDetalleResponse> lineas
    ) {

        return lineas.stream()
                .map(RemesaLineaDetalleResponse::importe)
                .filter(Objects::nonNull)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private BigDecimal elegirTotal(
            BigDecimal guardado,
            BigDecimal calculado
    ) {

        if (
                guardado != null &&
                        guardado.compareTo(
                                BigDecimal.ZERO
                        ) != 0
        ) {
            return guardado;
        }

        return valorOCero(calculado);
    }

    private BigDecimal valorOCero(
            BigDecimal valor
    ) {

        return valor == null
                ? BigDecimal.ZERO
                : valor;
    }

    private String subcadena(
            String texto,
            int inicio,
            int fin
    ) {

        if (
                texto == null ||
                        inicio >= texto.length()
        ) {
            return "";
        }

        return texto.substring(
                inicio,
                Math.min(
                        fin,
                        texto.length()
                )
        );
    }

    private static final class LineaHistorica {

        private final Long reciboContableId;

        private String titular;

        private BigDecimal importe =
                BigDecimal.ZERO;

        private final List<ConceptoHistorico>
                conceptos =
                new ArrayList<>();

        private LineaHistorica(
                Long reciboContableId,
                String titular
        ) {
            this.reciboContableId =
                    reciboContableId;

            this.titular =
                    titular == null
                            ? ""
                            : titular;
        }
    }

    private record ConceptoHistorico(
            String descripcion,
            String numeroDato
    ) {
    }
}