package com.gombeth.urban.service;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.Vecino;
import org.springframework.stereotype.Service;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Service
public class SepaC19Service {

    private static final int LONGITUD_REGISTRO = 600;
    private static final String CODIGO_NORMA_1915 = "19154";

    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter TIME_STAMP =
            DateTimeFormatter.ofPattern("HHmmss");

    private static final Pattern DIACRITICS =
            Pattern.compile("[\\p{InCombiningDiacriticalMarks}]");

    private static final Pattern NON_ALPHANUMERIC =
            Pattern.compile("[^A-Z0-9 ]");

    private final RemesaLineaConceptoRepository remesaLineaConceptoRepository;

    public SepaC19Service(
            RemesaLineaConceptoRepository remesaLineaConceptoRepository
    ) {
        this.remesaLineaConceptoRepository = remesaLineaConceptoRepository;
    }

    public String generarC19(
            FicheroGenerado remesa,
            Comunidad comunidad,
            List<RemesaLinea> lineas,
            List<Vecino> vecinos
    ) {

        StringBuilder file = new StringBuilder();

        String hoy = LocalDate.now().format(ISO_DATE);
        String ahora = LocalTime.now().format(TIME_STAMP);

        String fechaCobro =
                remesa.getFechaCobro() != null
                        ? remesa.getFechaCobro().format(ISO_DATE)
                        : hoy;

        String idAcreedor =
                safe(comunidad.getIdentificadorAcreedor());

        String ibanComunidad =
                safe(comunidad.getIban()).replace(" ", "");

        String entidadOficina =
                ibanComunidad.length() >= 12
                        ? ibanComunidad.substring(4, 12)
                        : "00000000";

        String idPresentador = idAcreedor;
        String nombrePresentador = comunidad.getNombre();

        String idFicheroRef =
                "PRE"
                        + hoy
                        + ahora
                        + "000"
                        + ultimos(idPresentador, 9);

        String r01 =
                "01"
                        + CODIGO_NORMA_1915
                        + "001"
                        + completar(idPresentador, 35)
                        + completar(nombrePresentador, 70)
                        + hoy
                        + completar(idFicheroRef, 35)
                        + completar(entidadOficina, 8)
                        + completar("", 434);

        String r02 =
                "02"
                        + CODIGO_NORMA_1915
                        + "002"
                        + completar(idAcreedor, 35)
                        + fechaCobro
                        + completar(comunidad.getNombre(), 70)
                        + completar(comunidad.getDireccion(), 50)
                        + completar(comunidad.getPoblacion(), 50)
                        + completar(".", 40)
                        + "ES"
                        + completar(ibanComunidad, 34)
                        + completar("", 301);

        file.append(completarRegistro(r01)).append("\n");
        file.append(completarRegistro(r02)).append("\n");

        BigDecimal totalRemesa = BigDecimal.ZERO;
        int adeudos003 = 0;
        int registros03 = 0;

        for (RemesaLinea linea : lineas) {

            if (!Boolean.TRUE.equals(linea.getIncluidoSepa())) {
                continue;
            }

            Vecino vecino =
                    buscarVecino(
                            vecinos,
                            linea.getVecinoId()
                    );

            if (vecino == null) {
                continue;
            }

            if (!vecino.isDomiciliado()) {
                continue;
            }

            if (vecino.getIban() == null ||
                    vecino.getIban().isBlank()) {
                continue;
            }

            if (vecino.getReferenciaMandato() == null ||
                    vecino.getReferenciaMandato().isBlank()) {
                continue;
            }

            BigDecimal importe =
                    linea.getImporte() == null
                            ? BigDecimal.ZERO
                            : linea.getImporte();

            if (importe.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String referenciaAdeudo =
                    linea.getReciboContableId()
                            + hoy
                            + ahora
                            + "003";

            List<String> conceptos =
                    obtenerConceptosC19(linea);

            for (int i = 0; i < conceptos.size(); i++) {

                String numeroDato =
                        switch (i) {
                            case 0 -> "003";
                            case 1 -> "004";
                            case 2 -> "005";
                            case 3 -> "006";
                            default -> "007";
                        };

                BigDecimal importeLinea =
                        "003".equals(numeroDato)
                                ? importe
                                : BigDecimal.ZERO;

                if ("003".equals(numeroDato)) {
                    totalRemesa =
                            totalRemesa.add(importe);

                    adeudos003++;
                }

                String r03 =
                        "03"
                                + CODIGO_NORMA_1915
                                + numeroDato
                                + completar(referenciaAdeudo, 35)
                                + completar(vecino.getReferenciaMandato(), 35)
                                + "RCUR"
                                + "    "
                                + formatearImporte(importeLinea, 11)
                                + hoy
                                + completar(vecino.getBic(), 11)
                                + completar(vecino.getNombre(), 70)
                                + completar(direccionVecino(vecino, comunidad), 50)
                                + completar(poblacionVecino(vecino, comunidad), 50)
                                + completar(".", 40)
                                + "ES"
                                + completar("", 72)
                                + "A"
                                + completar(
                                vecino.getIban().replace(" ", ""),
                                34
                        )
                                + completar("", 4)
                                + completar(conceptos.get(i), 140);

                file.append(completarRegistro(r03)).append("\n");

                registros03++;
            }
        }

        int count04 = 1 + registros03 + 1;
        int count05 = count04 + 1;
        int totalRegistrosFichero = 1 + count05 + 1;

        String r04 =
                "04"
                        + completar(idAcreedor, 35)
                        + fechaCobro
                        + formatearImporte(totalRemesa, 17)
                        + padLeft(String.valueOf(adeudos003), 8, '0')
                        + padLeft(String.valueOf(count04), 10, '0');

        String r05 =
                "05"
                        + completar(idAcreedor, 35)
                        + formatearImporte(totalRemesa, 17)
                        + padLeft(String.valueOf(adeudos003), 8, '0')
                        + padLeft(String.valueOf(count05), 10, '0');

        String r99 =
                "99"
                        + formatearImporte(totalRemesa, 17)
                        + padLeft(String.valueOf(adeudos003), 8, '0')
                        + padLeft(String.valueOf(totalRegistrosFichero), 10, '0');

        file.append(completarRegistro(r04)).append("\n");
        file.append(completarRegistro(r05)).append("\n");
        file.append(completarRegistro(r99)).append("\n");

        return file.toString();
    }

    private List<String> obtenerConceptosC19(RemesaLinea linea) {

        List<RemesaLineaConcepto> conceptos =
                remesaLineaConceptoRepository
                        .findByRemesaLineaIdOrderByOrdenAsc(
                                linea.getId()
                        );

        if (conceptos.isEmpty()) {
            return dividirConcepto(linea.getConcepto());
        }

        List<String> resultado =
                new ArrayList<>();

        for (RemesaLineaConcepto concepto : conceptos) {

            String descripcion =
                    concepto.getDescripcion() == null
                            ? "Concepto"
                            : concepto.getDescripcion();

            String importe =
                    concepto.getImporte() == null
                            ? "0.00"
                            : concepto.getImporte().toPlainString();

            resultado.add(
                    limitar(
                            descripcion + " " + importe + " EUR",
                            140
                    )
            );
        }

        return resultado;
    }

    private List<String> dividirConcepto(String concepto) {

        String texto =
                concepto == null || concepto.isBlank()
                        ? "RECIBO COMUNIDAD"
                        : concepto;

        String[] partes =
                texto.split("/");

        java.util.ArrayList<String> resultado =
                new java.util.ArrayList<>();

        for (String parte : partes) {
            String limpio = parte.trim();

            if (!limpio.isBlank()) {
                resultado.add(limitar(limpio, 140));
            }
        }

        if (resultado.isEmpty()) {
            resultado.add("RECIBO COMUNIDAD");
        }

        if (resultado.size() <= 5) {
            return resultado;
        }

        java.util.ArrayList<String> limitado =
                new java.util.ArrayList<>();

        limitado.add(resultado.get(0));
        limitado.add(resultado.get(1));
        limitado.add(resultado.get(2));
        limitado.add(resultado.get(3));

        String agrupado =
                "OTROS CONCEPTOS "
                        + String.join(
                        " ",
                        resultado.subList(4, resultado.size())
                );

        limitado.add(limitar(agrupado, 140));

        return limitado;
    }

    private Vecino buscarVecino(
            List<Vecino> vecinos,
            Long vecinoId
    ) {
        return vecinos
                .stream()
                .filter(v -> v.getId().equals(vecinoId))
                .findFirst()
                .orElse(null);
    }

    private String direccionVecino(
            Vecino vecino,
            Comunidad comunidad
    ) {
        if (vecino.getDireccion() != null &&
                !vecino.getDireccion().isBlank()) {
            return vecino.getDireccion();
        }

        return comunidad.getDireccion();
    }

    private String poblacionVecino(
            Vecino vecino,
            Comunidad comunidad
    ) {
        if (vecino.getPoblacion() != null &&
                !vecino.getPoblacion().isBlank()) {
            return vecino.getPoblacion();
        }

        return comunidad.getPoblacion();
    }

    private String completarRegistro(String contenido) {

        if (contenido.length() >= LONGITUD_REGISTRO) {
            return contenido.substring(0, LONGITUD_REGISTRO);
        }

        StringBuilder sb =
                new StringBuilder(contenido);

        while (sb.length() < LONGITUD_REGISTRO) {
            sb.append(" ");
        }

        return sb.toString();
    }

    private String completar(
            String texto,
            int longitud
    ) {
        String res =
                normalizarTexto(texto);

        if (res.length() >= longitud) {
            return res.substring(0, longitud);
        }

        return String.format(
                "%-" + longitud + "s",
                res
        );
    }

    private String normalizarTexto(String texto) {

        if (texto == null) {
            return "";
        }

        String temp =
                Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                );

        temp =
                DIACRITICS
                        .matcher(temp)
                        .replaceAll("");

        return NON_ALPHANUMERIC
                .matcher(temp.toUpperCase())
                .replaceAll(" ")
                .trim();
    }

    private String formatearImporte(
            BigDecimal importe,
            int longitud
    ) {
        if (importe == null) {
            importe = BigDecimal.ZERO;
        }

        long centimos =
                importe
                        .multiply(new BigDecimal("100"))
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValue();

        return padLeft(
                String.valueOf(centimos),
                longitud,
                '0'
        );
    }

    private String padLeft(
            String s,
            int n,
            char c
    ) {
        return String
                .format("%" + n + "s", s)
                .replace(' ', c);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String ultimos(
            String value,
            int length
    ) {
        String limpio =
                safe(value).replace(" ", "");

        if (limpio.length() <= length) {
            return limpio;
        }

        return limpio.substring(
                limpio.length() - length
        );
    }

    private String limitar(
            String texto,
            int max
    ) {
        String limpio =
                normalizarTexto(texto);

        if (limpio.length() <= max) {
            return limpio;
        }

        return limpio.substring(0, max);
    }
}