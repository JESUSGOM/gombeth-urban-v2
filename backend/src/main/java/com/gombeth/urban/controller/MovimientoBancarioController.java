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
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import com.gombeth.urban.repository.UsuarioComunidadRepository;
import com.gombeth.urban.repository.UsuarioRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.ConciliacionBancariaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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

    private final UsuarioRepository usuarioRepository;

    private final UsuarioComunidadRepository
            usuarioComunidadRepository;

    private final ConciliacionBancariaService
            conciliacionBancariaService;

    public MovimientoBancarioController(
            MovimientoBancarioRepository repository,
            ContabilidadReciboRepository reciboRepository,
            VecinoRepository vecinoRepository,
            ComunidadRepository comunidadRepository,
            UsuarioRepository usuarioRepository,
            UsuarioComunidadRepository
                    usuarioComunidadRepository,
            ConciliacionBancariaService
                    conciliacionBancariaService
    ) {
        this.repository = repository;

        this.reciboRepository =
                reciboRepository;

        this.vecinoRepository =
                vecinoRepository;

        this.comunidadRepository =
                comunidadRepository;

        this.usuarioRepository =
                usuarioRepository;

        this.usuarioComunidadRepository =
                usuarioComunidadRepository;

        this.conciliacionBancariaService =
                conciliacionBancariaService;
    }

    @GetMapping
    public List<MovimientoBancario> listar(
            @RequestParam Long comunidadId,
            @RequestParam Long usuarioId
    ) {
        validarAccesoComunidad(
                usuarioId,
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
            @RequestParam Long usuarioId
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        usuarioId
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
            @RequestParam Long usuarioId
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        usuarioId
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
            @RequestParam Long usuarioId,
            @RequestBody ConciliacionRequest request
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        usuarioId
                );

        try {
            return conciliacionBancariaService
                    .conciliarMovimientoConRecibos(
                            movimiento.getId(),
                            request.reciboIds()
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
            @RequestParam Long usuarioId
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        usuarioId
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
            @RequestParam Long usuarioId
    ) {
        MovimientoBancario movimiento =
                obtenerMovimientoAutorizado(
                        id,
                        usuarioId
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
            @RequestParam Long usuarioId
    ) {
        validarAccesoComunidad(
                usuarioId,
                comunidadId
        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElse(null);

        String nombre =
                comunidad != null
                        ? comunidad.getNombre()
                        : "Comunidad " + comunidadId;

        return new ComunidadNombreResponse(
                comunidadId,
                nombre
        );
    }

    private MovimientoBancario obtenerMovimientoAutorizado(
            Long movimientoId,
            Long usuarioId
    ) {
        MovimientoBancario movimiento =
                repository.findById(movimientoId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Movimiento no encontrado"
                                )
                        );

        validarAccesoComunidad(
                usuarioId,
                movimiento.getComunidadId()
        );

        return movimiento;
    }

    private void validarAccesoComunidad(
            Long usuarioId,
            Long comunidadId
    ) {
        Usuario usuario =
                usuarioRepository.findById(usuarioId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Usuario no autorizado"
                                )
                        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Comunidad no encontrada"
                                )
                        );

        boolean esUsuarioDirecto =
                comunidad.getUsuarioId() != null
                        && comunidad.getUsuarioId()
                        .equals(usuarioId);

        boolean esAdministrador =
                usuario.getAdministradorId() != null
                        && comunidad.getAdministradorId()
                        != null
                        && comunidad.getAdministradorId()
                        .equals(
                                usuario.getAdministradorId()
                        );

        boolean estaAsignado =
                usuarioComunidadRepository
                        .existsByUsuarioIdAndComunidadId(
                                usuarioId,
                                comunidadId
                        );

        if (
                !esUsuarioDirecto
                        && !esAdministrador
                        && !estaAsignado
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permiso para acceder "
                            + "a esta comunidad"
            );
        }
    }

    @GetMapping("/resumen")
    public ResumenTesoreriaResponse resumenTesoreria(
            @RequestParam Long comunidadId,
            @RequestParam Long usuarioId
    ) {
        validarAccesoComunidad(
                usuarioId,
                comunidadId
        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow();

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
}