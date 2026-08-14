package com.gombeth.urban.service;

import com.gombeth.urban.dto.SepaValidacionResultado;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.entity.VecinoDocumento;
import com.gombeth.urban.repository.VecinoDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SepaRemesaValidationServiceTest {

    @Mock
    private VecinoDocumentoRepository
            documentoRepository;

    private SepaRemesaValidationService service;

    @BeforeEach
    void configurar() {

        service =
                new SepaRemesaValidationService(
                        documentoRepository
                );
    }

    @Test
    void permiteMandatoSepaFirmadoValido() {

        Contexto contexto =
                crearContextoValido(
                        "BD:15"
                );

        VecinoDocumento documento =
                crearDocumento(
                        15L,
                        4L,
                        "MANDATO_SEPA_FIRMADO",
                        new byte[]{1, 2, 3}
                );

        when(
                documentoRepository.findById(
                        15L
                )
        ).thenReturn(
                Optional.of(documento)
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertTrue(
                resultado.isValida(),
                () -> String.join(
                        " | ",
                        resultado.getErrores()
                )
        );
    }

    @Test
    void permiteTipoMandatoHistoricoValido() {

        Contexto contexto =
                crearContextoValido(
                        "BD:16"
                );

        VecinoDocumento documento =
                crearDocumento(
                        16L,
                        4L,
                        "MANDATO",
                        new byte[]{4, 5, 6}
                );

        when(
                documentoRepository.findById(
                        16L
                )
        ).thenReturn(
                Optional.of(documento)
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertTrue(
                resultado.isValida(),
                () -> String.join(
                        " | ",
                        resultado.getErrores()
                )
        );
    }

    @Test
    void rechazaVecinoSinFechaDeMandato() {

        Contexto contexto =
                crearContextoValido(
                        "BD:15"
                );

        when(
                contexto.vecino().getFechaMandato()
        ).thenReturn(
                null
        );

        VecinoDocumento documento =
                crearDocumento(
                        15L,
                        4L,
                        "MANDATO_SEPA_FIRMADO",
                        new byte[]{1}
                );

        when(
                documentoRepository.findById(
                        15L
                )
        ).thenReturn(
                Optional.of(documento)
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneError(
                resultado,
                "fecha de mandato"
        );
    }

    @Test
    void advierteVecinoSinMandatoFirmadoSinBloquear() {

        Contexto contexto =
                crearContextoValido(
                        null
                );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneAdvertencia(
                resultado,
                "mandato firmado"
        );

        assertTrue(
                resultado.isValida(),
                () -> "La advertencia de mandato no debe bloquear la remesa: "
                        + resultado.getErrores()
        );
    }

    @Test
    void advierteReferenciaDeDocumentoNoValidaSinBloquear() {

        Contexto contexto =
                crearContextoValido(
                        "mandato-firmado.pdf"
                );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneAdvertencia(
                resultado,
                "referencia"
        );

        assertTrue(
                resultado.isValida(),
                () -> "La advertencia de mandato no debe bloquear la remesa: "
                        + resultado.getErrores()
        );
    }

    @Test
    void advierteDocumentoDeMandatoInexistenteSinBloquear() {

        Contexto contexto =
                crearContextoValido(
                        "BD:99"
                );

        when(
                documentoRepository.findById(
                        99L
                )
        ).thenReturn(
                Optional.empty()
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneAdvertencia(
                resultado,
                "no existe"
        );

        assertTrue(
                resultado.isValida(),
                () -> "La advertencia de mandato no debe bloquear la remesa: "
                        + resultado.getErrores()
        );
    }

    @Test
    void advierteDocumentoDeOtroPropietarioSinBloquear() {

        Contexto contexto =
                crearContextoValido(
                        "BD:20"
                );

        VecinoDocumento documento =
                crearDocumento(
                        20L,
                        999L,
                        "MANDATO_SEPA_FIRMADO",
                        new byte[]{1}
                );

        when(
                documentoRepository.findById(
                        20L
                )
        ).thenReturn(
                Optional.of(documento)
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneAdvertencia(
                resultado,
                "no pertenece"
        );

        assertTrue(
                resultado.isValida(),
                () -> "La advertencia de mandato no debe bloquear la remesa: "
                        + resultado.getErrores()
        );
    }

    @Test
    void advierteDocumentoQueNoEsUnMandatoSinBloquear() {

        Contexto contexto =
                crearContextoValido(
                        "BD:21"
                );

        VecinoDocumento documento =
                crearDocumento(
                        21L,
                        4L,
                        "OTRO",
                        new byte[]{1}
                );

        when(
                documentoRepository.findById(
                        21L
                )
        ).thenReturn(
                Optional.of(documento)
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneAdvertencia(
                resultado,
                "no es un mandato"
        );

        assertTrue(
                resultado.isValida(),
                () -> "La advertencia de mandato no debe bloquear la remesa: "
                        + resultado.getErrores()
        );
    }

    @Test
    void advierteDocumentoDeMandatoVacioSinBloquear() {

        Contexto contexto =
                crearContextoValido(
                        "BD:22"
                );

        VecinoDocumento documento =
                crearDocumento(
                        22L,
                        4L,
                        "MANDATO_SEPA_FIRMADO",
                        new byte[0]
                );

        when(
                documentoRepository.findById(
                        22L
                )
        ).thenReturn(
                Optional.of(documento)
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneAdvertencia(
                resultado,
                "está vacío"
        );

        assertTrue(
                resultado.isValida(),
                () -> "La advertencia de mandato no debe bloquear la remesa: "
                        + resultado.getErrores()
        );
    }

    @Test
    void rechazaIbanDeComunidadConControlMod97Incorrecto() {

        Contexto contexto =
                crearContextoValido(
                        "BD:15"
                );

        when(
                contexto.comunidad().getIban()
        ).thenReturn(
                "ES9021000418450200051332"
        );

        VecinoDocumento documento =
                crearDocumento(
                        15L,
                        4L,
                        "MANDATO_SEPA_FIRMADO",
                        new byte[]{1}
                );

        when(
                documentoRepository.findById(
                        15L
                )
        ).thenReturn(
                Optional.of(documento)
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneError(
                resultado,
                "IBAN de la comunidad"
        );
    }

    @Test
    void rechazaIbanDeVecinoConControlMod97Incorrecto() {

        Contexto contexto =
                crearContextoValido(
                        "BD:15"
                );

        when(
                contexto.vecino().getIban()
        ).thenReturn(
                "ES7821000813610123456789"
        );

        VecinoDocumento documento =
                crearDocumento(
                        15L,
                        4L,
                        "MANDATO_SEPA_FIRMADO",
                        new byte[]{1}
                );

        when(
                documentoRepository.findById(
                        15L
                )
        ).thenReturn(
                Optional.of(documento)
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneError(
                resultado,
                "IBAN del vecino"
        );
    }

    @Test
    void rechazaFechaDeMandatoFutura() {

        Contexto contexto =
                crearContextoValido(
                        "BD:15"
                );

        when(
                contexto.vecino().getFechaMandato()
        ).thenReturn(
                LocalDate.now().plusDays(1)
        );

        VecinoDocumento documento =
                crearDocumento(
                        15L,
                        4L,
                        "MANDATO_SEPA_FIRMADO",
                        new byte[]{1}
                );

        when(
                documentoRepository.findById(
                        15L
                )
        ).thenReturn(
                Optional.of(documento)
        );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneError(
                resultado,
                "fecha de mandato futura"
        );
    }

    @Test
    void noDuplicaErroresDelMismoVecinoEnVariasLineas() {

        Contexto contexto =
                crearContextoValido(
                        null
                );

        when(
                contexto.vecino().getFechaMandato()
        ).thenReturn(
                null
        );

        RemesaLinea segundaLinea =
                mock(RemesaLinea.class);

//        when(
//                segundaLinea.getId()
//        ).thenReturn(
//                200L
//        );

        when(
                segundaLinea.getVecinoId()
        ).thenReturn(
                4L
        );

        when(
                segundaLinea.getImporte()
        ).thenReturn(
                new BigDecimal("10.00")
        );

        when(
                segundaLinea.getConcepto()
        ).thenReturn(
                "Cuota extraordinaria"
        );

        when(
                segundaLinea.getIncluidoSepa()
        ).thenReturn(
                true
        );

        SepaValidacionResultado resultado =
                service.validarRemesaSepa(
                        contexto.comunidad(),
                        List.of(
                                contexto.linea(),
                                segundaLinea
                        ),
                        List.of(
                                contexto.vecino()
                        )
                );

        long erroresFecha =
                contarErroresQueContienen(
                        resultado,
                        "no tiene fecha de mandato"
                );

        long advertenciasDocumento =
                contarAdvertenciasQueContienen(
                        resultado,
                        "no tiene mandato firmado"
                );

        assertEquals(
                1L,
                erroresFecha,
                () -> "La fecha de mandato se informó repetida: "
                        + resultado.getErrores()
        );

        assertEquals(
                1L,
                advertenciasDocumento,
                () -> "La advertencia de mandato firmado se informó repetida: "
                        + resultado.getAdvertencias()
        );
    }

    private SepaValidacionResultado validar(
            Contexto contexto
    ) {

        return service.validarRemesaSepa(
                contexto.comunidad(),
                List.of(
                        contexto.linea()
                ),
                List.of(
                        contexto.vecino()
                )
        );
    }

    private Contexto crearContextoValido(
            String rutaMandatoFirmado
    ) {

        Comunidad comunidad =
                mock(Comunidad.class);

        when(
                comunidad.getNombre()
        ).thenReturn(
                "Comunidad de prueba"
        );

        when(
                comunidad.getIban()
        ).thenReturn(
                "ES9121000418450200051332"
        );

        when(
                comunidad.getIdentificadorAcreedor()
        ).thenReturn(
                "ES12ZZZ12345678"
        );

        RemesaLinea linea =
                mock(RemesaLinea.class);


        when(
                linea.getVecinoId()
        ).thenReturn(
                4L
        );

        when(
                linea.getImporte()
        ).thenReturn(
                new BigDecimal("25.50")
        );

        when(
                linea.getConcepto()
        ).thenReturn(
                "Cuota ordinaria"
        );

        when(
                linea.getIncluidoSepa()
        ).thenReturn(
                true
        );

        Vecino vecino =
                mock(Vecino.class);

        when(
                vecino.getId()
        ).thenReturn(
                4L
        );

        when(
                vecino.isDomiciliado()
        ).thenReturn(
                true
        );

        when(
                vecino.getNombre()
        ).thenReturn(
                "Propietario de prueba"
        );

        when(
                vecino.getIban()
        ).thenReturn(
                "ES7921000813610123456789"
        );

        when(
                vecino.getReferenciaMandato()
        ).thenReturn(
                "MANDATO-4-2020"
        );

        when(
                vecino.getFechaMandato()
        ).thenReturn(
                LocalDate.of(
                        2020,
                        5,
                        15
                )
        );

        when(
                vecino.getRutaMandatoFirmado()
        ).thenReturn(
                rutaMandatoFirmado
        );

        return new Contexto(
                comunidad,
                linea,
                vecino
        );
    }

    private VecinoDocumento crearDocumento(
            Long documentoId,
            Long vecinoId,
            String tipoDocumento,
            byte[] contenido
    ) {

        VecinoDocumento documento =
                new VecinoDocumento();

        documento.setId(
                documentoId
        );

        documento.setVecinoId(
                vecinoId
        );

        documento.setTipoDocumento(
                tipoDocumento
        );

        documento.setNombreArchivo(
                "mandato.pdf"
        );

        documento.setContentType(
                "application/pdf"
        );

        documento.setContenido(
                contenido
        );

        return documento;
    }

    private void assertContieneError(
            SepaValidacionResultado resultado,
            String fragmento
    ) {

        assertTrue(
                resultado.getErrores()
                        .stream()
                        .anyMatch(error ->
                                error.toLowerCase()
                                        .contains(
                                                fragmento.toLowerCase()
                                        )
                        ),
                () -> "No se encontró el texto '"
                        + fragmento
                        + "' en los errores: "
                        + resultado.getErrores()
        );
    }

    private void assertContieneAdvertencia(
            SepaValidacionResultado resultado,
            String fragmento
    ) {

        assertTrue(
                resultado.getAdvertencias()
                        .stream()
                        .anyMatch(advertencia ->
                                advertencia.toLowerCase()
                                        .contains(
                                                fragmento.toLowerCase()
                                        )
                        ),
                () -> "No se encontró el texto '"
                        + fragmento
                        + "' en las advertencias: "
                        + resultado.getAdvertencias()
        );
    }

    private long contarAdvertenciasQueContienen(
            SepaValidacionResultado resultado,
            String fragmento
    ) {

        return resultado.getAdvertencias()
                .stream()
                .filter(advertencia ->
                        advertencia.toLowerCase()
                                .contains(
                                        fragmento.toLowerCase()
                                )
                )
                .count();
    }

    private long contarErroresQueContienen(
            SepaValidacionResultado resultado,
            String fragmento
    ) {

        return resultado.getErrores()
                .stream()
                .filter(error ->
                        error.toLowerCase()
                                .contains(
                                        fragmento.toLowerCase()
                                )
                )
                .count();
    }

    private record Contexto(
            Comunidad comunidad,
            RemesaLinea linea,
            Vecino vecino
    ) {
    }
}