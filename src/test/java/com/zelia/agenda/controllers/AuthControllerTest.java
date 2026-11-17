package com.zelia.agenda.controllers;

import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.domain.user.UserType;
import com.zelia.agenda.domain.user.dto.LoginRequestDTO;
import com.zelia.agenda.exceptions.RateLimitExceededException;
import com.zelia.agenda.infra.ratelimit.RateLimiter;
import com.zelia.agenda.infra.security.jwt.TokenResponse;
import com.zelia.agenda.infra.security.jwt.TokenService;
import com.zelia.agenda.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private UserRepository userRepository;
    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private RateLimiter rateLimiter;
    private AuthController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = mock(UserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(TokenService.class);
        rateLimiter = mock(RateLimiter.class);
        controller = new AuthController(userRepository, userService, passwordEncoder, tokenService, rateLimiter);

        request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
    }

    private static User user(boolean ativo) {
        User user = new User("Ana", "ana@exemplo.com", "hash", UserType.PROFESSOR, ativo);
        user.setId(1L);
        return user;
    }

    @Test
    void validLoginReturnsToken() {
        when(userRepository.findByEmailIgnoreCase("ana@exemplo.com")).thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches("senha12345", "hash")).thenReturn(true);
        when(tokenService.gerarToken(1L, 0)).thenReturn("jwt-gerado");

        ResponseEntity<?> resposta = controller.login(
                new LoginRequestDTO("  Ana@Exemplo.com ", "senha12345"), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEqualTo(new TokenResponse("jwt-gerado"));
    }

    @Test
    void wrongPasswordReturns401() {
        when(userRepository.findByEmailIgnoreCase("ana@exemplo.com")).thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        ResponseEntity<?> resposta = controller.login(new LoginRequestDTO("ana@exemplo.com", "errada"), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).isEqualTo("Email ou Senha invalidos");
    }

    @Test
    void unknownEmailReturns401() {
        when(userRepository.findByEmailIgnoreCase("ninguem@exemplo.com")).thenReturn(Optional.empty());

        ResponseEntity<?> resposta = controller.login(new LoginRequestDTO("ninguem@exemplo.com", "senha12345"), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(tokenService, never()).gerarToken(any(), anyInt());
    }

    @Test
    void inactiveAccountGetsNoTokenEvenWithCorrectPassword() {
        when(userRepository.findByEmailIgnoreCase("ana@exemplo.com")).thenReturn(Optional.of(user(false)));
        when(passwordEncoder.matches("senha12345", "hash")).thenReturn(true);

        ResponseEntity<?> resposta = controller.login(new LoginRequestDTO("ana@exemplo.com", "senha12345"), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(tokenService, never()).gerarToken(any(), anyInt());
    }

    @Test
    void rateLimitBlocksBeforeQueryingDatabase() {
        doThrow(new RateLimitExceededException())
                .when(rateLimiter).check(startsWith("login:ip:"), anyInt(), any(Duration.class));

        assertThatThrownBy(() -> controller.login(
                new LoginRequestDTO("ana@exemplo.com", "senha12345"), request))
                .isInstanceOf(RateLimitExceededException.class);
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void logoutCallsServiceAndReturns204() {
        ResponseEntity<Void> resposta = controller.logout(1L);

        verify(userService).logout(1L);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
