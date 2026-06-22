package com.gombeth.urban.controller;

import com.gombeth.urban.dto.CandidatoConciliacionResponse;
import com.gombeth.urban.dto.ComunidadNombreResponse;
import com.gombeth.urban.dto.ConciliacionRequest;
import com.gombeth.urban.dto.MovimientoContextoResponse;
import com.gombeth.urban.dto.ReciboPendienteResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.gombeth.urban.dto.ResumenTesoreriaResponse;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoBancarioController {

    private final MovimientoBancarioRepository repository;
    private final ContabilidadReciboRepository reciboRepository;
    private final VecinoRepository vecinoRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioComunidadRepository usuarioComunidadRepository;


    public MovimientoBancarioController(
            MovimientoBancarioRepository repository,
            ContabilidadReciboRepository reciboRepository,
            VecinoRepository vecinoRepository,
            ComunidadRepository comunidadRepository,
            UsuarioRepository usuarioRepository,
            UsuarioComunidadRepository usuarioComunidadRepository
    ) {
        this.repository = repository;
        this.reciboRepository = reciboRepository;
        this.vecinoRepository = vecinoRepository;
        this.comunidadRepository = comunidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioComunidadRepository = usuarioComunidadRepository;
    }

    @GetMapping
    public List<MovimientoBancario> listar(
            @RequestParam Long comunidadId,
            @RequestParam Long usuarioId
    ) {
        validarAccesoComunidad(usuarioId, comunidadId);

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
        MovimientoBancario movimiento = obtenerMovimientoAutorizado(
                id,
                usuarioId
        );

        List<ContabilidadRecibo> recibos =
                reciboRepository.findByComunidadIdAndEstado(
                        movimiento.getComunidadId(),
                        "PENDIENTE"
                );

        return recibos.stream()
                .filter(r ->
                        r.getImporte()
                                .compareTo(movimiento.getImporte()) == 0
                )
                .map(r ->
                        new CandidatoConciliacionResponse(
                                r.getId(),
                                r.getVecinoId(),
                                r.getConcepto(),
                                r.getImporte(),
                                r.getEstado()
                        )
                )
                .toList();
    }

    @GetMapping("/{id}/recibos-pendientes")
    public List<ReciboPendienteResponse> recibosPendientes(
            @PathVariable Long id,
            @RequestParam Long usuarioId
    ) {
        MovimientoBancario movimiento = obtenerMovimientoAutorizado(
                id,
                usuarioId
        );

        return reciboRepository
                .findByComunidadIdAndEstadoOrderByImporte(
                        movimiento.getComunidadId(),
                        "PENDIENTE"
                )
                .stream()
                .map(r -> {
                    Vecino vecino =
                            vecinoRepository
                                    .findById(r.getVecinoId())
                                    .orElse(null);

                    String nombreVecino =
                            vecino != null
                                    ? vecino.getNombre()
                                    : "Vecino " + r.getVecinoId();

                    return new ReciboPendienteResponse(
                            r.getId(),
                            r.getVecinoId(),
                            nombreVecino,
                            r.getFechaEmision(),
                            r.getFechaEmision().getMonthValue()
                                    + "/"
                                    + r.getFechaEmision().getYear(),
                            r.getConcepto(),
                            r.getImporte(),
                            r.getEstado()
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
        MovimientoBancario movimiento = obtenerMovimientoAutorizado(
                id,
                usuarioId
        );

        List<ContabilidadRecibo> recibos =
                reciboRepository.findByIdIn(
                        request.reciboIds()
                );

        for (ContabilidadRecibo recibo : recibos) {
            if (!recibo.getComunidadId().equals(movimiento.getComunidadId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Hay recibos que no pertenecen a la comunidad del movimiento"
                );
            }
        }

        BigDecimal totalSeleccionado =
                recibos.stream()
                        .map(ContabilidadRecibo::getImporte)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSeleccionado.compareTo(movimiento.getImporte()) != 0) {
            throw new RuntimeException(
                    "El total seleccionado no coincide con el importe del movimiento bancario"
            );
        }

        for (ContabilidadRecibo recibo : recibos) {
            recibo.setEstado("COBRADO");
            recibo.setFechaCobroBanco(movimiento.getFechaOperacion());
            recibo.setMovimientoBancarioId(movimiento.getId());
            recibo.setPagadoAcumulado(recibo.getImporte());
        }

        reciboRepository.saveAll(recibos);

        movimiento.setConciliado(true);
        movimiento.setProcesado(true);

        return repository.save(movimiento);
    }

    @GetMapping("/{id}/contexto")
    public MovimientoContextoResponse contexto(
            @PathVariable Long id,
            @RequestParam Long usuarioId
    ) {
        MovimientoBancario movimiento = obtenerMovimientoAutorizado(
                id,
                usuarioId
        );

        Comunidad comunidad =
                comunidadRepository
                        .findById(movimiento.getComunidadId())
                        .orElse(null);

        String nombreComunidad =
                comunidad != null
                        ? comunidad.getNombre()
                        : "Comunidad " + movimiento.getComunidadId();

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
        validarAccesoComunidad(usuarioId, comunidadId);

        Comunidad comunidad =
                comunidadRepository.findById(comunidadId)
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
                comunidadRepository.findById(comunidadId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Comunidad no encontrada"
                                )
                        );

        boolean esUsuarioDirecto =
                comunidad.getUsuarioId() != null
                        && comunidad.getUsuarioId().equals(usuarioId);

        boolean esAdministrador =
                usuario.getAdministradorId() != null
                        && comunidad.getAdministradorId() != null
                        && comunidad.getAdministradorId()
                        .equals(usuario.getAdministradorId());

        boolean estaAsignado =
                usuarioComunidadRepository
                        .existsByUsuarioIdAndComunidadId(
                                usuarioId,
                                comunidadId
                        );

        if (!esUsuarioDirecto && !esAdministrador && !estaAsignado) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permiso para acceder a esta comunidad"
            );
        }
    }

    @GetMapping("/resumen")
    public ResumenTesoreriaResponse resumenTesoreria(
            @RequestParam Long comunidadId,
            @RequestParam Long usuarioId
    ) {
        validarAccesoComunidad(usuarioId, comunidadId);

        Comunidad comunidad =
                comunidadRepository.findById(comunidadId)
                        .orElseThrow();

        List<ContabilidadRecibo> recibosPendientes =
                reciboRepository.findByComunidadIdAndEstado(
                        comunidadId,
                        "PENDIENTE"
                );

        List<MovimientoBancario> movimientos =
                repository.findByComunidadIdOrderByFechaOperacionAscIdAsc(
                        comunidadId
                );

        BigDecimal importePendiente =
                recibosPendientes.stream()
                        .map(ContabilidadRecibo::getImporte)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MovimientoBancario> sinConciliar =
                movimientos.stream()
                        .filter(m -> m.getConciliado() == null || !m.getConciliado())
                        .toList();

        BigDecimal importeSinConciliar =
                sinConciliar.stream()
                        .map(m -> "2".equals(m.getSigno())
                                ? m.getImporte()
                                : m.getImporte().negate())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

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