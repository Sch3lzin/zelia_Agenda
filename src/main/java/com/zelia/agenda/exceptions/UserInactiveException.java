package com.zelia.agenda.exceptions;

public class UserInactiveException extends RuntimeException {

    private final Long userId;

    public UserInactiveException(Long id) {
        super("Usuário inativo: " + id);
        this.userId = id;
    }

    public Long getUserId() {
        return userId;
    }
}