package com.zelia.agenda.service;

import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.domain.user.dto.CreateUserDTO;
import com.zelia.agenda.domain.user.dto.ForgotPasswordDTO;
import com.zelia.agenda.domain.user.dto.ResetPasswordDTO;
import com.zelia.agenda.domain.user.dto.UpdateUserDTO;
import com.zelia.agenda.exceptions.RegisteredEmailException;
import com.zelia.agenda.exceptions.UserInactiveException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.zelia.agenda.exceptions.InvalidTokenException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario não encontrado: " + id));

        return user;
    }

    public User createUser(CreateUserDTO dto) {
        if (userRepository.findByEmailIgnoreCase(dto.email()).isPresent()) {
            throw new RegisteredEmailException(dto.email());
        }

        User user = new User(dto.name(),
                dto.email(),
                passwordEncoder.encode(dto.password()),
                dto.userType(),
                true);

        return userRepository.save(user);
    }

    public User updateUser(Long id, UpdateUserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario não encontrado: " + id));

        if (!user.getActive()) {
            throw new UserInactiveException(id);
        }

        if (!user.getEmail().equals(dto.email())
                && userRepository.findByEmailIgnoreCase(dto.email()).isPresent()) {
            throw new RegisteredEmailException(dto.email());
        }

        user.update(dto.name(), dto.email(), dto.userType());
        return userRepository.save(user);
    }

    public User reactiveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario não encontrado: " + id));

        user.setActive(true);
        return userRepository.save(user);
    }

    public User deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario não encontrado: " + id));

        if (!user.getActive()) {
            throw new UserInactiveException(id);
        }

        user.setActive(false);
        return userRepository.save(user);
    }

    public void sendToken(ForgotPasswordDTO dto) {
        userRepository.findByEmailIgnoreCase(dto.email())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .ifPresent(user -> {
                    String token = generateToken();

                    user.setToken(hashToken(token));
                    user.setExpirationToken(LocalDateTime.now().plus(TOKEN_VALIDITY));
                    userRepository.save(user);

                    emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token); // e-mail: o token original
                });
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isExpired(LocalDateTime expiration) {
        return expiration == null || expiration.isBefore(LocalDateTime.now());
    }

    public void resetPassword(ResetPasswordDTO dto) {
        User user = userRepository.findByToken(hashToken(dto.token()))
                .orElseThrow(InvalidTokenException::new);

        if (!Boolean.TRUE.equals(user.getActive()) || isExpired(user.getExpirationToken())) {
            throw new InvalidTokenException();
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setToken(null);
        user.setExpirationToken(null);

        userRepository.save(user);
    }
}
