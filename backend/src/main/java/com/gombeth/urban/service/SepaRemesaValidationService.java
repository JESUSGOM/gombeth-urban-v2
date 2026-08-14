package com.gombeth.urban.service;

import com.gombeth.urban.dto.SepaValidacionResultado;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.entity.VecinoDocumento;
import com.gombeth.urban.repository.VecinoDocumentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SepaRemesaValidationService {

    private static final String PREFIJO_DOCUMENTO_BD =
            "BD:";

    private static final List<String> TIPOS_MANDATO_VALIDOS =
            List.of(
                    "MANDATO_SEPA_FIRMADO",
                    "MANDATO"
            );

    private final VecinoDocumentoRepository
            documentoRepository;

    public SepaRemesaValidationService(
            VecinoDocumentoRepository documentoRepository
    ) {
        this.documentoRepository =
                documentoRepository;
    }

    public SepaValidacionResultado validarRemesaSepa(
            Comunidad comunidad,
            List<RemesaLinea> lineas,
            List<Vecino> vecinos
    ) {
        SepaValidacionResultado resultado =
                new SepaValidacionResultado();

        validarComunidad(
                comunidad,
                resultado
        );

        if (lineas == null || lineas.isEmpty()) {
            resultado.addError(
                    "La remesa no tiene líneas."
            );

            return resultado;
        }

        List<RemesaLinea> lineasSepa =
                lineas.stream()
                        .filter(linea ->
                                linea != null
                                        && Boolean.TRUE.equals(
                                        linea.getIncluidoSepa()
                                )
                        )
                        .toList();

        if (lineasSepa.isEmpty()) {
            resultado.addError(
                    "La remesa no tiene líneas incluidas en SEPA."
            );

            return resultado;
        }

        if (vecinos == null || vecinos.isEmpty()) {
            resultado.addError(
                    "No se han proporcionado los propietarios de la remesa."
            );

            return resultado;
        }

        Map<Long, Vecino> vecinosPorId =
                vecinos.stream()
                        .filter(vecino ->
                                vecino != null
                                        && vecino.getId() != null
                        )
                        .collect(
                                Collectors.toMap(
                                        Vecino::getId,
                                        vecino -> vecino,
                                        (primero, segundo) ->
                                                primero
                                )
                        );

        Set<Long> vecinosValidados =
                new HashSet<>();

        for (RemesaLinea linea : lineasSepa) {
            validarDatosLinea(
                    linea,
                    resultado
            );

            Long vecinoId =
                    linea.getVecinoId();

            if (
                    vecinoId == null
                            || !vecinosValidados.add(vecinoId)
            ) {
                continue;
            }

            validarVecino(
                    linea,
                    vecinosPorId.get(vecinoId),
                    resultado
            );
        }

        return resultado;
    }

    private void validarComunidad(
            Comunidad comunidad,
            SepaValidacionResultado resultado
    ) {
        if (comunidad == null) {
            resultado.addError(
                    "No se ha informado la comunidad."
            );

            return;
        }

        if (
                comunidad.getNombre() == null
                        || comunidad.getNombre().isBlank()
        ) {
            resultado.addError(
                    "La comunidad no tiene nombre."
            );
        }

        if (
                comunidad.getIban() == null
                        || comunidad.getIban().isBlank()
        ) {
            resultado.addError(
                    "La comunidad no tiene IBAN informado."
            );

        } else if (
                !esIbanValido(
                        comunidad.getIban()
                )
        ) {
            resultado.addError(
                    "El IBAN de la comunidad no es válido."
            );
        }

        if (
                comunidad.getIdentificadorAcreedor() == null
                        || comunidad.getIdentificadorAcreedor()
                        .isBlank()
        ) {
            resultado.addError(
                    "La comunidad no tiene identificador "
                            + "de acreedor SEPA informado."
            );

        } else if (
                !pareceIdentificadorAcreedorValido(
                        comunidad.getIdentificadorAcreedor()
                )
        ) {
            resultado.addAdvertencia(
                    "El identificador de acreedor SEPA "
                            + "de la comunidad tiene formato dudoso."
            );
        }
    }

    private void validarDatosLinea(
            RemesaLinea linea,
            SepaValidacionResultado resultado
    ) {
        if (linea == null) {
            resultado.addError(
                    "Existe una línea nula en la remesa."
            );

            return;
        }

        if (
                linea.getImporte() == null
                        || linea.getImporte()
                        .compareTo(BigDecimal.ZERO) <= 0
        ) {
            resultado.addError(
                    "La línea "
                            + linea.getId()
                            + " tiene importe inválido."
            );
        }

        if (
                linea.getConcepto() == null
                        || linea.getConcepto().isBlank()
        ) {
            resultado.addError(
                    "La línea "
                            + linea.getId()
                            + " no tiene concepto."
            );
        }

        if (
                linea.getConcepto() != null
                        && linea.getConcepto().length() > 140
        ) {
            resultado.addAdvertencia(
                    "La línea "
                            + linea.getId()
                            + " tiene un concepto demasiado largo."
            );
        }
    }

    private void validarVecino(
            RemesaLinea linea,
            Vecino vecino,
            SepaValidacionResultado resultado
    ) {
        if (vecino == null) {
            resultado.addError(
                    "No existe el vecino asociado a la línea "
                            + linea.getId()
                            + "."
            );

            return;
        }

        if (!vecino.isDomiciliado()) {
            resultado.addError(
                    "El vecino "
                            + vecino.getId()
                            + " no está marcado como domiciliado."
            );
        }

        if (
                vecino.getNombre() == null
                        || vecino.getNombre().isBlank()
        ) {
            resultado.addError(
                    "El vecino "
                            + vecino.getId()
                            + " no tiene nombre."
            );
        }

        if (
                vecino.getNombre() != null
                        && vecino.getNombre().length() > 70
        ) {
            resultado.addAdvertencia(
                    "El nombre del vecino "
                            + vecino.getId()
                            + " es demasiado largo."
            );
        }

        if (
                vecino.getIban() == null
                        || vecino.getIban().isBlank()
        ) {
            resultado.addError(
                    "El vecino "
                            + vecino.getId()
                            + " no tiene IBAN."
            );

        } else if (
                !esIbanValido(
                        vecino.getIban()
                )
        ) {
            resultado.addError(
                    "El IBAN del vecino "
                            + vecino.getId()
                            + " no es válido."
            );
        }

        if (
                vecino.getReferenciaMandato() == null
                        || vecino.getReferenciaMandato()
                        .isBlank()
        ) {
            resultado.addError(
                    "El vecino "
                            + vecino.getId()
                            + " no tiene referencia de mandato."
            );
        }

        if (vecino.getFechaMandato() == null) {
            resultado.addError(
                    "El vecino "
                            + vecino.getId()
                            + " no tiene fecha de mandato."
            );

        } else if (
                vecino.getFechaMandato()
                        .isAfter(LocalDate.now())
        ) {
            resultado.addError(
                    "El vecino "
                            + vecino.getId()
                            + " tiene una fecha de mandato futura."
            );
        }

        validarMandatoFirmado(
                vecino,
                resultado
        );
    }

    private void validarMandatoFirmado(
            Vecino vecino,
            SepaValidacionResultado resultado
    ) {
        String referencia =
                vecino.getRutaMandatoFirmado();

        if (
                referencia == null
                        || referencia.isBlank()
        ) {
            resultado.addAdvertencia(
                    "El vecino "
                            + vecino.getId()
                            + " no tiene mandato firmado."
            );

            return;
        }

        Long documentoId =
                obtenerDocumentoId(
                        referencia
                );

        if (documentoId == null) {
            resultado.addAdvertencia(
                    "La referencia del mandato firmado "
                            + "del vecino "
                            + vecino.getId()
                            + " no es válida."
            );

            return;
        }

        Optional<VecinoDocumento> documentoOptional =
                documentoRepository.findById(
                        documentoId
                );

        if (documentoOptional.isEmpty()) {
            resultado.addAdvertencia(
                    "El documento de mandato firmado "
                            + documentoId
                            + " del vecino "
                            + vecino.getId()
                            + " no existe."
            );

            return;
        }

        VecinoDocumento documento =
                documentoOptional.get();

        if (
                documento.getVecinoId() == null
                        || !documento.getVecinoId()
                        .equals(vecino.getId())
        ) {
            resultado.addAdvertencia(
                    "El documento de mandato firmado "
                            + documentoId
                            + " no pertenece al vecino "
                            + vecino.getId()
                            + "."
            );

            return;
        }

        String tipoDocumento =
                documento.getTipoDocumento() == null
                        ? ""
                        : documento.getTipoDocumento()
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                !TIPOS_MANDATO_VALIDOS.contains(
                        tipoDocumento
                )
        ) {
            resultado.addAdvertencia(
                    "El documento "
                            + documentoId
                            + " del vecino "
                            + vecino.getId()
                            + " no es un mandato SEPA firmado."
            );

            return;
        }

        if (
                documento.getContenido() == null
                        || documento.getContenido().length == 0
        ) {
            resultado.addAdvertencia(
                    "El documento de mandato firmado "
                            + documentoId
                            + " del vecino "
                            + vecino.getId()
                            + " está vacío."
            );
        }
    }

    private Long obtenerDocumentoId(
            String referencia
    ) {
        String referenciaLimpia =
                referencia.trim();

        if (
                !referenciaLimpia
                        .toUpperCase(Locale.ROOT)
                        .startsWith(PREFIJO_DOCUMENTO_BD)
        ) {
            return null;
        }

        String identificador =
                referenciaLimpia.substring(
                                PREFIJO_DOCUMENTO_BD.length()
                        )
                        .trim();

        if (
                identificador.isBlank()
                        || !identificador.matches("[0-9]+")
        ) {
            return null;
        }

        try {
            long documentoId =
                    Long.parseLong(
                            identificador
                    );

            return documentoId > 0
                    ? documentoId
                    : null;

        } catch (NumberFormatException error) {
            return null;
        }
    }

    private boolean esIbanValido(
            String iban
    ) {
        if (iban == null) {
            return false;
        }

        String limpio =
                iban.replaceAll(
                                "[^A-Za-z0-9]",
                                ""
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                limpio.length() < 15
                        || limpio.length() > 34
                        || !limpio.matches(
                        "^[A-Z]{2}[0-9]{2}[A-Z0-9]+$"
                )
        ) {
            return false;
        }

        String reorganizado =
                limpio.substring(4)
                        + limpio.substring(0, 4);

        int resto = 0;

        for (
                int i = 0;
                i < reorganizado.length();
                i++
        ) {
            char caracter =
                    reorganizado.charAt(i);

            if (Character.isDigit(caracter)) {
                resto =
                        (
                                resto * 10
                                        + Character.digit(
                                        caracter,
                                        10
                                )
                        ) % 97;

            } else if (
                    caracter >= 'A'
                            && caracter <= 'Z'
            ) {
                int valor =
                        caracter - 'A' + 10;

                resto =
                        (
                                resto * 100
                                        + valor
                        ) % 97;

            } else {
                return false;
            }
        }

        return resto == 1;
    }

    private boolean pareceIdentificadorAcreedorValido(
            String identificador
    ) {
        if (identificador == null) {
            return false;
        }

        String limpio =
                identificador.replaceAll(
                                "[^A-Za-z0-9]",
                                ""
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        return limpio.length() >= 8
                && limpio.length() <= 35
                && limpio.matches(
                "^[A-Z]{2}[0-9A-Z]+$"
        );
    }
}