package com.zelia.agenda.exceptions;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundReturns404WithMessage() {
        ResponseEntity<ErrorResponse> resposta =
                handler.handleNotFound(new EntityNotFoundException("Turma 9 nao encontrada"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().status()).isEqualTo(404);
        assertThat(resposta.getBody().mensagem()).isEqualTo("Turma 9 nao encontrada");
        assertThat(resposta.getBody().timestamp()).isNotNull();
    }

    @Test
    void accessDeniedReturns403() {
        ResponseEntity<ErrorResponse> resposta =
                handler.handleAccessDenied(new AccessDeniedException("negado"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void occupiedSlotReturns409() {
        ResponseEntity<ErrorResponse> resposta =
                handler.handleAgendaAlreadyExists(new AgendaAlreadyExistsException("conflito"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().mensagem()).isEqualTo("conflito");
    }

    @Test
    void registeredEmailReturns409() {
        ResponseEntity<ErrorResponse> resposta =
                handler.handleRegisteredEmail(new RegisteredEmailException("a@exemplo.com"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void invalidPasswordReturns400() {
        ResponseEntity<ErrorResponse> resposta =
                handler.handleInvalidPassword(new InvalidPasswordException("curta"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidRecoveryTokenReturns400() {
        ResponseEntity<ErrorResponse> resposta = handler.handleInvalidToken(new InvalidTokenException());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().mensagem()).startsWith("Token inv");
    }

    @Test
    void rateLimitReturns429() {
        ResponseEntity<ErrorResponse> resposta = handler.handleRateLimit(new RateLimitExceededException());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void dataIntegrityReturns409WithoutLeakingDetails() {
        ResponseEntity<ErrorResponse> resposta =
                handler.handleDataIntegrity(new DataIntegrityViolationException("SQL interno ao banco"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().mensagem()).isEqualTo("Duplicate or invalid data.");
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() {
        ResponseEntity<ErrorResponse> resposta = handler.handleUnexpected(new RuntimeException("segredo interno"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().mensagem()).isEqualTo("Internal server error");
    }
}
