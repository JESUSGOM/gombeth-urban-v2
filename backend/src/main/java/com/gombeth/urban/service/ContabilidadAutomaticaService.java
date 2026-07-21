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
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.repository.ContabilidadGastoRepository;

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
    private final ContabilidadGastoRepository gastoRepository;

    public ContabilidadAutomaticaService(
            ContabilidadMovimientoRepository movimientoRepository,
            CuentaContableRepository cuentaRepository,
            ContabilidadReciboRepository reciboRepository,
            MovimientoBancarioRepository movimientoBancarioRepository,
            ContabilidadAsientoService asientoService,
            ContabilidadGastoRepository gastoRepository
    ) {
        this.movimientoRepository = movimientoRepository;
        this.cuentaRepository = cuentaRepository;
        this.reciboRepository = reciboRepository;
        this.movimientoBancarioRepository = movimientoBancarioRepository;
        this.asientoService = asientoService;
        this.gastoRepository = gastoRepository;
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
    public void registrarDevengoRecibo(ContabilidadRecibo recibo) {

        if (recibo == null || recibo.getId() == null) {
            return;
        }

        String numeroAsientoControl = "DEVENGO-RECIBO-" + recibo.getId();

        boolean yaExiste =
                movimientoRepository.existsByComunidadIdAndNumeroAsiento(
                        recibo.getComunidadId(),
                        numeroAsientoControl
                );

        if (yaExiste) {
            return;
        }

        CuentaContable cuentaDeudores =
                buscarCuentaPorPrefijoConAlternativa(
                        recibo.getComunidadId(),
                        "447",
                        "430",
                        "No existe cuenta de deudores 447 ni 430 para la comunidad "
                );

        CuentaContable cuentaIngresos =
                buscarCuentaPorPrefijoConAlternativa(
                        recibo.getComunidadId(),
                        "731",
                        "705",
                        "No existe cuenta de ingresos 731 ni 705 para la comunidad "
                );

        BigDecimal importe = recibo.getImporte();

        if (importe == null || importe.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        LocalDate fecha =
                recibo.getFechaEmision() != null
                        ? recibo.getFechaEmision()
                        : LocalDate.now();

        ContabilidadAsiento asiento =
                asientoService.crearAsientoAutomatico(
                        recibo.getComunidadId(),
                        fecha,
                        "Emisión recibo " + recibo.getId(),
                        "RECIBO_EMITIDO",
                        recibo.getId(),
                        null
                );

        String concepto =
                "Emisión recibo " + recibo.getId()
                        + " - asiento " + asiento.getNumeroAsiento();

        ContabilidadMovimiento debeDeudores = new ContabilidadMovimiento();
        debeDeudores.setComunidadId(recibo.getComunidadId());
        debeDeudores.setFecha(fecha);
        debeDeudores.setNumeroAsiento(numeroAsientoControl);
        debeDeudores.setConcepto(concepto);
        debeDeudores.setCuentaId(cuentaDeudores.getId());
        debeDeudores.setDebe(importe);
        debeDeudores.setHaber(BigDecimal.ZERO);

        ContabilidadMovimiento haberIngresos = new ContabilidadMovimiento();
        haberIngresos.setComunidadId(recibo.getComunidadId());
        haberIngresos.setFecha(fecha);
        haberIngresos.setNumeroAsiento(numeroAsientoControl);
        haberIngresos.setConcepto(concepto);
        haberIngresos.setCuentaId(cuentaIngresos.getId());
        haberIngresos.setDebe(BigDecimal.ZERO);
        haberIngresos.setHaber(importe);

        movimientoRepository.save(debeDeudores);
        movimientoRepository.save(haberIngresos);
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

    @Transactional
    public void contabilizarGasto(Long gastoId) {

        ContabilidadGasto gasto = gastoRepository.findById(gastoId)
                .orElseThrow(() ->
                        new IllegalStateException("No existe el gasto " + gastoId)
                );

        if (gasto.getNumeroAsiento() != null && !gasto.getNumeroAsiento().isBlank()) {
            return;
        }

        if (gasto.getComunidadId() == null) {
            throw new IllegalStateException("El gasto no tiene comunidad asociada.");
        }

        if (gasto.getCuentaGastoId() == null) {
            throw new IllegalStateException("El gasto no tiene cuenta de gasto asociada.");
        }

        if (gasto.getImporteTotal() == null || gasto.getImporteTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El gasto no tiene importe válido.");
        }

        CuentaContable cuentaProveedor = buscarCuentaPorPrefijo(
                gasto.getComunidadId(),
                "410",
                "No existe cuenta de proveedor 410 para la comunidad "
        );

        LocalDate fecha = gasto.getFechaFactura() != null
                ? gasto.getFechaFactura()
                : LocalDate.now();

        ContabilidadAsiento asiento = asientoService.crearAsientoAutomatico(
                gasto.getComunidadId(),
                fecha,
                "Factura proveedor " + gasto.getProveedor(),
                "GASTO_CONTABILIZADO",
                gasto.getId(),
                null
        );

        String numeroAsientoControl = "GASTO-" + gasto.getId();

        ContabilidadMovimiento debeGasto = new ContabilidadMovimiento();
        debeGasto.setComunidadId(gasto.getComunidadId());
        debeGasto.setFecha(fecha);
        debeGasto.setNumeroAsiento(numeroAsientoControl);
        debeGasto.setConcepto("Factura " + gasto.getNumeroFactura() + " - " + gasto.getProveedor());
        debeGasto.setCuentaId(gasto.getCuentaGastoId());
        debeGasto.setDebe(gasto.getImporteTotal());
        debeGasto.setHaber(BigDecimal.ZERO);

        ContabilidadMovimiento haberProveedor = new ContabilidadMovimiento();
        haberProveedor.setComunidadId(gasto.getComunidadId());
        haberProveedor.setFecha(fecha);
        haberProveedor.setNumeroAsiento(numeroAsientoControl);
        haberProveedor.setConcepto("Factura " + gasto.getNumeroFactura() + " - " + gasto.getProveedor());
        haberProveedor.setCuentaId(cuentaProveedor.getId());
        haberProveedor.setDebe(BigDecimal.ZERO);
        haberProveedor.setHaber(gasto.getImporteTotal());

        movimientoRepository.save(debeGasto);
        movimientoRepository.save(haberProveedor);

        gasto.setNumeroAsiento("GASTO-" + gasto.getId() + "-ASIENTO-" + asiento.getNumeroAsiento());

        gastoRepository.save(gasto);
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