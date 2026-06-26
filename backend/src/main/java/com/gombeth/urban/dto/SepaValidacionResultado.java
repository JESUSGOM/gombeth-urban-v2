package com.gombeth.urban.dto;

import java.util.ArrayList;
import java.util.List;

public class SepaValidacionResultado {

    private final List<String> errores = new ArrayList<>();
    private final List<String> advertencias = new ArrayList<>();

    public List<String> getErrores() {
        return errores;
    }

    public List<String> getAdvertencias() {
        return advertencias;
    }

    public boolean isValida() {
        return errores.isEmpty();
    }

    public void addError(String mensaje) {
        errores.add(mensaje);
    }

    public void addAdvertencia(String mensaje) {
        advertencias.add(mensaje);
    }
}