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
    void rechazaVecinoSinMandatoFirmado() {

        Contexto contexto =
                crearContextoValido(
                        null
                );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneError(
                resultado,
                "mandato firmado"
        );
    }

    @Test
    void rechazaReferenciaDeDocumentoNoValida() {

        Contexto contexto =
                crearContextoValido(
                        "mandato-firmado.pdf"
                );

        SepaValidacionResultado resultado =
                validar(contexto);

        assertContieneError(
                resultado,
                "referencia"
        );
    }

    @Test
    void rechazaDocumentoDeMandatoInexistente() {

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

        assertContieneError(
                resultado,
                "no existe"
        );
    }

    @Test
    void rechazaDocumentoDeOtroPropietario() {

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

        assertContieneError(
                resultado,
                "no pertenece"
        );
    }

    @Test
    void rechazaDocumentoQueNoEsUnMandato() {

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

        assertContieneError(
                resultado,
                "no es un mandato"
        );
    }

    @Test
    void rechazaDocumentoDeMandatoVacio() {

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

        assertContieneError(
                resultado,
                "está vacío"
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

    private record Contexto(
            Comunidad comunidad,
            RemesaLinea linea,
            Vecino vecino
    ) {
    }
}
