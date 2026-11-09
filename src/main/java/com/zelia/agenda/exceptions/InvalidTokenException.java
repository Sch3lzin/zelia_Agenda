package com.zelia.agenda.exceptions;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Token inválido ou expirado");
    }
}