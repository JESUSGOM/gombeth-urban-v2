package com.gombeth.urban.controller;

import com.gombeth.urban.dto.GenerarRemesaSeleccionRequest;
import com.gombeth.urban.dto.RemesaResumenResponse;
import com.gombeth.urban.dto.ValidacionRemesaResponse;
import com.gombeth.urban.dto.SepaValidacionResultado;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.FicheroGenerado;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.FicheroGeneradoRepository;
import com.gombeth.urban.repository.RemesaLineaRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.RemesaService;
import com.gombeth.urban.service.SepaC19Service;
import com.gombeth.urban.service.SepaCoreXmlService;
import com.gombeth.urban.service.SepaRemesaValidationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final SepaC19Service sepaC19Service;
    private final RemesaService remesaService;
    private final SepaRemesaValidationService sepaRemesaValidationService;

    public RemesaController(
            ContabilidadReciboRepository reciboRepository,
            FicheroGeneradoRepository ficheroGeneradoRepository,
            RemesaLineaRepository remesaLineaRepository,
            VecinoRepository vecinoRepository,
            ComunidadRepository comunidadRepository,
            SepaCoreXmlService sepaCoreXmlService,
            SepaC19Service sepaC19Service,
            RemesaService remesaService,
            SepaRemesaValidationService sepaRemesaValidationService
    ) {
        this.reciboRepository = reciboRepository;
        this.ficheroGeneradoRepository = ficheroGeneradoRepository;
        this.remesaLineaRepository = remesaLineaRepository;
        this.vecinoRepository = vecinoRepository;
        this.comunidadRepository = comunidadRepository;
        this.sepaCoreXmlService = sepaCoreXmlService;
        this.sepaC19Service = sepaC19Service;
        this.remesaService = remesaService;
        this.sepaRemesaValidationService = sepaRemesaValidationService;
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

        FicheroGenerado fichero = remesaService.crearRemesaInicial(
                comunidadId,
                fechaCobroDate,
                "recibos pendientes"
        );

        for (ContabilidadRecibo recibo : recibosPendientes) {

            if (remesaService.reciboYaIncluidoEnRemesa(recibo.getId())) {
                recibosOmitidos++;
                continue;
            }

            RemesaLinea linea = remesaService.crearLineaDesdeRecibo(
                    fichero,
                    recibo
            );

            total = total.add(recibo.getImporte());

            if (Boolean.TRUE.equals(linea.getDomiciliado())) {
                totalDomiciliado = totalDomiciliado.add(recibo.getImporte());
            } else {
                totalNoDomiciliado = totalNoDomiciliado.add(recibo.getImporte());
            }

            lineasGeneradas++;
        }

        if (lineasGeneradas == 0) {

            remesaService.eliminarRemesa(fichero);

            return Map.of(
                    "comunidadId", comunidadId,
                    "fechaCobro", fechaCobroDate,
                    "lineasGeneradas", 0,
                    "recibosOmitidos", recibosOmitidos,
                    "mensaje", "No se creó remesa porque todos los recibos pendientes ya estaban incluidos en otra remesa"
            );
        }

        remesaService.actualizarTotalesRemesa(
                fichero,
                total,
                totalDomiciliado,
                totalNoDomiciliado,
                lineasGeneradas
        );

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

    @PostMapping("/generar-seleccion")
    public Map<String, Object> generarRemesaSeleccion(
            @RequestBody GenerarRemesaSeleccionRequest request
    ) {
        List<ContabilidadRecibo> recibosSeleccionados =
                reciboRepository.findByIdIn(
                        request.reciboIds()
                );

        if (recibosSeleccionados.isEmpty()) {
            return Map.of(
                    "comunidadId", request.comunidadId(),
                    "lineasGeneradas", 0,
                    "mensaje", "No se encontraron recibos seleccionados"
            );
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalDomiciliado = BigDecimal.ZERO;
        BigDecimal totalNoDomiciliado = BigDecimal.ZERO;

        int lineasGeneradas = 0;
        int recibosOmitidos = 0;

        FicheroGenerado fichero = remesaService.crearRemesaInicial(
                request.comunidadId(),
                request.fechaCobro(),
                "recibos seleccionados"
        );

        for (ContabilidadRecibo recibo : recibosSeleccionados) {

            if (!remesaService.perteneceAComunidad(recibo, request.comunidadId())) {
                recibosOmitidos++;
                continue;
            }

            if (!remesaService.esReciboPendiente(recibo)) {
                recibosOmitidos++;
                continue;
            }

            if (remesaService.reciboYaIncluidoEnRemesa(recibo.getId())) {
                recibosOmitidos++;
                continue;
            }

            RemesaLinea linea = remesaService.crearLineaDesdeRecibo(
                    fichero,
                    recibo
            );

            total = total.add(recibo.getImporte());

            if (Boolean.TRUE.equals(linea.getDomiciliado())) {
                totalDomiciliado = totalDomiciliado.add(recibo.getImporte());
            } else {
                totalNoDomiciliado = totalNoDomiciliado.add(recibo.getImporte());
            }

            lineasGeneradas++;
        }

        if (lineasGeneradas == 0) {

            remesaService.eliminarRemesa(fichero);

            return Map.of(
                    "comunidadId", request.comunidadId(),
                    "fechaCobro", request.fechaCobro(),
                    "lineasGeneradas", 0,
                    "recibosOmitidos", recibosOmitidos,
                    "mensaje", "No se creó remesa porque ningún recibo seleccionado era válido"
            );
        }

        remesaService.actualizarTotalesRemesa(
                fichero,
                total,
                totalDomiciliado,
                totalNoDomiciliado,
                lineasGeneradas
        );

        return Map.of(
                "remesaId", fichero.getId(),
                "comunidadId", request.comunidadId(),
                "fechaCobro", request.fechaCobro(),
                "lineasGeneradas", lineasGeneradas,
                "recibosOmitidos", recibosOmitidos,
                "totalImporte", total,
                "totalDomiciliado", totalDomiciliado,
                "totalNoDomiciliado", totalNoDomiciliado,
                "esquemaSepa", "CORE",
                "mensaje", "Remesa generada correctamente desde recibos seleccionados"
        );
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

        List<Vecino> vecinos = obtenerVecinosDeLineas(lineas);

        SepaValidacionResultado resultadoValidacion =
                sepaRemesaValidationService.validarRemesaSepa(
                        comunidad,
                        lineas,
                        vecinos
                );

        if (!resultadoValidacion.isValida()) {
            throw new RuntimeException(
                    "La remesa no es válida para SEPA: " +
                            String.join(" | ", resultadoValidacion.getErrores())
            );
        }

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

        List<Vecino> vecinos = obtenerVecinosDeLineas(lineas);

        SepaValidacionResultado resultadoValidacion =
                sepaRemesaValidationService.validarRemesaSepa(
                        comunidad,
                        lineas,
                        vecinos
                );

        List<String> mensajes = new ArrayList<>();
        mensajes.addAll(resultadoValidacion.getErrores());
        mensajes.addAll(resultadoValidacion.getAdvertencias());

        return new ValidacionRemesaResponse(
                id,
                resultadoValidacion.isValida(),
                resultadoValidacion.getErrores().size(),
                mensajes
        );
    }

    @GetMapping("/{id}/c19")
    public String descargarC19(@PathVariable Long id) {

        FicheroGenerado remesa =
                ficheroGeneradoRepository.findById(id)
                        .orElseThrow();

        Comunidad comunidad =
                comunidadRepository.findById(remesa.getComunidadId())
                        .orElseThrow();

        List<RemesaLinea> lineas =
                remesaLineaRepository.findByRemesaIdOrderByIdAsc(id);

        List<Vecino> vecinos = obtenerVecinosDeLineas(lineas);

        SepaValidacionResultado resultadoValidacion =
                sepaRemesaValidationService.validarRemesaSepa(
                        comunidad,
                        lineas,
                        vecinos
                );

        if (!resultadoValidacion.isValida()) {
            throw new RuntimeException(
                    "La remesa no es válida para C19: " +
                            String.join(" | ", resultadoValidacion.getErrores())
            );
        }

        return sepaC19Service.generarC19(
                remesa,
                comunidad,
                lineas,
                vecinos
        );
    }

    private List<Vecino> obtenerVecinosDeLineas(List<RemesaLinea> lineas) {
        return lineas.stream()
                .map(RemesaLinea::getVecinoId)
                .distinct()
                .map(vecinoId ->
                        vecinoRepository.findById(vecinoId)
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "Vecino no encontrado: " + vecinoId
                                        )
                                )
                )
                .toList();
    }
}