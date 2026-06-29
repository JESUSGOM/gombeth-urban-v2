package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.ContabilidadMovimientoRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuentaContableRepository;
import com.gombeth.urban.repository.MovimientoBancarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ContabilidadAutomaticaService {

    private final ContabilidadMovimientoRepository movimientoRepository;
    private final CuentaContableRepository cuentaRepository;
    private final ContabilidadReciboRepository reciboRepository;
    private final MovimientoBancarioRepository movimientoBancarioRepository;
    private final ContabilidadAsientoService asientoService;

    public ContabilidadAutomaticaService(
            ContabilidadMovimientoRepository movimientoRepository,
            CuentaContableRepository cuentaRepository,
            ContabilidadReciboRepository reciboRepository,
            MovimientoBancarioRepository movimientoBancarioRepository,
            ContabilidadAsientoService asientoService
    ) {
        this.movimientoRepository = movimientoRepository;
        this.cuentaRepository = cuentaRepository;
        this.reciboRepository = reciboRepository;
        this.movimientoBancarioRepository = movimientoBancarioRepository;
        this.asientoService = asientoService;
    }

    @Transactional
    public void registrarCobroRecibo(
            ContabilidadRecibo recibo,
            MovimientoBancario movimiento
    ) {
        if (recibo == null || movimiento == null) {
            return;
        }

        String numeroAsientoControl = "COBRO-RECIBO-" + recibo.getId();

        boolean yaExiste = movimientoRepository.existsByComunidadIdAndNumeroAsiento(
                recibo.getComunidadId(),
                numeroAsientoControl
        );

        if (yaExiste) {
            return;
        }

        CuentaContable cuentaBanco = buscarCuentaPorPrefijo(
                recibo.getComunidadId(),
                "572",
                "No existe cuenta bancaria 572 para la comunidad "
        );

        CuentaContable cuentaDeudores = buscarCuentaPorPrefijoConAlternativa(
                recibo.getComunidadId(),
                "447",
                "430",
                "No existe cuenta de deudores 447 ni 430 para la comunidad "
        );

        BigDecimal importe = recibo.getImporte();

        if (importe == null || importe.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        LocalDate fecha = movimiento.getFechaOperacion() != null
                ? movimiento.getFechaOperacion()
                : LocalDate.now();

        ContabilidadAsiento asiento = asientoService.crearAsientoAutomatico(
                recibo.getComunidadId(),
                fecha,
                "Cobro recibo " + recibo.getId(),
                "RECIBO_COBRADO",
                recibo.getId(),
                null
        );

        String concepto = "Cobro recibo " + recibo.getId()
                + " - asiento " + asiento.getNumeroAsiento();

        ContabilidadMovimiento debeBanco = new ContabilidadMovimiento();
        debeBanco.setComunidadId(recibo.getComunidadId());
        debeBanco.setFecha(fecha);
        debeBanco.setNumeroAsiento(numeroAsientoControl);
        debeBanco.setConcepto(concepto);
        debeBanco.setCuentaId(cuentaBanco.getId());
        debeBanco.setDebe(importe);
        debeBanco.setHaber(BigDecimal.ZERO);

        ContabilidadMovimiento haberDeudores = new ContabilidadMovimiento();
        haberDeudores.setComunidadId(recibo.getComunidadId());
        haberDeudores.setFecha(fecha);
        haberDeudores.setNumeroAsiento(numeroAsientoControl);
        haberDeudores.setConcepto(concepto);
        haberDeudores.setCuentaId(cuentaDeudores.getId());
        haberDeudores.setDebe(BigDecimal.ZERO);
        haberDeudores.setHaber(importe);

        movimientoRepository.save(debeBanco);
        movimientoRepository.save(haberDeudores);
    }

    @Transactional
    public int regularizarCobrosConciliadosComunidad(Long comunidadId) {
        List<ContabilidadRecibo> recibos =
                reciboRepository.findByComunidadIdAndEstadoAndMovimientoBancarioIdIsNotNull(
                        comunidadId,
                        "COBRADO"
                );

        int generados = 0;

        for (ContabilidadRecibo recibo : recibos) {
            MovimientoBancario movimiento = movimientoBancarioRepository
                    .findById(recibo.getMovimientoBancarioId())
                    .orElse(null);

            if (movimiento == null) {
                continue;
            }

            String numeroAsientoControl = "COBRO-RECIBO-" + recibo.getId();

            boolean yaExiste = movimientoRepository.existsByComunidadIdAndNumeroAsiento(
                    recibo.getComunidadId(),
                    numeroAsientoControl
            );

            if (yaExiste) {
                continue;
            }

            registrarCobroRecibo(recibo, movimiento);
            generados++;
        }

        return generados;
    }

    private CuentaContable buscarCuentaPorPrefijo(
            Long comunidadId,
            String prefijo,
            String mensajeError
    ) {
        return cuentaRepository
                .findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                        comunidadId,
                        prefijo
                )
                .orElseThrow(() -> new IllegalStateException(mensajeError + comunidadId));
    }

    private CuentaContable buscarCuentaPorPrefijoConAlternativa(
            Long comunidadId,
            String prefijoPrincipal,
            String prefijoAlternativo,
            String mensajeError
    ) {
        Optional<CuentaContable> cuentaPrincipal =
                cuentaRepository.findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                        comunidadId,
                        prefijoPrincipal
                );

        if (cuentaPrincipal.isPresent()) {
            return cuentaPrincipal.get();
        }

        return cuentaRepository
                .findFirstByComunidad_IdAndCodigoStartingWithOrderByCodigoAsc(
                        comunidadId,
                        prefijoAlternativo
                )
                .orElseThrow(() -> new IllegalStateException(mensajeError + comunidadId));
    }
}