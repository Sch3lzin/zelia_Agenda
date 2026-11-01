package com.zelia.agenda.exceptions;

public class UserAlreadyInactiveException extends RuntimeException {
    public UserAlreadyInactiveException(Long id) {
        super("Usuário já está inativo: " + id);
    }
}