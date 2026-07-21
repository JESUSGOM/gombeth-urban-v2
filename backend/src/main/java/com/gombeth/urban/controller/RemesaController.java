package com.gombeth.urban.controller;

import com.gombeth.urban.dto.GenerarRemesaSeleccionRequest;
import com.gombeth.urban.dto.RemesaResumenResponse;
import com.gombeth.urban.dto.ValidacionRemesaResponse;
import com.gombeth.urban.dto.SepaValidacionResultado;
import com.gombeth.urban.dto.remesa.ProcesoRemesaRequest;
import com.gombeth.urban.dto.remesa.ProcesoRemesaResponse;
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
import com.gombeth.urban.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.gombeth.urban.service.storage.*;
import com.gombeth.urban.dto.remesa.RemesaDetalleResponse;
import com.gombeth.urban.dto.remesa.RemesaLineaDetalleResponse;
import com.gombeth.urban.dto.remesa.RemesaLineaConceptoDetalleResponse;
import com.gombeth.urban.entity.RemesaLineaConcepto;
import com.gombeth.urban.repository.RemesaLineaConceptoRepository;

import java.nio.file.Path;
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
    private final DocumentStorageService documentStorageService;
    private final ProcesoRemesaService procesoRemesaService;
    private final RemesaLineaConceptoRepository remesaLineaConceptoRepository;
    private final AccesoComunidadService accesoComunidadService;

    public RemesaController(
            ContabilidadReciboRepository reciboRepository,
            FicheroGeneradoRepository ficheroGeneradoRepository,
            RemesaLineaRepository remesaLineaRepository,
            VecinoRepository vecinoRepository,
            ComunidadRepository comunidadRepository,
            SepaCoreXmlService sepaCoreXmlService,
            SepaC19Service sepaC19Service,
            RemesaService remesaService,
            SepaRemesaValidationService sepaRemesaValidationService,
            DocumentStorageService documentStorageService,
            ProcesoRemesaService procesoRemesaService,
            RemesaLineaConceptoRepository remesaLineaConceptoRepository,
            AccesoComunidadService accesoComunidadService
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
        this.documentStorageService = documentStorageService;
        this.procesoRemesaService = procesoRemesaService;
        this.remesaLineaConceptoRepository = remesaLineaConceptoRepository;
        this.accesoComunidadService = accesoComunidadService;
    }

    @PostMapping("/generar")
    public Map<String, Object> generarRemesa(
            @RequestParam Long comunidadId,
            @RequestParam String fechaCobro,
            @RequestParam String fechaDesde,
            @RequestParam String fechaHasta,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(authentication, comunidadId);

        LocalDate fechaCobroDate = LocalDate.parse(fechaCobro);
        LocalDate fechaDesdeDate = LocalDate.parse(fechaDesde);
        LocalDate fechaHastaDate = LocalDate.parse(fechaHasta);

        List<ContabilidadRecibo> recibosPendientes =
                remesaService.obtenerRecibosParaRemesa(
                        comunidadId,
                        fechaDesdeDate
                );

        recibosPendientes =
                remesaService.eliminarRecibosYaIncluidos(
                        recibosPendientes
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
                fechaDesdeDate.getYear(),
                fechaDesdeDate.getMonthValue(),
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
            @RequestBody GenerarRemesaSeleccionRequest request,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                request.comunidadId()
        );

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
                request.fechaCobro().getYear(),
                request.fechaCobro().getMonthValue(),
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
            @PathVariable Long id,
            Authentication authentication
    ) {
        FicheroGenerado remesa = obtenerRemesaAutorizada(
                id,
                authentication
        );

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

        String nombreArchivo;

        Path rutaGuardada;
        try {
            rutaGuardada =
                    documentStorageService.guardarRemesaXml(
                            Path.of("W:/PROYECTOS/gombeth-urban-v2/ficheros"),
                            comunidad,
                            xml,
                            remesa.getFechaCreacion(),
                            remesa.getFechaCobro(),
                            remesa.getEsquemaSepa()
                    );

            nombreArchivo = rutaGuardada.getFileName().toString();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error guardando fichero XML en disco: " + e.getMessage(),
                    e
            );
        }

        remesa.setContenido(xml);
        remesa.setNombreArchivo(nombreArchivo);

        ficheroGeneradoRepository.save(remesa);

        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\""
                )
                .contentType(MediaType.APPLICATION_XML)
                .body(bytes);
    }

    @GetMapping
    public List<RemesaResumenResponse> listarRemesas(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

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
            @PathVariable Long id,
            Authentication authentication
    ) {
        FicheroGenerado remesa = obtenerRemesaAutorizada(
                id,
                authentication
        );

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
    public ResponseEntity<byte[]> descargarC19(
            @PathVariable Long id,
            Authentication authentication
    ) {

        FicheroGenerado remesa = obtenerRemesaAutorizada(
                id,
                authentication
        );

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

        String contenido = sepaC19Service.generarC19(
                remesa,
                comunidad,
                lineas,
                vecinos
        );

        String nombreArchivo =
                "REMESA_" + remesa.getId() + ".c19";

        Path rutaGuardada;
        try {
            rutaGuardada =
                    documentStorageService.guardarRemesaC19(
                            Path.of("W:/PROYECTOS/gombeth-urban-v2/ficheros"),
                            comunidad,
                            contenido,
                            remesa.getFechaCreacion(),
                            remesa.getFechaCobro(),
                            remesa.getEsquemaSepa()
                    );

            nombreArchivo = rutaGuardada.getFileName().toString();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error guardando fichero C19 en disco: " + e.getMessage(),
                    e
            );
        }

        remesa.setContenido(contenido);
        remesa.setNombreArchivo(nombreArchivo);
        ficheroGeneradoRepository.save(remesa);

        byte[] bytes =
                contenido.getBytes(StandardCharsets.ISO_8859_1);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\""
                )
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
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

    @PostMapping("/proceso")
    public ProcesoRemesaResponse procesoCompleto(
            @RequestBody ProcesoRemesaRequest request,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                request.getComunidadId()
        );

        return procesoRemesaService.ejecutar(request);

    }

    @GetMapping("/{id}/detalle")
    public RemesaDetalleResponse detalleRemesa(
            @PathVariable Long id,
            Authentication authentication
    ) {
        FicheroGenerado remesa = obtenerRemesaAutorizada(
                id,
                authentication
        );

        Comunidad comunidad = comunidadRepository
                .findById(remesa.getComunidadId())
                .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        List<RemesaLinea> lineas =
                remesaLineaRepository.findByRemesaIdOrderByIdAsc(id);

        List<RemesaLineaDetalleResponse> lineasDetalle =
                lineas.stream()
                        .map(linea -> {

                            Vecino vecino = vecinoRepository
                                    .findById(linea.getVecinoId())
                                    .orElse(null);

                            List<RemesaLineaConceptoDetalleResponse> conceptos =
                                    remesaLineaConceptoRepository
                                            .findByRemesaLineaIdOrderByOrdenAsc(linea.getId())
                                            .stream()
                                            .map(c -> new RemesaLineaConceptoDetalleResponse(
                                                    c.getId(),
                                                    c.getDescripcion(),
                                                    c.getImporte(),
                                                    c.getOrden(),
                                                    c.getAgrupadoEnUltimaLinea()
                                            ))
                                            .toList();

                            return new RemesaLineaDetalleResponse(
                                    linea.getId(),
                                    linea.getVecinoId(),
                                    vecino == null ? "Vecino no encontrado" : vecino.getNombre(),
                                    linea.getReciboContableId(),
                                    linea.getImporte(),
                                    linea.getDomiciliado(),
                                    linea.getIncluidoSepa(),
                                    linea.getPdfGenerado(),
                                    linea.getEmailEnviado(),
                                    conceptos
                            );
                        })
                        .toList();

        return new RemesaDetalleResponse(
                remesa.getId(),
                remesa.getComunidadId(),
                comunidad.getNombre(),
                remesa.getFechaCreacion(),
                remesa.getFechaCobro(),
                remesa.getEstado(),
                remesa.getEsquemaSepa(),
                remesa.getTotalImporte(),
                remesa.getTotalDomiciliado(),
                remesa.getTotalNoDomiciliado(),
                remesa.getNumeroRecibos(),
                remesa.getNombreArchivo(),
                lineasDetalle
        );
    }

    private FicheroGenerado obtenerRemesaAutorizada(
            Long remesaId,
            Authentication authentication
    ) {
        FicheroGenerado remesa = ficheroGeneradoRepository
                .findById(remesaId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Remesa no encontrada: " + remesaId
                        )
                );

        accesoComunidadService.validarAcceso(
                authentication,
                remesa.getComunidadId()
        );

        return remesa;
    }

}