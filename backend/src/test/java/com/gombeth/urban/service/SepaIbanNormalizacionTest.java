package com.gombeth.urban.service;

import com.gombeth.urban.dto.SepaValidacionResultado;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.repository.VecinoDocumentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class SepaIbanNormalizacionTest {

    @Mock
    private VecinoDocumentoRepository documentoRepository;

    @InjectMocks
    private SepaRemesaValidationService service;

    @Test
    void aceptaIbanValidoConSeparadoresUnicode() {

        Comunidad comunidad = new Comunidad();

        comunidad.setNombre(
                "Comunidad de prueba"
        );

        comunidad.setIban(
                "ES82\u200B0000\u00A00000"
                        + "0000\u200B0000\u00A00000"
        );

        comunidad.setIdentificadorAcreedor(
                "ES12ZZZ000000000"
        );

        SepaValidacionResultado resultado =
                service.validarRemesaSepa(
                        comunidad,
                        List.of(),
                        List.of()
                );

        boolean contieneErrorIban =
                resultado.getErrores()
                        .stream()
                        .anyMatch(mensaje ->
                                mensaje.contains(
                                        "IBAN de la comunidad"
                                )
                        );

        assertFalse(
                contieneErrorIban,
                "Un IBAN válido con separadores Unicode "
                        + "no debe ser rechazado."
        );
    }
}