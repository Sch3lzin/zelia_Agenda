package com.zelia.agenda.controllers;

import com.zelia.agenda.domain.user.dto.LoginRequestDTO;
import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.infra.security.jwt.TokenResponse;
import com.zelia.agenda.infra.security.jwt.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.email())
                .orElse(null);

        if (user == null
                || !user.getActive()
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou Senha invalidos");
        }

        String token = tokenService.gerarToken(user.getEmail(), user.getUserType().name());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
