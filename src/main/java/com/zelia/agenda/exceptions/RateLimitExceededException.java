package com.zelia.agenda.exceptions;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Muitas tentativas. Tente novamente mais tarde.");
    }
}
