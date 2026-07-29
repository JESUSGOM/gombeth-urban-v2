package com.gombeth.urban.controller;

import com.gombeth.urban.dto.Norma43MovimientoPreviewResponse;
import com.gombeth.urban.dto.Norma43PrevisualizacionResponse;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ConciliacionBancariaService;
import com.gombeth.urban.service.Norma43Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/norma43")
public class Norma43Controller {

    private final Norma43Service norma43Service;

    private final ConciliacionBancariaService
            conciliacionBancariaService;

    private final AccesoComunidadService
            accesoComunidadService;

    public Norma43Controller(
            Norma43Service norma43Service,
            ConciliacionBancariaService
                    conciliacionBancariaService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.norma43Service = norma43Service;

        this.conciliacionBancariaService =
                conciliacionBancariaService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Analiza un fichero Norma 43 sin guardar movimientos,
     * modificar recibos ni ejecutar conciliaciones.
     */
    @PostMapping(
            value = "/previsualizar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Norma43PrevisualizacionResponse previsualizar(
            @RequestParam("comunidadId") Long comunidadId,
            @RequestParam("fichero") MultipartFile fichero,
            Authentication authentication
    ) throws IOException {
        validarAccesoComunidad(
                authentication,
                comunidadId
        );

        validarFichero(fichero);

        String contenido = leerContenido(fichero);

        List<MovimientoBancario> movimientos =
                norma43Service.previsualizarContenido(
                        comunidadId,
                        contenido
                );

        return crearPrevisualizacion(
                comunidadId,
                obtenerNombreFichero(fichero),
                movimientos
        );
    }

    /**
     * Importa un fichero Norma 43 únicamente después de
     * comprobar que el usuario autenticado tiene acceso
     * a la comunidad.
     */
    @PostMapping(
            value = "/importar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<MovimientoBancario> importar(
            @RequestParam("comunidadId") Long comunidadId,
            @RequestParam("fichero") MultipartFile fichero,
            Authentication authentication
    ) throws IOException {
        validarAccesoComunidad(
                authentication,
                comunidadId
        );

        validarFichero(fichero);

        String contenido = leerContenido(fichero);

        return norma43Service.importarContenido(
                comunidadId,
                contenido
        );
    }

    /**
     * Ejecuta la conciliación automática solamente para
     * una comunidad accesible por el usuario autenticado.
     */
    @PostMapping("/conciliar")
    public Map<String, Object> conciliar(
            @RequestParam("comunidadId") Long comunidadId,
            Authentication authentication
    ) {
        validarAccesoComunidad(
                authentication,
                comunidadId
        );

        int conciliados =
                conciliacionBancariaService
                        .conciliarAutomaticamenteComunidad(
                                comunidadId
                        );

        return Map.of(
                "comunidadId",
                comunidadId,
                "conciliados",
                conciliados,
                "mensaje",
                "Conciliación automática finalizada"
        );
    }

    private Norma43PrevisualizacionResponse crearPrevisualizacion(
            Long comunidadId,
            String nombreFichero,
            List<MovimientoBancario> movimientos
    ) {
        List<Norma43MovimientoPreviewResponse> detalles =
                movimientos.stream()
                        .map(this::crearDetalleMovimiento)
                        .toList();

        BigDecimal totalDebe = sumarPorSigno(
                movimientos,
                "1"
        );

        BigDecimal totalHaber = sumarPorSigno(
                movimientos,
                "2"
        );

        LocalDate fechaInicial = movimientos.stream()
                .map(MovimientoBancario::getFechaOperacion)
                .filter(fecha -> fecha != null)
                .min(Comparator.naturalOrder())
                .orElse(null);

        LocalDate fechaFinal = movimientos.stream()
                .map(MovimientoBancario::getFechaOperacion)
                .filter(fecha -> fecha != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new Norma43PrevisualizacionResponse(
                comunidadId,
                nombreFichero,
                detalles.size(),
                totalDebe,
                totalHaber,
                fechaInicial,
                fechaFinal,
                detalles
        );
    }

    private Norma43MovimientoPreviewResponse crearDetalleMovimiento(
            MovimientoBancario movimiento
    ) {
        return new Norma43MovimientoPreviewResponse(
                movimiento.getFechaOperacion(),
                movimiento.getFechaValor(),
                movimiento.getSigno(),
                obtenerTipoMovimiento(movimiento.getSigno()),
                movimiento.getImporte(),
                movimiento.getConcepto(),
                movimiento.getConceptoCompleto(),
                movimiento.getReferenciaBancaria(),
                movimiento.getDocumentoExtra()
        );
    }

    private BigDecimal sumarPorSigno(
            List<MovimientoBancario> movimientos,
            String signo
    ) {
        return movimientos.stream()
                .filter(movimiento ->
                        signo.equals(movimiento.getSigno())
                )
                .map(MovimientoBancario::getImporte)
                .filter(importe -> importe != null)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private String obtenerTipoMovimiento(String signo) {
        return switch (signo) {
            case "1" -> "DEBE";
            case "2" -> "HABER";
            default -> "DESCONOCIDO";
        };
    }

    private String leerContenido(
            MultipartFile fichero
    ) throws IOException {
        return new String(
                fichero.getBytes(),
                StandardCharsets.ISO_8859_1
        );
    }

    private String obtenerNombreFichero(
            MultipartFile fichero
    ) {
        String nombre = fichero.getOriginalFilename();

        if (nombre == null || nombre.isBlank()) {
            return "extracto-norma43.txt";
        }

        return nombre.trim();
    }

    private void validarFichero(
            MultipartFile fichero
    ) {
        if (
                fichero == null
                        || fichero.isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un fichero Norma 43."
            );
        }
    }

    private void validarAccesoComunidad(
            Authentication authentication,
            Long comunidadId
    ) {
        if (comunidadId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad es obligatoria."
            );
        }

        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );
    }
}
