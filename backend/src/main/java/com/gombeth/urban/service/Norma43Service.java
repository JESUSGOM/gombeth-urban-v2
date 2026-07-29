package com.gombeth.urban.service;

import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class Norma43Service {

    private static final int LONGITUD_MINIMA_REGISTRO_22 = 42;
    private static final int LONGITUD_MAXIMA_CONCEPTO = 500;

    private final MovimientoBancarioRepository movimientoBancarioRepository;

    public Norma43Service(
            MovimientoBancarioRepository movimientoBancarioRepository
    ) {
        this.movimientoBancarioRepository = movimientoBancarioRepository;
    }

    /**
     * Analiza un contenido Norma 43 sin guardar datos en la base de datos.
     *
     * Se procesan los registros:
     * - 22: movimiento principal.
     * - 23: ampliación multilínea del concepto del movimiento anterior.
     * - 33: cierre de la cuenta y finalización del último movimiento.
     */
    public List<MovimientoBancario> previsualizarContenido(
            Long comunidadId,
            String contenido
    ) {
        validarEntrada(comunidadId, contenido);

        List<MovimientoBancario> movimientos = new ArrayList<>();

        MovimientoBancario movimientoActual = null;
        String registro22Actual = null;
        StringBuilder conceptoAcumulado = new StringBuilder();

        String[] lineas = contenido.split("\\R", -1);

        for (int indice = 0; indice < lineas.length; indice++) {
            String linea = lineas[indice];
            int numeroLinea = indice + 1;

            if (linea == null || linea.isBlank()) {
                continue;
            }

            String tipoRegistro = obtenerTipoRegistro(linea);

            switch (tipoRegistro) {
                case "22" -> {
                    if (movimientoActual != null) {
                        finalizarMovimiento(
                                movimientos,
                                movimientoActual,
                                registro22Actual,
                                conceptoAcumulado
                        );
                    }

                    movimientoActual = parsearRegistroMovimiento(
                            comunidadId,
                            linea,
                            numeroLinea
                    );

                    registro22Actual = linea;
                    conceptoAcumulado = new StringBuilder();

                    agregarFragmentoConcepto(
                            conceptoAcumulado,
                            extraerSeguro(linea, 52, 80)
                    );
                }

                case "23" -> {
                    if (movimientoActual != null) {
                        agregarFragmentoConcepto(
                                conceptoAcumulado,
                                extraerSeguro(linea, 4, linea.length())
                        );
                    }
                }

                case "33" -> {
                    if (movimientoActual != null) {
                        finalizarMovimiento(
                                movimientos,
                                movimientoActual,
                                registro22Actual,
                                conceptoAcumulado
                        );

                        movimientoActual = null;
                        registro22Actual = null;
                        conceptoAcumulado = new StringBuilder();
                    }
                }

                default -> {
                    // Los demás registros no forman movimientos en esta fase.
                }
            }
        }

        if (movimientoActual != null) {
            finalizarMovimiento(
                    movimientos,
                    movimientoActual,
                    registro22Actual,
                    conceptoAcumulado
            );
        }

        return movimientos;
    }

    /**
     * Conserva el endpoint de importación existente, pero utiliza el parser
     * sin persistencia como única fuente de movimientos.
     */
    public List<MovimientoBancario> importarContenido(
            Long comunidadId,
            String contenido
    ) {
        List<MovimientoBancario> movimientosAnalizados =
                previsualizarContenido(
                        comunidadId,
                        contenido
                );

        List<MovimientoBancario> movimientosNuevos =
                movimientosAnalizados.stream()
                        .filter(movimiento -> !existeMovimiento(movimiento))
                        .toList();

        if (movimientosNuevos.isEmpty()) {
            return List.of();
        }

        return movimientoBancarioRepository.saveAll(movimientosNuevos);
    }

    private void validarEntrada(
            Long comunidadId,
            String contenido
    ) {
        if (comunidadId == null) {
            throw new IllegalArgumentException(
                    "La comunidad es obligatoria."
            );
        }

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException(
                    "El contenido del fichero Norma 43 está vacío."
            );
        }
    }

    private boolean existeMovimiento(
            MovimientoBancario movimiento
    ) {
        return movimientoBancarioRepository
                .existsByComunidadIdAndFechaOperacionAndFechaValorAndImporteAndSignoAndReferenciaBancaria(
                        movimiento.getComunidadId(),
                        movimiento.getFechaOperacion(),
                        movimiento.getFechaValor(),
                        movimiento.getImporte(),
                        movimiento.getSigno(),
                        movimiento.getReferenciaBancaria()
                );
    }

    private MovimientoBancario parsearRegistroMovimiento(
            Long comunidadId,
            String linea,
            int numeroLinea
    ) {
        if (linea.length() < LONGITUD_MINIMA_REGISTRO_22) {
            throw new IllegalArgumentException(
                    "El registro 22 de la línea "
                            + numeroLinea
                            + " es demasiado corto."
            );
        }

        MovimientoBancario movimiento = new MovimientoBancario();

        movimiento.setComunidadId(comunidadId);
        movimiento.setProcesado(false);
        movimiento.setConciliado(false);

        movimiento.setFechaOperacion(
                parsearFechaAaMmDd(
                        extraerSeguro(linea, 10, 16),
                        numeroLinea,
                        "operación"
                )
        );

        movimiento.setFechaValor(
                parsearFechaAaMmDd(
                        extraerSeguro(linea, 16, 22),
                        numeroLinea,
                        "valor"
                )
        );

        movimiento.setSigno(
                extraerSigno(
                        linea,
                        numeroLinea
                )
        );

        movimiento.setImporte(
                extraerImporte(
                        linea,
                        numeroLinea
                )
        );

        movimiento.setDocumentoExtra(
                limpiarTexto(
                        extraerSeguro(linea, 42, 52)
                )
        );

        return movimiento;
    }

    private void finalizarMovimiento(
            List<MovimientoBancario> movimientos,
            MovimientoBancario movimiento,
            String registro22,
            StringBuilder conceptoAcumulado
    ) {
        String concepto = limpiarConcepto(
                conceptoAcumulado == null
                        ? ""
                        : conceptoAcumulado.toString()
        );

        if (concepto.isBlank()) {
            concepto = "Movimiento importado Norma 43";
        }

        movimiento.setConcepto(concepto);
        movimiento.setConceptoCompleto(concepto);
        movimiento.setReferenciaBancaria(
                generarReferenciaTemporal(registro22)
        );

        movimientos.add(movimiento);
    }

    private void agregarFragmentoConcepto(
            StringBuilder acumulado,
            String fragmento
    ) {
        String limpio = limpiarTexto(fragmento);

        if (limpio.isBlank()) {
            return;
        }

        if (!acumulado.isEmpty()) {
            acumulado.append(' ');
        }

        acumulado.append(limpio);
    }

    private String obtenerTipoRegistro(String linea) {
        if (linea.length() < 2) {
            return "";
        }

        return linea.substring(0, 2);
    }

    private LocalDate parsearFechaAaMmDd(
            String fecha,
            int numeroLinea,
            String tipoFecha
    ) {
        if (fecha == null || !fecha.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                    "La fecha de "
                            + tipoFecha
                            + " del registro 22 de la línea "
                            + numeroLinea
                            + " no tiene formato AAMMDD."
            );
        }

        int anio = Integer.parseInt(fecha.substring(0, 2));
        int mes = Integer.parseInt(fecha.substring(2, 4));
        int dia = Integer.parseInt(fecha.substring(4, 6));

        int anioCompleto =
                anio >= 70
                        ? 1900 + anio
                        : 2000 + anio;

        try {
            return LocalDate.of(
                    anioCompleto,
                    mes,
                    dia
            );
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(
                    "La fecha de "
                            + tipoFecha
                            + " del registro 22 de la línea "
                            + numeroLinea
                            + " no es válida: "
                            + fecha
                            + ".",
                    e
            );
        }
    }

    private String extraerSigno(
            String linea,
            int numeroLinea
    ) {
        String signo = limpiarTexto(
                extraerSeguro(linea, 27, 28)
        );

        if ("1".equals(signo) || "2".equals(signo)) {
            return signo;
        }

        throw new IllegalArgumentException(
                "El signo del registro 22 de la línea "
                        + numeroLinea
                        + " debe ser 1 o 2."
        );
    }

    private BigDecimal extraerImporte(
            String linea,
            int numeroLinea
    ) {
        String importeTexto =
                extraerSeguro(linea, 28, 42);

        if (!importeTexto.matches("\\d{14}")) {
            throw new IllegalArgumentException(
                    "El importe del registro 22 de la línea "
                            + numeroLinea
                            + " debe contener 14 dígitos."
            );
        }

        return new BigDecimal(importeTexto)
                .movePointLeft(2);
    }

    private String limpiarConcepto(String texto) {
        String limpio = limpiarTexto(texto);

        if (limpio.length() <= LONGITUD_MAXIMA_CONCEPTO) {
            return limpio;
        }

        return limpio.substring(
                0,
                LONGITUD_MAXIMA_CONCEPTO - 3
        ) + "...";
    }

    private String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String generarReferenciaTemporal(String linea) {
        String limpia = limpiarTexto(linea);

        if (limpia.length() <= 50) {
            return limpia;
        }

        return limpia.substring(0, 50);
    }

    private String extraerSeguro(
            String texto,
            int desde,
            int hasta
    ) {
        if (texto == null || texto.length() <= desde) {
            return "";
        }

        int fin = Math.min(hasta, texto.length());

        return texto.substring(desde, fin);
    }
}