package com.zelia.agenda.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegisteredEmailException.class)
    public ResponseEntity<ErrorResponse> registeredEmailException(RegisteredEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<Map<String, Object>> handleUserInactive(UserInactiveException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "erro", "UserInactiveException",
                        "mensagem", e.getMessage(),
                        "id", e.getUserId()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}