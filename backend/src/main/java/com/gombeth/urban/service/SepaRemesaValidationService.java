package com.gombeth.urban.service;

import com.gombeth.urban.dto.SepaValidacionResultado;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.RemesaLinea;
import com.gombeth.urban.entity.Vecino;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SepaRemesaValidationService {

    public SepaValidacionResultado validarRemesaSepa(
            Comunidad comunidad,
            List<RemesaLinea> lineas,
            List<Vecino> vecinos
    ) {
        SepaValidacionResultado resultado = new SepaValidacionResultado();

        validarComunidad(comunidad, resultado);

        if (lineas == null || lineas.isEmpty()) {
            resultado.addError("La remesa no tiene líneas.");
            return resultado;
        }

        List<RemesaLinea> lineasSepa = lineas.stream()
                .filter(linea -> Boolean.TRUE.equals(linea.getIncluidoSepa()))
                .toList();

        if (lineasSepa.isEmpty()) {
            resultado.addError("La remesa no tiene líneas incluidas en SEPA.");
            return resultado;
        }

        Map<Long, Vecino> vecinosPorId = vecinos.stream()
                .collect(Collectors.toMap(Vecino::getId, v -> v, (a, b) -> a));

        for (RemesaLinea linea : lineasSepa) {
            validarLinea(linea, vecinosPorId.get(linea.getVecinoId()), resultado);
        }

        return resultado;
    }

    private void validarComunidad(
            Comunidad comunidad,
            SepaValidacionResultado resultado
    ) {
        if (comunidad == null) {
            resultado.addError("No se ha informado la comunidad.");
            return;
        }

        if (comunidad.getNombre() == null || comunidad.getNombre().isBlank()) {
            resultado.addError("La comunidad no tiene nombre.");
        }

        if (comunidad.getIban() == null || comunidad.getIban().isBlank()) {
            resultado.addError("La comunidad no tiene IBAN informado.");
        } else if (!pareceIbanValido(comunidad.getIban())) {
            resultado.addAdvertencia("El IBAN de la comunidad tiene formato dudoso.");
        }

        if (comunidad.getIdentificadorAcreedor() == null ||
                comunidad.getIdentificadorAcreedor().isBlank()) {
            resultado.addError("La comunidad no tiene identificador de acreedor SEPA informado.");
        } else if (!pareceIdentificadorAcreedorValido(comunidad.getIdentificadorAcreedor())) {
            resultado.addAdvertencia("El identificador de acreedor SEPA de la comunidad tiene formato dudoso.");
        }
    }

    private void validarLinea(
            RemesaLinea linea,
            Vecino vecino,
            SepaValidacionResultado resultado
    ) {
        if (linea == null) {
            resultado.addError("Existe una línea nula en la remesa.");
            return;
        }

        if (linea.getImporte() == null ||
                linea.getImporte().compareTo(BigDecimal.ZERO) <= 0) {
            resultado.addError("La línea " + linea.getId() + " tiene importe inválido.");
        }

        if (linea.getConcepto() == null || linea.getConcepto().isBlank()) {
            resultado.addError("La línea " + linea.getId() + " no tiene concepto.");
        }

        if (linea.getConcepto() != null && linea.getConcepto().length() > 140) {
            resultado.addAdvertencia("La línea " + linea.getId() + " tiene un concepto demasiado largo.");
        }

        if (vecino == null) {
            resultado.addError("No existe el vecino asociado a la línea " + linea.getId() + ".");
            return;
        }

        if (!vecino.isDomiciliado()) {
            resultado.addError("El vecino " + vecino.getId() + " no está marcado como domiciliado.");
        }

        if (vecino.getNombre() == null || vecino.getNombre().isBlank()) {
            resultado.addError("El vecino " + vecino.getId() + " no tiene nombre.");
        }

        if (vecino.getNombre() != null && vecino.getNombre().length() > 70) {
            resultado.addAdvertencia("El nombre del vecino " + vecino.getId() + " es demasiado largo.");
        }

        if (vecino.getIban() == null || vecino.getIban().isBlank()) {
            resultado.addError("El vecino " + vecino.getId() + " no tiene IBAN.");
        } else if (!pareceIbanValido(vecino.getIban())) {
            resultado.addAdvertencia("El IBAN del vecino " + vecino.getId() + " tiene formato dudoso.");
        }

        if (vecino.getReferenciaMandato() == null ||
                vecino.getReferenciaMandato().isBlank()) {
            resultado.addError("El vecino " + vecino.getId() + " no tiene referencia de mandato.");
        }
    }

    private boolean pareceIbanValido(String iban) {
        String limpio = iban.replace(" ", "").trim().toUpperCase();

        return limpio.length() >= 15
                && limpio.length() <= 34
                && limpio.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]+$");
    }

    private boolean pareceIdentificadorAcreedorValido(String identificador) {
        String limpio = identificador.replace(" ", "").trim().toUpperCase();

        return limpio.length() >= 8
                && limpio.length() <= 35
                && limpio.matches("^[A-Z]{2}[0-9A-Z]+$");
    }
}