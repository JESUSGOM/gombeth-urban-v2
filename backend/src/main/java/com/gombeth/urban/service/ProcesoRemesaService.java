package com.gombeth.urban.service;

import com.gombeth.urban.dto.remesa.ProcesoRemesaRequest;
import com.gombeth.urban.dto.remesa.ProcesoRemesaResponse;
import com.gombeth.urban.entity.*;
import com.gombeth.urban.repository.*;
import com.gombeth.urban.dto.SepaValidacionResultado;
import com.gombeth.urban.service.storage.DocumentStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Service
public class ProcesoRemesaService {

    private final ComunidadRepository comunidadRepository;
    private final CuotaPresupuestoRepository cuotaPresupuestoRepository;
    private final ContabilidadReciboRepository reciboRepository;
    private final GeneracionReciboConceptosService generacionReciboConceptosService;
    private final ContabilidadAutomaticaService contabilidadAutomaticaService;
    private final RemesaService remesaService;
    private final RemesaLineaRepository remesaLineaRepository;
    private final VecinoRepository vecinoRepository;
    private final SepaRemesaValidationService sepaRemesaValidationService;
    private final SepaC19Service sepaC19Service;
    private final SepaC19ValidationService sepaC19ValidationService;
    private final SepaCoreXmlService sepaCoreXmlService;
    private final SepaXmlValidationService sepaXmlValidationService;
    private final DocumentStorageService documentStorageService;
    private final FicheroGeneradoRepository ficheroGeneradoRepository;
    private final RemesaSeguimientoService remesaSeguimientoService;

    public ProcesoRemesaService(
            ComunidadRepository comunidadRepository,
            CuotaPresupuestoRepository cuotaPresupuestoRepository,
            ContabilidadReciboRepository reciboRepository,
            GeneracionReciboConceptosService generacionReciboConceptosService,
            ContabilidadAutomaticaService contabilidadAutomaticaService,
            RemesaService remesaService,
            RemesaLineaRepository remesaLineaRepository,
            VecinoRepository vecinoRepository,
            SepaRemesaValidationService sepaRemesaValidationService,
            SepaC19Service sepaC19Service,
            SepaC19ValidationService sepaC19ValidationService,
            SepaCoreXmlService sepaCoreXmlService,
            SepaXmlValidationService sepaXmlValidationService,
            DocumentStorageService documentStorageService,
            FicheroGeneradoRepository ficheroGeneradoRepository,
            RemesaSeguimientoService remesaSeguimientoService
    ) {
        this.comunidadRepository = comunidadRepository;
        this.cuotaPresupuestoRepository = cuotaPresupuestoRepository;
        this.reciboRepository = reciboRepository;
        this.generacionReciboConceptosService = generacionReciboConceptosService;
        this.contabilidadAutomaticaService = contabilidadAutomaticaService;
        this.remesaService = remesaService;
        this.remesaLineaRepository = remesaLineaRepository;
        this.vecinoRepository = vecinoRepository;
        this.sepaRemesaValidationService = sepaRemesaValidationService;
        this.sepaC19Service = sepaC19Service;
        this.sepaC19ValidationService = sepaC19ValidationService;
        this.sepaCoreXmlService = sepaCoreXmlService;
        this.sepaXmlValidationService = sepaXmlValidationService;
        this.documentStorageService = documentStorageService;
        this.ficheroGeneradoRepository = ficheroGeneradoRepository;
        this.remesaSeguimientoService = remesaSeguimientoService;
    }

    @Transactional
    public ProcesoRemesaResponse ejecutar(
            ProcesoRemesaRequest request
    ) {
        return ejecutar(
                request,
                null
        );
    }

    @Transactional
    public ProcesoRemesaResponse ejecutar(
            ProcesoRemesaRequest request,
            CuentaPresentador cuentaPresentador
    ) {
        ProcesoRemesaResponse response = new ProcesoRemesaResponse();

        try {
            validarRequest(request);

            Comunidad comunidad =
                    comunidadRepository.findById(request.getComunidadId())
                            .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

            LocalDate fechaEmision =
                    LocalDate.of(request.getAnio(), request.getMes(), 1);

            int recibosGenerados =
                    generarRecibosSiFaltan(
                            request.getComunidadId(),
                            request.getAnio(),
                            request.getMes(),
                            fechaEmision
                    );

            List<ContabilidadRecibo> recibosPendientes =
                    remesaService.obtenerRecibosParaRemesa(
                            request.getComunidadId(),
                            fechaEmision
                    );

            recibosPendientes =
                    remesaService.eliminarRecibosYaIncluidos(
                            recibosPendientes
                    );

            if (recibosPendientes.isEmpty()) {
                response.setCorrecto(false);
                response.setMensaje("No hay recibos pendientes nuevos para generar remesa.");
                response.setRecibos(0);
                return response;
            }

            FicheroGenerado remesa =
                    crearRemesa(
                            request,
                            recibosPendientes,
                            cuentaPresentador
                    );

            remesaSeguimientoService.registrarEvento(
                    remesa,
                    null,
                    RemesaEventoTipo.REMESA_GENERADA,
                    "REMESA",
                    null,
                    "Remesa generada desde proceso completo."
            );

            List<RemesaLinea> lineas =
                    remesaLineaRepository.findByRemesaIdOrderByIdAsc(
                            remesa.getId()
                    );

            List<RemesaLinea> lineasSepa =
                    lineas.stream()
                            .filter(l -> Boolean.TRUE.equals(l.getIncluidoSepa()))
                            .toList();

            List<Vecino> vecinos =
                    obtenerVecinosDeLineas(lineas);

            SepaValidacionResultado validacion =
                    sepaRemesaValidationService.validarRemesaSepa(
                            comunidad,
                            lineasSepa,
                            vecinos
                    );

            if (!validacion.isValida()) {
                response.setCorrecto(false);
                response.setRemesaId(remesa.getId());
                response.setRecibos(lineas.size());
                response.setMensaje(
                        "La remesa se creó, pero no es válida para SEPA: "
                                + String.join(" | ", validacion.getErrores())
                );
                return response;
            }

            String contenidoC19 =
                    sepaC19Service.generarC19(
                            remesa,
                            comunidad,
                            lineas,
                            vecinos
                    );

            SepaValidacionResultado validacionC19 =
                    sepaC19ValidationService.validar(
                            contenidoC19
                    );

            if (!validacionC19.isValida()) {
                response.setCorrecto(false);
                response.setRemesaId(remesa.getId());
                response.setRecibos(lineas.size());
                response.setMensaje(
                        "La remesa se creó, pero el C19 generado "
                                + "no supera la validación estructural: "
                                + String.join(
                                " | ",
                                validacionC19.getErrores()
                        )
                );
                return response;
            }

            String contenidoXml =
                    sepaCoreXmlService.generarXmlCore(
                            remesa,
                            comunidad,
                            lineasSepa,
                            vecinos
                    );

            SepaValidacionResultado validacionXml =
                    sepaXmlValidationService.validar(
                            contenidoXml
                    );

            if (!validacionXml.isValida()) {
                response.setCorrecto(false);
                response.setRemesaId(remesa.getId());
                response.setRecibos(lineas.size());
                response.setMensaje(
                        "La remesa se creó, pero el XML generado "
                                + "no supera la validación XSD oficial: "
                                + String.join(
                                " | ",
                                validacionXml.getErrores()
                        )
                );
                return response;
            }

            Path rutaC19 =
                    documentStorageService.guardarRemesaC19(
                            comunidad,
                            contenidoC19,
                            fechaEmision,
                            remesa.getFechaCobro(),
                            remesa.getEsquemaSepa()
                    );

            Path rutaXml =
                    documentStorageService.guardarRemesaXml(
                            comunidad,
                            contenidoXml,
                            fechaEmision,
                            remesa.getFechaCobro(),
                            remesa.getEsquemaSepa()
                    );

            remesa.setContenido(contenidoXml);
            remesa.setNombreArchivo(rutaXml.getFileName().toString());
            ficheroGeneradoRepository.save(remesa);

            remesaSeguimientoService.cambiarEstado(
                    remesa,
                    RemesaEstado.VALIDADA,
                    null,
                    RemesaEventoTipo.VALIDACION_CORRECTA,
                    "SEPA",
                    null,
                    "Validación SEPA, C19 estructural y XML XSD superada."
            );

            remesaSeguimientoService.registrarEvento(
                    remesa,
                    null,
                    RemesaEventoTipo.C19_GENERADO,
                    "C19",
                    rutaC19.getFileName().toString(),
                    "Fichero C19 generado y almacenado correctamente."
            );

            remesaSeguimientoService.cambiarEstado(
                    remesa,
                    RemesaEstado.FICHERO_GENERADO,
                    null,
                    RemesaEventoTipo.XML_GENERADO,
                    "XML",
                    rutaXml.getFileName().toString(),
                    "Ficheros bancarios C19 y XML generados y almacenados correctamente."
            );

            response.setCorrecto(true);
            response.setRemesaId(remesa.getId());
            response.setRecibos(lineas.size());
            response.setFicheroC19(rutaC19.getFileName().toString());
            response.setFicheroXml(rutaXml.getFileName().toString());
            int recibosDomiciliados =
                    lineasSepa.size();

            int recibosNoDomiciliados =
                    lineas.size() - recibosDomiciliados;

            String importeEnviadoBanco =
                    remesa.getTotalDomiciliado() == null
                            ? "0,00"
                            : remesa.getTotalDomiciliado()
                            .toPlainString()
                            .replace('.', ',');

            response.setMensaje(
                    "Proceso finalizado correctamente. "
                            + "Recibos nuevos generados: "
                            + recibosGenerados
                            + ". Recibos totales incluidos: "
                            + lineas.size()
                            + ". Domiciliados enviados al banco: "
                            + recibosDomiciliados
                            + ". No domiciliados excluidos del fichero bancario: "
                            + recibosNoDomiciliados
                            + ". Importe enviado al banco: "
                            + importeEnviadoBanco
                            + " €"
            );

            return response;

        } catch (Exception e) {
            response.setCorrecto(false);
            response.setMensaje("Error en proceso de remesa: " + e.getMessage());
            return response;
        }
    }

    private void validarRequest(
            ProcesoRemesaRequest request
    ) {
        if (request == null) {
            throw new RuntimeException("La petición no puede estar vacía.");
        }

        if (request.getComunidadId() == null) {
            throw new RuntimeException("Debe indicar comunidad.");
        }

        if (request.getAnio() == null) {
            throw new RuntimeException("Debe indicar año.");
        }

        if (request.getMes() == null || request.getMes() < 1 || request.getMes() > 12) {
            throw new RuntimeException("Debe indicar un mes válido.");
        }

        if (request.getFechaCobro() == null) {
            throw new RuntimeException("Debe indicar fecha de cobro.");
        }

        LocalDate fechaMinimaCobro =
                LocalDate.of(
                        request.getAnio(),
                        request.getMes(),
                        1
                );

        if (request.getFechaCobro().isBefore(fechaMinimaCobro)) {
            throw new RuntimeException(
                    "La fecha de cobro no puede ser anterior "
                            + "a la fecha de emisión del período: "
                            + fechaMinimaCobro
            );
        }
    }

    private int generarRecibosSiFaltan(
            Long comunidadId,
            Integer anio,
            Integer mes,
            LocalDate fechaEmision
    ) {
        List<CuotaPresupuesto> cuotas =
                cuotaPresupuestoRepository
                        .findByComunidadIdAndAnioAndEstadoOrderByIdAsc(
                                comunidadId,
                                anio,
                                "APROBADA"
                        );

        int generados = 0;

        for (CuotaPresupuesto cuota : cuotas) {

            if (!cuotaAplicaAlMes(cuota, mes)) {
                continue;
            }

            boolean yaExiste =
                    reciboRepository.existsByCuotaPresupuestoIdAndFechaEmision(
                            cuota.getId(),
                            fechaEmision
                    );

            if (yaExiste) {
                continue;
            }

            ContabilidadRecibo recibo =
                    new ContabilidadRecibo();

            recibo.setComunidadId(comunidadId);
            recibo.setVecinoId(cuota.getVecinoId());
            recibo.setCuotaPresupuestoId(cuota.getId());
            recibo.setFechaEmision(fechaEmision);
            recibo.setImporte(cuota.getImporteMensual());
            recibo.setEstado("PENDIENTE");
            recibo.setConcepto(
                    cuota.getDescripcion()
                            + " - "
                            + mes
                            + "/"
                            + anio
            );
            recibo.setTipoRemesa("ORDINARIA");
            recibo.setPagadoAcumulado(BigDecimal.ZERO);

            ContabilidadRecibo reciboGuardado =
                    reciboRepository.save(recibo);

            generacionReciboConceptosService.generarConceptosDesdeCuota(
                    reciboGuardado,
                    cuota,
                    mes
            );

            contabilidadAutomaticaService.registrarDevengoRecibo(
                    reciboGuardado
            );

            generados++;
        }

        return generados;
    }

    private boolean cuotaAplicaAlMes(
            CuotaPresupuesto cuota,
            Integer mes
    ) {
        int mesInicio =
                cuota.getMesInicio() == null
                        ? 1
                        : cuota.getMesInicio();

        int mesFin =
                cuota.getMesFin() == null
                        ? 12
                        : cuota.getMesFin();

        return mes >= mesInicio
                && mes <= mesFin;
    }

    private FicheroGenerado crearRemesa(
            ProcesoRemesaRequest request,
            List<ContabilidadRecibo> recibosPendientes,
            CuentaPresentador cuentaPresentador
    ) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalDomiciliado = BigDecimal.ZERO;
        BigDecimal totalNoDomiciliado = BigDecimal.ZERO;

        int lineasGeneradas = 0;

        FicheroGenerado fichero =
                remesaService.crearRemesaInicial(
                        request.getComunidadId(),
                        request.getAnio(),
                        request.getMes(),
                        request.getFechaCobro(),
                        "proceso completo",
                        cuentaPresentador
                );

        for (ContabilidadRecibo recibo : recibosPendientes) {

            if (remesaService.reciboYaIncluidoEnRemesa(recibo.getId())) {
                continue;
            }

            RemesaLinea linea =
                    remesaService.crearLineaDesdeRecibo(
                            fichero,
                            recibo
                    );

            BigDecimal importe =
                    recibo.getImporte() == null
                            ? BigDecimal.ZERO
                            : recibo.getImporte();

            total = total.add(importe);

            if (Boolean.TRUE.equals(linea.getDomiciliado())) {
                totalDomiciliado = totalDomiciliado.add(importe);
            } else {
                totalNoDomiciliado = totalNoDomiciliado.add(importe);
            }

            lineasGeneradas++;
        }

        if (lineasGeneradas == 0) {
            remesaService.eliminarRemesa(fichero);
            throw new RuntimeException(
                    "No se creó remesa porque todos los recibos ya estaban incluidos en otra remesa."
            );
        }

        remesaService.actualizarTotalesRemesa(
                fichero,
                total,
                totalDomiciliado,
                totalNoDomiciliado,
                lineasGeneradas
        );

        return fichero;
    }

    private List<Vecino> obtenerVecinosDeLineas(
            List<RemesaLinea> lineas
    ) {
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