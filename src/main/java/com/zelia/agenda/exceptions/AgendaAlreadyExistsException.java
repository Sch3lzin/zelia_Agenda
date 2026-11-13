package com.zelia.agenda.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AgendaAlreadyExistsException extends RuntimeException {

    public AgendaAlreadyExistsException(String message) {
        super(message);
    }
}