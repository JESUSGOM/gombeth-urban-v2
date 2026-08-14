package com.gombeth.urban.controller;

import com.gombeth.urban.dto.RepartoPresupuestoResponse;
import com.gombeth.urban.entity.Presupuesto;
import com.gombeth.urban.entity.PresupuestoRepartoConfiguracion;
import com.gombeth.urban.entity.PresupuestoRepartoVecino;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadConfiguracionRepartoRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.CuotaPresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRepository;
import com.gombeth.urban.repository.PresupuestoRepartoConfiguracionRepository;
import com.gombeth.urban.repository.PresupuestoRepartoVecinoRepository;
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
class PresupuestoRepartoSeleccionPropietariosTest {

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
    private PresupuestoRevisionRepository presupuestoRevisionRepository;

    @Mock
    private PresupuestoRepartoConfiguracionRepository
            presupuestoRepartoConfiguracionRepository;

    @Mock
    private PresupuestoRepartoVecinoRepository
            presupuestoRepartoVecinoRepository;

    @Mock
    private ContabilidadReciboRepository contabilidadReciboRepository;

    @Mock
    private GeneracionReciboConceptosService generacionReciboConceptosService;

    @Mock
    private ContabilidadAutomaticaService contabilidadAutomaticaService;

    @Mock
    private RegeneracionRecibosService regeneracionRecibosService;

    @Mock
    private AccesoComunidadService accesoComunidadService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PresupuestoController controller;

    @Test
    void excluyeLocalYNormalizaCoeficientesDelGrupoSeleccionado() {
        Long comunidadId = 9L;
        int anio = 2026;

        Presupuesto partida =
                mock(Presupuesto.class);

        when(partida.getId())
                .thenReturn(500L);

        when(partida.getImporte())
                .thenReturn(
                        new BigDecimal("1200.00")
                );

        when(
                presupuestoRepository
                        .findByComunidadIdAndAnioOrderByCuentaCodigoAsc(
                                comunidadId,
                                anio
                        )
        ).thenReturn(
                List.of(partida)
        );

        Vecino vivienda1 = vecino(
                1L,
                "1A",
                "Vivienda uno",
                "10.0000"
        );

        Vecino vivienda2 = vecino(
                2L,
                "2A",
                "Vivienda dos",
                "20.0000"
        );

        Vecino local = vecino(
                3L,
                "LOCAL",
                "Local comercial",
                "30.0000"
        );

        when(
                vecinoRepository
                        .findByComunidadIdAndActivoTrueOrderByViviendaAscNombreAsc(
                                comunidadId
                        )
        ).thenReturn(
                List.of(
                        vivienda1,
                        vivienda2,
                        local
                )
        );

        PresupuestoRepartoConfiguracion configuracion =
                new PresupuestoRepartoConfiguracion();

        configuracion.setPresupuestoId(500L);
        configuracion.setMetodoReparto("COEFICIENTE");
        configuracion.setAplicaTodos(false);

        when(
                presupuestoRepartoConfiguracionRepository
                        .findByPresupuestoId(500L)
        ).thenReturn(
                Optional.of(configuracion)
        );

        when(
                presupuestoRepartoVecinoRepository
                        .findByPresupuestoIdOrderByVecinoIdAsc(500L)
        ).thenReturn(
                List.of(
                        relacion(500L, 1L),
                        relacion(500L, 2L)
                )
        );

        List<RepartoPresupuestoResponse> reparto =
                controller.simularReparto(
                        comunidadId,
                        anio,
                        authentication
                );

        assertEquals(3, reparto.size());

        assertEquals(
                new BigDecimal("400.00"),
                reparto.get(0).getImporteAnual()
        );

        assertEquals(
                new BigDecimal("800.00"),
                reparto.get(1).getImporteAnual()
        );

        assertEquals(
                new BigDecimal("0.00"),
                reparto.get(2).getImporteAnual()
        );
    }

    private Vecino vecino(
            Long id,
            String vivienda,
            String nombre,
            String coeficiente
    ) {
        Vecino vecino = mock(Vecino.class);
        when(vecino.getId()).thenReturn(id);
        when(vecino.getVivienda()).thenReturn(vivienda);
        when(vecino.getNombre()).thenReturn(nombre);
        when(vecino.getCoeficiente()).thenReturn(new BigDecimal(coeficiente));
        return vecino;
    }

    private PresupuestoRepartoVecino relacion(
            Long presupuestoId,
            Long vecinoId
    ) {
        PresupuestoRepartoVecino relacion =
                new PresupuestoRepartoVecino();
        relacion.setPresupuestoId(presupuestoId);
        relacion.setVecinoId(vecinoId);
        return relacion;
    }
}
