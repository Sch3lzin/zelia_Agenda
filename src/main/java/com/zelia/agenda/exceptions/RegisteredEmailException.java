package com.zelia.agenda.exceptions;

public class RegisteredEmailException extends RuntimeException {

    public RegisteredEmailException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}