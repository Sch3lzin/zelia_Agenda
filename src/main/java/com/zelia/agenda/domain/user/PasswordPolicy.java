package com.zelia.agenda.domain.user;

import com.zelia.agenda.exceptions.InvalidPasswordException;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    // O BCrypt considera apenas os primeiros 72 bytes da senha.
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new InvalidPasswordException("A senha deve ter pelo menos " + MIN_LENGTH + " caracteres.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new InvalidPasswordException("A senha excede o tamanho máximo permitido.");
        }
    }
}
