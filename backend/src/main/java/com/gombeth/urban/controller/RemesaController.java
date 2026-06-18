package com.gombeth.urban.controller;

import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.FicheroGeneradoRepository;
import com.gombeth.urban.repository.RemesaLineaRepository;
import com.gombeth.urban.repository.VecinoRepository;
import org.springframework.web.bind.annotation.*;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.service.SepaCoreXmlService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import com.gombeth.urban.dto.RemesaResumenResponse;
import com.gombeth.urban.dto.ValidacionRemesaResponse;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/remesas")
public class RemesaController {

    private final ContabilidadReciboRepository reciboRepository;
    private final FicheroGeneradoRepository ficheroGeneradoRepository;
    private final RemesaLineaRepository remesaLineaRepository;
    private final VecinoRepository vecinoRepository;
    private final ComunidadRepository comunidadRepository;
    private final SepaCoreXmlService sepaCoreXmlService;

    public RemesaController(
            ContabilidadReciboRepository reciboRepository,
            FicheroGeneradoRepository ficheroGeneradoRepository,
            RemesaLineaRepository remesaLineaRepository,
            VecinoRepository vecinoRepository,
            ComunidadRepository comunidadRepository,
            SepaCoreXmlService sepaCoreXmlService
    ) {
        this.reciboRepository = reciboRepository;
        this.ficheroGeneradoRepository = ficheroGeneradoRepository;
        this.remesaLineaRepository = remesaLineaRepository;
        this.vecinoRepository = vecinoRepository;
        this.comunidadRepository = comunidadRepository;
        this.sepaCoreXmlService = sepaCoreXmlService;
    }

    @PostMapping("/generar")
    public Map<String, Object> generarRemesa(
            @RequestParam Long comunidadId,
            @RequestParam String fechaCobro,
            @RequestParam String fechaDesde,
            @RequestParam String fechaHasta
    ) {
        LocalDate fechaCobroDate = LocalDate.parse(fechaCobro);
        LocalDate fechaDesdeDate = LocalDate.parse(fechaDesde);
        LocalDate fechaHastaDate = LocalDate.parse(fechaHasta);

        List<ContabilidadRecibo> recibosPendientes =
                reciboRepository
                        .findByComunidadIdAndEstadoAndFechaEmisionBetweenOrderByFechaEmisionAscIdAsc(
                                comunidadId,
                                "PENDIENTE",
                                fechaDesdeDate,
                                fechaHastaDate
                        );

        if (recibosPendientes.isEmpty()) {
            return Map.of(
                    "comunidadId", comunidadId,
                    "lineasGeneradas", 0,
                    "mensaje", "No existen recibos pendientes para generar remesa en el periodo indicado"
            );
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalDomiciliado = BigDecimal.ZERO;
        BigDecimal totalNoDomiciliado = BigDecimal.ZERO;

        int lineasGeneradas = 0;
        int recibosOmitidos = 0;

        FicheroGenerado fichero = new FicheroGenerado();
        fichero.setComunidadId(comunidadId);
        fichero.setFechaCreacion(LocalDate.now());
        fichero.setFechaCobro(fechaCobroDate);
        fichero.setEstado("GENERADA");
        fichero.setTipoRemesa("ORDINARIA");
        fichero.setEsquemaSepa("CORE");
        fichero.setIdentificadorFichero(
                "REM-" + comunidadId + "-" + System.currentTimeMillis()
        );
        fichero.setNombreArchivo(
                "remesa_" + comunidadId + "_" + fechaCobroDate + ".xml"
        );
        fichero.setTotalImporte(BigDecimal.ZERO);
        fichero.setTotalDomiciliado(BigDecimal.ZERO);
        fichero.setTotalNoDomiciliado(BigDecimal.ZERO);
        fichero.setNumeroRecibos(0);
        fichero.setObservaciones(
                "Remesa CORE generada desde recibos pendientes el "
                        + LocalDateTime.now()
        );

        fichero = ficheroGeneradoRepository.save(fichero);

        for (ContabilidadRecibo recibo : recibosPendientes) {

            if (remesaLineaRepository.existsByReciboContableId(recibo.getId())) {
                recibosOmitidos++;
                continue;
            }

            Vecino vecino = vecinoRepository.findById(recibo.getVecinoId())
                    .orElse(null);

            boolean domiciliado =
                    vecino != null
                            && vecino.isDomiciliado()
                            && vecino.getIban() != null
                            && !vecino.getIban().isBlank();

            RemesaLinea linea = new RemesaLinea();

            linea.setRemesaId(fichero.getId());
            linea.setVecinoId(recibo.getVecinoId());
            linea.setReciboContableId(recibo.getId());
            linea.setImporte(recibo.getImporte());
            linea.setConcepto(limitarConcepto(recibo.getConcepto()));
            linea.setDomiciliado(domiciliado);
            linea.setIncluidoSepa(domiciliado);

            remesaLineaRepository.save(linea);

            total = total.add(recibo.getImporte());

            if (domiciliado) {
                totalDomiciliado = totalDomiciliado.add(recibo.getImporte());
            } else {
                totalNoDomiciliado = totalNoDomiciliado.add(recibo.getImporte());
            }

            lineasGeneradas++;
        }

        if (lineasGeneradas == 0) {

            ficheroGeneradoRepository.delete(fichero);

            return Map.of(
                    "comunidadId", comunidadId,
                    "fechaCobro", fechaCobroDate,
                    "lineasGeneradas", 0,
                    "recibosOmitidos", recibosOmitidos,
                    "mensaje", "No se creó remesa porque todos los recibos pendientes ya estaban incluidos en otra remesa"
            );
        }

        fichero.setTotalImporte(total);
        fichero.setTotalDomiciliado(totalDomiciliado);
        fichero.setTotalNoDomiciliado(totalNoDomiciliado);
        fichero.setNumeroRecibos(lineasGeneradas);

        ficheroGeneradoRepository.save(fichero);

        return Map.of(
                "remesaId", fichero.getId(),
                "comunidadId", comunidadId,
                "fechaCobro", fechaCobroDate,
                "lineasGeneradas", lineasGeneradas,
                "recibosOmitidos", recibosOmitidos,
                "totalImporte", total,
                "totalDomiciliado", totalDomiciliado,
                "totalNoDomiciliado", totalNoDomiciliado,
                "esquemaSepa", "CORE",
                "mensaje", "Remesa generada correctamente"
        );
    }

    private String limitarConcepto(String concepto) {
        if (concepto == null || concepto.isBlank()) {
            return "Recibo comunidad";
        }
        if (concepto.length() <= 140) {
            return concepto;
        }
        return concepto.substring(0, 140);
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<byte[]> generarXml(
            @PathVariable Long id
    ) {
        FicheroGenerado remesa = ficheroGeneradoRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Remesa no encontrada"));

        Comunidad comunidad = comunidadRepository
                .findById(remesa.getComunidadId())
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        List<RemesaLinea> lineas = remesaLineaRepository
                .findByRemesaIdOrderByIdAsc(id)
                .stream()
                .filter(linea -> Boolean.TRUE.equals(linea.getIncluidoSepa()))
                .toList();

        if (lineas.isEmpty()) {
            throw new RuntimeException("La remesa no tiene líneas SEPA incluidas");
        }

        List<Vecino> vecinos = lineas.stream()
                .map(RemesaLinea::getVecinoId)
                .distinct()
                .map(vecinoId -> vecinoRepository.findById(vecinoId)
                        .orElseThrow(() -> new RuntimeException("Vecino no encontrado: " + vecinoId)))
                .toList();

        String xml = sepaCoreXmlService.generarXmlCore(
                remesa,
                comunidad,
                lineas,
                vecinos
        );

        remesa.setContenido(xml);
        remesa.setNombreArchivo(
                "remesa_core_" + remesa.getId() + ".xml"
        );

        ficheroGeneradoRepository.save(remesa);

        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + remesa.getNombreArchivo() + "\""
                )
                .contentType(MediaType.APPLICATION_XML)
                .body(bytes);
    }

    @GetMapping
    public List<RemesaResumenResponse> listarRemesas(
            @RequestParam Long comunidadId
    ) {
        return ficheroGeneradoRepository
                .findByComunidadIdOrderByIdDesc(comunidadId)
                .stream()
                .map(r -> new RemesaResumenResponse(
                        r.getId(),
                        r.getComunidadId(),
                        r.getIdentificadorFichero(),
                        r.getFechaCreacion(),
                        r.getTotalImporte(),
                        r.getNumeroRecibos(),
                        r.getNombreArchivo(),
                        r.getEstado(),
                        r.getTipoRemesa(),
                        r.getFechaCobro(),
                        r.getEsquemaSepa(),
                        r.getTotalDomiciliado(),
                        r.getTotalNoDomiciliado(),
                        r.getObservaciones()
                ))
                .toList();
    }

    @GetMapping("/{id}/validar")
    public ValidacionRemesaResponse validarRemesa(
            @PathVariable Long id
    ) {
        FicheroGenerado remesa = ficheroGeneradoRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Remesa no encontrada"));

        Comunidad comunidad = comunidadRepository
                .findById(remesa.getComunidadId())
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        List<RemesaLinea> lineas = remesaLineaRepository
                .findByRemesaIdOrderByIdAsc(id)
                .stream()
                .filter(linea -> Boolean.TRUE.equals(linea.getIncluidoSepa()))
                .toList();

        List<String> mensajes = new ArrayList<>();

        if (lineas.isEmpty()) {
            mensajes.add("La remesa no tiene líneas SEPA incluidas.");
        }

        if (comunidad.getIban() == null || comunidad.getIban().isBlank()) {
            mensajes.add("La comunidad no tiene IBAN informado.");
        }

        if (comunidad.getIdentificadorAcreedor() == null ||
                comunidad.getIdentificadorAcreedor().isBlank()) {
            mensajes.add("La comunidad no tiene identificador de acreedor SEPA informado.");
        }

        for (RemesaLinea linea : lineas) {
            Vecino vecino = vecinoRepository.findById(linea.getVecinoId())
                    .orElse(null);

            if (vecino == null) {
                mensajes.add("No existe el vecino de la línea " + linea.getId());
                continue;
            }

            if (!vecino.isDomiciliado()) {
                mensajes.add("El vecino " + vecino.getId() + " no está domiciliado.");
            }

            if (vecino.getIban() == null || vecino.getIban().isBlank()) {
                mensajes.add("El vecino " + vecino.getId() + " no tiene IBAN.");
            }

            if (vecino.getReferenciaMandato() == null ||
                    vecino.getReferenciaMandato().isBlank()) {
                mensajes.add("El vecino " + vecino.getId() + " no tiene referencia de mandato.");
            }

            if (linea.getImporte() == null ||
                    linea.getImporte().signum() <= 0) {
                mensajes.add("La línea " + linea.getId() + " tiene importe inválido.");
            }
        }

        return new ValidacionRemesaResponse(
                id,
                mensajes.isEmpty(),
                mensajes.size(),
                mensajes
        );
    }
}