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
import com.gombeth.urban.service.ConciliacionBancariaService;
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

    private final ConciliacionBancariaService
            conciliacionBancariaService;

    public MovimientoBancarioController(
            MovimientoBancarioRepository repository,
            ContabilidadReciboRepository reciboRepository,
            VecinoRepository vecinoRepository,
            ComunidadRepository comunidadRepository,
            AccesoComunidadService accesoComunidadService,
            ConciliacionBancariaService
                    conciliacionBancariaService
    ) {
        this.repository = repository;
        this.reciboRepository = reciboRepository;
        this.vecinoRepository = vecinoRepository;
        this.comunidadRepository = comunidadRepository;
        this.accesoComunidadService =
                accesoComunidadService;
        this.conciliacionBancariaService =
                conciliacionBancariaService;
    }

    @GetMapping
    public List<MovimientoBancario> listar(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        return repository
                .findByComunidadIdOrderByFechaOperacionAscIdAsc(
                        comunidadId
                );
    }

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

        return recibos.stream()
                .filter(recibo ->
                        recibo.getImporte()
                                .compareTo(
                                        movimiento.getImporte()
                                ) == 0
                )
                .map(recibo ->
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

        Long usuarioId =
                accesoComunidadService.obtenerUsuarioId(
                        authentication
                );

        try {
            return conciliacionBancariaService
                    .conciliarMovimientoConRecibos(
                            movimiento.getId(),
                            request.reciboIds(),
                            usuarioId
                    );
        } catch (IllegalArgumentException excepcion) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    excepcion.getMessage(),
                    excepcion
            );
        } catch (IllegalStateException excepcion) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    excepcion.getMessage(),
                    excepcion
            );
        }
    }

    @PostMapping("/{id}/desconciliar")
    public MovimientoBancario desconciliar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        authentication
                );

        Long usuarioId =
                accesoComunidadService.obtenerUsuarioId(
                        authentication
                );

        try {
            return conciliacionBancariaService
                    .desconciliarMovimiento(
                            movimiento.getId(),
                            usuarioId,
                            null
                    );
        } catch (IllegalArgumentException excepcion) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    excepcion.getMessage(),
                    excepcion
            );
        } catch (IllegalStateException excepcion) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    excepcion.getMessage(),
                    excepcion
            );
        }
    }

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

    @GetMapping("/comunidad/{comunidadId}/nombre")
    public ComunidadNombreResponse nombreComunidad(
            @PathVariable Long comunidadId,
            Authentication authentication
    ) {
        Comunidad comunidad =
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                comunidadId
                        );

        return new ComunidadNombreResponse(
                comunidadId,
                comunidad.getNombre()
        );
    }

    @GetMapping("/resumen")
    public ResumenTesoreriaResponse resumenTesoreria(
            @RequestParam Long comunidadId,
            Authentication authentication
    ) {
        Comunidad comunidad =
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                comunidadId
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
                recibosPendientes.stream()
                        .map(
                                ContabilidadRecibo::getImporte
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        List<MovimientoBancario> sinConciliar =
                movimientos.stream()
                        .filter(movimiento ->
                                movimiento.getConciliado()
                                        == null
                                        || !movimiento
                                        .getConciliado()
                        )
                        .toList();

        BigDecimal importeSinConciliar =
                sinConciliar.stream()
                        .map(movimiento ->
                                "2".equals(
                                        movimiento.getSigno()
                                )
                                        ? movimiento.getImporte()
                                        : movimiento.getImporte()
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
                repository.findById(movimientoId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Movimiento no encontrado"
                                )
                        );

        accesoComunidadService.validarAcceso(
                authentication,
                movimiento.getComunidadId()
        );

        return movimiento;
    }
}