package com.gombeth.urban.service;

import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    private static final int LONGITUD_MINIMA = 8;

    /*
     * Se conserva exactamente el límite actualmente
     * utilizado por la aplicación para BCrypt.
     */
    private static final int LONGITUD_MAXIMA = 72;

    public String validar(
            String password
    ) {

        if (password == null || password.isBlank()) {
            return "Debe indicar una contraseña.";
        }

        if (
                password.length()
                        < LONGITUD_MINIMA
        ) {
            return "La contraseña debe tener "
                    + "al menos 8 caracteres.";
        }

        if (
                password.length()
                        > LONGITUD_MAXIMA
        ) {
            return "La contraseña no puede "
                    + "superar los 72 caracteres.";
        }

        boolean contieneMayuscula =
                password.chars()
                        .anyMatch(
                                Character::isUpperCase
                        );

        boolean contieneMinuscula =
                password.chars()
                        .anyMatch(
                                Character::isLowerCase
                        );

        boolean contieneNumero =
                password.chars()
                        .anyMatch(
                                Character::isDigit
                        );

        if (
                !contieneMayuscula
                        || !contieneMinuscula
                        || !contieneNumero
        ) {
            return "La contraseña debe contener "
                    + "mayúsculas, minúsculas y números.";
        }

        return null;
    }
}