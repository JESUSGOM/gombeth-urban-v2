package com.gombeth.urban.controller;

import com.gombeth.urban.dto.RepartoPresupuestoResponse;
import com.gombeth.urban.entity.ComunidadConfiguracionReparto;
import com.gombeth.urban.entity.Presupuesto;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadConfiguracionRepartoRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuotaPresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRevisionRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ContabilidadAutomaticaService;
import com.gombeth.urban.service.GeneracionReciboConceptosService;
import com.gombeth.urban.service.RegeneracionRecibosService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresupuestoRepartoPartesIgualesTest {

    @Mock
    private PresupuestoRepository presupuestoRepository;

    @Mock
    private VecinoRepository vecinoRepository;

    @Mock
    private CuotaPresupuestoRepository cuotaPresupuestoRepository;

    @Mock
    private ComunidadConfiguracionRepartoRepository
            configuracionRepartoRepository;

    @Mock
    private PresupuestoRevisionRepository
            presupuestoRevisionRepository;

    @Mock
    private ContabilidadReciboRepository
            contabilidadReciboRepository;

    @Mock
    private GeneracionReciboConceptosService
            generacionReciboConceptosService;

    @Mock
    private ContabilidadAutomaticaService
            contabilidadAutomaticaService;

    @Mock
    private RegeneracionRecibosService
            regeneracionRecibosService;

    @Mock
    private AccesoComunidadService
            accesoComunidadService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PresupuestoController controller;

    @Test
    void reparteAPartesIgualesAunqueElCoeficienteSeaCero() {

        Long comunidadId = 33L;
        int anio = 2026;

        Presupuesto presupuesto =
                mock(Presupuesto.class);

        when(presupuesto.getImporte())
                .thenReturn(
                        new BigDecimal("906.00")
                );

        when(
                presupuestoRepository
                        .findByComunidadIdAndAnioOrderByCuentaCodigoAsc(
                                comunidadId,
                                anio
                        )
        ).thenReturn(
                List.of(presupuesto)
        );

        Vecino vecino =
                mock(Vecino.class);

        when(vecino.getId())
                .thenReturn(362L);

        when(vecino.getNombre())
                .thenReturn("Propietario de prueba");

        when(vecino.getVivienda())
                .thenReturn("261A");

        when(vecino.getCoeficiente())
                .thenReturn(BigDecimal.ZERO);

        when(
                vecinoRepository
                        .findByComunidadIdAndActivoTrueOrderByViviendaAscNombreAsc(
                                comunidadId
                        )
        ).thenReturn(
                List.of(vecino)
        );

        ComunidadConfiguracionReparto configuracion =
                new ComunidadConfiguracionReparto();

        configuracion.setComunidadId(
                comunidadId
        );

        configuracion.setMetodoReparto(
                "PARTES_IGUALES"
        );

        when(
                configuracionRepartoRepository
                        .findByComunidadId(
                                comunidadId
                        )
        ).thenReturn(
                Optional.of(configuracion)
        );

        List<RepartoPresupuestoResponse> reparto =
                controller.simularReparto(
                        comunidadId,
                        anio,
                        authentication
                );

        assertEquals(
                1,
                reparto.size()
        );

        assertEquals(
                new BigDecimal("906.00"),
                reparto.get(0).getImporteAnual()
        );

        assertEquals(
                new BigDecimal("75.50"),
                reparto.get(0).getImporteMensual()
        );
    }
}