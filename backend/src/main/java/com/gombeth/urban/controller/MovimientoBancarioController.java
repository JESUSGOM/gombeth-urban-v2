package com.gombeth.urban.controller;

import com.gombeth.urban.dto.CandidatoConciliacionResponse;
import com.gombeth.urban.dto.ComunidadNombreResponse;
import com.gombeth.urban.dto.ConciliacionRequest;
import com.gombeth.urban.dto.MovimientoContextoResponse;
import com.gombeth.urban.dto.ReciboPendienteResponse;
import com.gombeth.urban.dto.ResumenTesoreriaResponse;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoBancarioController {

    private final MovimientoBancarioRepository repository;

    private final ContabilidadReciboRepository
            reciboRepository;

    private final VecinoRepository vecinoRepository;

    private final ComunidadRepository comunidadRepository;

    private final AccesoComunidadService
            accesoComunidadService;

    public MovimientoBancarioController(
            MovimientoBancarioRepository repository,
            ContabilidadReciboRepository reciboRepository,
            VecinoRepository vecinoRepository,
            ComunidadRepository comunidadRepository,
            AccesoComunidadService accesoComunidadService
    ) {
        this.repository = repository;
        this.reciboRepository = reciboRepository;
        this.vecinoRepository = vecinoRepository;
        this.comunidadRepository = comunidadRepository;
        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Lista los movimientos bancarios de una comunidad
     * accesible por el usuario autenticado.
     *
     * Cualquier usuarioId enviado por el navegador se ignora.
     */
    @GetMapping
    public List<MovimientoBancario> listar(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        validarAccesoComunidad(
                authentication,
                comunidadId
        );

        return repository
                .findByComunidadIdOrderByFechaOperacionAscIdAsc(
                        comunidadId
                );
    }

    /**
     * Obtiene los recibos cuyo importe coincide con el
     * movimiento bancario.
     */
    @GetMapping("/{id}/candidatos")
    public List<CandidatoConciliacionResponse> candidatos(
            @PathVariable Long id,
            Authentication authentication
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        authentication
                );

        List<ContabilidadRecibo> recibos =
                reciboRepository
                        .findByComunidadIdAndEstado(
                                movimiento.getComunidadId(),
                                "PENDIENTE"
                        );

        return recibos
                .stream()
                .filter(
                        recibo ->
                                recibo.getImporte()
                                        .compareTo(
                                                movimiento.getImporte()
                                        ) == 0
                )
                .map(
                        recibo ->
                                new CandidatoConciliacionResponse(
                                        recibo.getId(),
                                        recibo.getVecinoId(),
                                        recibo.getConcepto(),
                                        recibo.getImporte(),
                                        recibo.getEstado()
                                )
                )
                .toList();
    }

    /**
     * Obtiene los recibos pendientes de la comunidad
     * correspondiente al movimiento.
     */
    @GetMapping("/{id}/recibos-pendientes")
    public List<ReciboPendienteResponse> recibosPendientes(
            @PathVariable Long id,
            Authentication authentication
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        authentication
                );

        return reciboRepository
                .findByComunidadIdAndEstadoOrderByImporte(
                        movimiento.getComunidadId(),
                        "PENDIENTE"
                )
                .stream()
                .map(recibo -> {

                    Vecino vecino =
                            vecinoRepository
                                    .findById(
                                            recibo.getVecinoId()
                                    )
                                    .orElse(null);

                    String nombreVecino =
                            vecino != null
                                    ? vecino.getNombre()
                                    : "Vecino "
                                    + recibo.getVecinoId();

                    return new ReciboPendienteResponse(
                            recibo.getId(),
                            recibo.getVecinoId(),
                            nombreVecino,
                            recibo.getFechaEmision(),
                            recibo.getFechaEmision()
                                    .getMonthValue()
                                    + "/"
                                    + recibo.getFechaEmision()
                                    .getYear(),
                            recibo.getConcepto(),
                            recibo.getImporte(),
                            recibo.getEstado()
                    );
                })
                .toList();
    }

    /**
     * Concilia un movimiento exclusivamente con recibos
     * pertenecientes a la misma comunidad.
     */
    @PostMapping("/{id}/conciliar")
    public MovimientoBancario conciliar(
            @PathVariable Long id,
            @RequestBody ConciliacionRequest request,
            Authentication authentication
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        authentication
                );

        if (
                request == null
                        || request.reciboIds() == null
                        || request.reciboIds().isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar al menos un recibo."
            );
        }

        List<ContabilidadRecibo> recibos =
                reciboRepository.findByIdIn(
                        request.reciboIds()
                );

        if (recibos.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se han encontrado los recibos seleccionados."
            );
        }

        for (ContabilidadRecibo recibo : recibos) {

            if (
                    recibo.getComunidadId() == null
                            || !recibo
                            .getComunidadId()
                            .equals(
                                    movimiento.getComunidadId()
                            )
            ) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Hay recibos que no pertenecen "
                                + "a la comunidad del movimiento."
                );
            }
        }

        BigDecimal totalSeleccionado =
                recibos
                        .stream()
                        .map(ContabilidadRecibo::getImporte)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        if (
                totalSeleccionado.compareTo(
                        movimiento.getImporte()
                ) != 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El total seleccionado no coincide "
                            + "con el importe del movimiento bancario."
            );
        }

        for (ContabilidadRecibo recibo : recibos) {

            recibo.setEstado("COBRADO");

            recibo.setFechaCobroBanco(
                    movimiento.getFechaOperacion()
            );

            recibo.setMovimientoBancarioId(
                    movimiento.getId()
            );

            recibo.setPagadoAcumulado(
                    recibo.getImporte()
            );
        }

        reciboRepository.saveAll(recibos);

        movimiento.setConciliado(true);
        movimiento.setProcesado(true);

        return repository.save(movimiento);
    }

    /**
     * Devuelve los datos de contexto del movimiento después
     * de comprobar su comunidad.
     */
    @GetMapping("/{id}/contexto")
    public MovimientoContextoResponse contexto(
            @PathVariable Long id,
            Authentication authentication
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        authentication
                );

        Comunidad comunidad =
                comunidadRepository
                        .findById(
                                movimiento.getComunidadId()
                        )
                        .orElse(null);

        String nombreComunidad =
                comunidad != null
                        ? comunidad.getNombre()
                        : "Comunidad "
                        + movimiento.getComunidadId();

        return new MovimientoContextoResponse(
                movimiento.getId(),
                movimiento.getComunidadId(),
                nombreComunidad,
                movimiento.getFechaOperacion(),
                movimiento.getFechaValor(),
                movimiento.getImporte(),
                movimiento.getConcepto()
        );
    }

    /**
     * Devuelve el nombre de una comunidad accesible.
     */
    @GetMapping("/comunidad/{comunidadId}/nombre")
    public ComunidadNombreResponse nombreComunidad(
            @PathVariable Long comunidadId,
            Authentication authentication
    ) {
        validarAccesoComunidad(
                authentication,
                comunidadId
        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Comunidad no encontrada."
                                        )
                        );

        return new ComunidadNombreResponse(
                comunidadId,
                comunidad.getNombre()
        );
    }

    /**
     * Devuelve el resumen de tesorería después de comprobar
     * el acceso a la comunidad.
     */
    @GetMapping("/resumen")
    public ResumenTesoreriaResponse resumenTesoreria(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        validarAccesoComunidad(
                authentication,
                comunidadId
        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Comunidad no encontrada."
                                        )
                        );

        List<ContabilidadRecibo> recibosPendientes =
                reciboRepository
                        .findByComunidadIdAndEstado(
                                comunidadId,
                                "PENDIENTE"
                        );

        List<MovimientoBancario> movimientos =
                repository
                        .findByComunidadIdOrderByFechaOperacionAscIdAsc(
                                comunidadId
                        );

        BigDecimal importePendiente =
                recibosPendientes
                        .stream()
                        .map(ContabilidadRecibo::getImporte)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        List<MovimientoBancario> sinConciliar =
                movimientos
                        .stream()
                        .filter(
                                movimiento ->
                                        movimiento.getConciliado() == null
                                                || !movimiento
                                                .getConciliado()
                        )
                        .toList();

        BigDecimal importeSinConciliar =
                sinConciliar
                        .stream()
                        .map(
                                movimiento ->
                                        "2".equals(
                                                movimiento.getSigno()
                                        )
                                                ? movimiento.getImporte()
                                                : movimiento
                                                .getImporte()
                                                .negate()
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return new ResumenTesoreriaResponse(
                comunidadId,
                comunidad.getNombre(),
                recibosPendientes.size(),
                importePendiente,
                movimientos.size(),
                sinConciliar.size(),
                importeSinConciliar
        );
    }

    private MovimientoBancario obtenerMovimientoAutorizado(
            Long movimientoId,
            Authentication authentication
    ) {
        MovimientoBancario movimiento =
                repository
                        .findById(movimientoId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Movimiento no encontrado."
                                        )
                        );

        validarAccesoComunidad(
                authentication,
                movimiento.getComunidadId()
        );

        return movimiento;
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