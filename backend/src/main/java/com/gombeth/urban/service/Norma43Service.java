package com.gombeth.urban.service;

import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class Norma43Service {

    private final MovimientoBancarioRepository movimientoBancarioRepository;

    public Norma43Service(
            MovimientoBancarioRepository movimientoBancarioRepository
    ) {
        this.movimientoBancarioRepository = movimientoBancarioRepository;
    }

    public List<MovimientoBancario> importarContenido(
            Long comunidadId,
            String contenido
    ) {
        if (comunidadId == null) {
            throw new IllegalArgumentException("La comunidad es obligatoria.");
        }

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido del fichero Norma 43 está vacío.");
        }

        List<MovimientoBancario> movimientos = new ArrayList<>();

        String[] lineas = contenido.split("\\R");

        for (String linea : lineas) {
            if (linea == null || linea.isBlank()) {
                continue;
            }

            String tipoRegistro = obtenerTipoRegistro(linea);

            if ("22".equals(tipoRegistro)) {
                MovimientoBancario movimiento = parsearRegistroMovimiento(
                        comunidadId,
                        linea
                );

                if (!existeMovimiento(movimiento)) {
                    movimientos.add(movimiento);
                }
            }
        }

        if (movimientos.isEmpty()) {
            return List.of();
        }

        return movimientoBancarioRepository.saveAll(movimientos);
    }

    private boolean existeMovimiento(MovimientoBancario movimiento) {
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
            String linea
    ) {
        MovimientoBancario movimiento = new MovimientoBancario();

        movimiento.setComunidadId(comunidadId);
        movimiento.setProcesado(false);
        movimiento.setConciliado(false);

        LocalDate fechaOperacion = extraerFechaOperacion(linea);
        LocalDate fechaValor = extraerFechaValor(linea);

        movimiento.setFechaOperacion(fechaOperacion);
        movimiento.setFechaValor(fechaValor);

        movimiento.setSigno(extraerSigno(linea));
        movimiento.setImporte(extraerImporte(linea));

        movimiento.setConcepto(extraerConcepto(linea));
        movimiento.setConceptoCompleto(linea);
        movimiento.setReferenciaBancaria(generarReferenciaTemporal(linea));

        return movimiento;
    }

    private String obtenerTipoRegistro(String linea) {
        if (linea.length() < 2) {
            return "";
        }

        return linea.substring(0, 2);
    }

    private LocalDate extraerFechaOperacion(String linea) {
        String fecha = extraerSeguro(linea, 2, 8);

        return parseFechaAaMmDd(fecha);
    }

    private LocalDate extraerFechaValor(String linea) {
        String fecha = extraerSeguro(linea, 8, 14);

        return parseFechaAaMmDd(fecha);
    }

    private LocalDate parseFechaAaMmDd(String fecha) {
        try {
            if (fecha == null || fecha.length() != 6 || !fecha.matches("\\d{6}")) {
                return LocalDate.now();
            }

            int anio = Integer.parseInt(fecha.substring(0, 2));
            int mes = Integer.parseInt(fecha.substring(2, 4));
            int dia = Integer.parseInt(fecha.substring(4, 6));

            int anioCompleto = anio >= 70 ? 1900 + anio : 2000 + anio;

            return LocalDate.of(anioCompleto, mes, dia);

        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private String extraerSigno(String linea) {
        String signo = extraerSeguro(linea, 30, 31);

        if ("H".equalsIgnoreCase(signo)) {
            return "1";
        }

        if ("D".equalsIgnoreCase(signo)) {
            return "2";
        }

        if ("N".equalsIgnoreCase(signo)) {
            return "2";
        }

        return "2";
    }

    private BigDecimal extraerImporte(String linea) {
        String importeTexto = extraerSeguro(linea, 31, 45)
                .replaceAll("[^0-9]", "");

        if (importeTexto.isBlank()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(importeTexto)
                .movePointLeft(2);
    }

    private String extraerConcepto(String linea) {
        String concepto = extraerSeguro(linea, 43, linea.length()).trim();

        if (concepto.isBlank()) {
            return "Movimiento importado Norma 43";
        }

        if (concepto.length() > 500) {
            return concepto.substring(0, 500);
        }

        return concepto;
    }

    private String generarReferenciaTemporal(String linea) {
        String limpia = linea.trim();

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