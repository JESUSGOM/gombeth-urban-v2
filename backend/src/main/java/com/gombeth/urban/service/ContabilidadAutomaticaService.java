package com.gombeth.urban.service;

import com.gombeth.urban.entity.ContabilidadAsiento;
import com.gombeth.urban.entity.ContabilidadGasto;
import com.gombeth.urban.entity.ContabilidadMovimiento;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.CuentaContable;
import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.repository.ContabilidadAsientoRepository;
import com.gombeth.urban.repository.ContabilidadGastoRepository;
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

    private static final String ESTADO_ANULADO =
            "ANULADO";

    private static final String ORIGEN_COBRO_RECIBO =
            "RECIBO_COBRADO";

    private static final String PREFIJO_ASIENTO =
            "ASIENTO-";

    private static final String PREFIJO_COBRO_HISTORICO =
            "COBRO-RECIBO-";

    private final ContabilidadMovimientoRepository movimientoRepository;
    private final CuentaContableRepository cuentaRepository;
    private final ContabilidadReciboRepository reciboRepository;
    private final MovimientoBancarioRepository movimientoBancarioRepository;
    private final ContabilidadAsientoService asientoService;
    private final ContabilidadGastoRepository gastoRepository;
    private final ContabilidadAsientoRepository asientoRepository;

    public ContabilidadAutomaticaService(
            ContabilidadMovimientoRepository movimientoRepository,
            CuentaContableRepository cuentaRepository,
            ContabilidadReciboRepository reciboRepository,
            MovimientoBancarioRepository movimientoBancarioRepository,
            ContabilidadAsientoService asientoService,
            ContabilidadGastoRepository gastoRepository,
            ContabilidadAsientoRepository asientoRepository
    ) {
        this.movimientoRepository = movimientoRepository;
        this.cuentaRepository = cuentaRepository;
        this.reciboRepository = reciboRepository;
        this.movimientoBancarioRepository = movimientoBancarioRepository;
        this.asientoService = asientoService;
        this.gastoRepository = gastoRepository;
        this.asientoRepository = asientoRepository;
    }

    @Transactional
    public void registrarCobroRecibo(
            ContabilidadRecibo recibo,
            MovimientoBancario movimiento
    ) {
        registrarCobroReciboSiNecesario(
                recibo,
                movimiento
        );
    }

    private boolean registrarCobroReciboSiNecesario(
            ContabilidadRecibo recibo,
            MovimientoBancario movimiento
    ) {
        if (
                recibo == null
                        || recibo.getId() == null
                        || recibo.getComunidadId() == null
                        || movimiento == null
        ) {
            return false;
        }

        BigDecimal importe = recibo.getImporte();

        if (
                importe == null
                        || importe.compareTo(BigDecimal.ZERO) <= 0
        ) {
            return false;
        }

        Optional<ContabilidadAsiento> ultimoAsientoCobro =
                asientoRepository
                        .findTopByComunidadIdAndOrigenAndOrigenIdOrderByIdDesc(
                                recibo.getComunidadId(),
                                ORIGEN_COBRO_RECIBO,
                                recibo.getId()
                        );

        if (
                ultimoAsientoCobro.isPresent()
                        && !ESTADO_ANULADO.equals(
                        ultimoAsientoCobro.get().getEstado()
                )
        ) {
            ContabilidadAsiento asientoExistente =
                    ultimoAsientoCobro.get();

            validarIdentificadorAsiento(
                    asientoExistente,
                    recibo.getId()
            );

            String referenciaAsientoExistente =
                    PREFIJO_ASIENTO
                            + asientoExistente.getId();

            boolean yaExisteReferenciaNueva =
                    movimientoRepository
                            .existsByComunidadIdAndNumeroAsiento(
                                    recibo.getComunidadId(),
                                    referenciaAsientoExistente
                            );

            if (yaExisteReferenciaNueva) {
                return false;
            }

            String referenciaHistorica =
                    PREFIJO_COBRO_HISTORICO
                            + recibo.getId();

            boolean yaExisteReferenciaHistorica =
                    movimientoRepository
                            .existsByComunidadIdAndNumeroAsiento(
                                    recibo.getComunidadId(),
                                    referenciaHistorica
                            );

            if (yaExisteReferenciaHistorica) {
                return false;
            }
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

        LocalDate fecha = movimiento.getFechaOperacion() != null
                ? movimiento.getFechaOperacion()
                : LocalDate.now();

        ContabilidadAsiento asiento = asientoService.crearAsientoAutomatico(
                recibo.getComunidadId(),
                fecha,
                "Cobro recibo " + recibo.getId(),
                ORIGEN_COBRO_RECIBO,
                recibo.getId(),
                null
        );

        validarIdentificadorAsiento(
                asiento,
                recibo.getId()
        );

        String numeroAsientoControl =
                PREFIJO_ASIENTO
                        + asiento.getId();

        boolean yaExiste =
                movimientoRepository
                        .existsByComunidadIdAndNumeroAsiento(
                                recibo.getComunidadId(),
                                numeroAsientoControl
                        );

        if (yaExiste) {
            return false;
        }

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

        return true;
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

            boolean generado =
                    registrarCobroReciboSiNecesario(
                            recibo,
                            movimiento
                    );

            if (generado) {
                generados++;
            }
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

    private void validarIdentificadorAsiento(
            ContabilidadAsiento asiento,
            Long reciboId
    ) {
        if (
                asiento == null
                        || asiento.getId() == null
        ) {
            throw new IllegalStateException(
                    "El asiento del cobro del recibo "
                            + reciboId
                            + " no tiene identificador."
            );
        }
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