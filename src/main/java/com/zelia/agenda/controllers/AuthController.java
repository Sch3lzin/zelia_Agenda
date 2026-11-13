package com.zelia.agenda.controllers;

import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.domain.user.dto.LoginRequestDTO;
import com.zelia.agenda.infra.ratelimit.RateLimiter;
import com.zelia.agenda.infra.security.jwt.TokenResponse;
import com.zelia.agenda.infra.security.jwt.TokenService;
import com.zelia.agenda.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final int LOGIN_MAX_PER_IP = 100;
    private static final int LOGIN_MAX_PER_EMAIL = 10;

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RateLimiter rateLimiter;

    private final String dummyHash;

    public AuthController(UserRepository userRepository,
                          UserService userService,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          RateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO request, HttpServletRequest httpRequest) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        rateLimiter.check("login:ip:" + httpRequest.getRemoteAddr(), LOGIN_MAX_PER_IP, LOGIN_WINDOW);
        rateLimiter.check("login:email:" + email, LOGIN_MAX_PER_EMAIL, LOGIN_WINDOW);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean senhaValida = passwordEncoder.matches(
                request.password(), user != null ? user.getPassword() : dummyHash);

        if (user == null || !senhaValida || !user.getActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou Senha invalidos");
        }

        String token = tokenService.gerarToken(user.getId(), user.getTokenVersion());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
        userService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
