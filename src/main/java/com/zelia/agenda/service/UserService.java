package com.zelia.agenda.service;

import com.zelia.agenda.domain.user.PasswordPolicy;
import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.domain.user.UserType;
import com.zelia.agenda.domain.user.dto.CreateUserDTO;
import com.zelia.agenda.domain.user.dto.ForgotPasswordDTO;
import com.zelia.agenda.domain.user.dto.ResetPasswordDTO;
import com.zelia.agenda.domain.user.dto.UpdateUserDTO;
import com.zelia.agenda.exceptions.BusinessRuleException;
import com.zelia.agenda.exceptions.InvalidTokenException;
import com.zelia.agenda.exceptions.RegisteredEmailException;
import com.zelia.agenda.exceptions.UserInactiveException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public List<User> getAllUsers() {
        return userRepository.findByActiveTrue();
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario não encontrado: " + id));
    }

    public User createUser(CreateUserDTO dto) {
        PasswordPolicy.validate(dto.password());
        String email = dto.email().trim();

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new RegisteredEmailException(email);
        }

        User user = new User(dto.name(),
                email,
                passwordEncoder.encode(dto.password()),
                dto.userType(),
                true);

        return userRepository.save(user);
    }

    public User updateUser(Long id, UpdateUserDTO dto) {
        User user = getUser(id);

        if (!user.getActive()) {
            throw new UserInactiveException(id);
        }

        String email = dto.email().trim();
        if (!user.getEmail().equalsIgnoreCase(email)
                && userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new RegisteredEmailException(email);
        }

        if (user.getUserType() == UserType.ADMIN && dto.userType() != UserType.ADMIN) {
            ensureAnotherActiveAdminExists();
        }

        boolean acessoAlterado = !user.getEmail().equalsIgnoreCase(email)
                || user.getUserType() != dto.userType();

        user.update(dto.name(), email, dto.userType());

        // Mudança de e-mail ou de papel invalida os tokens já emitidos para esta conta.
        if (acessoAlterado) {
            user.invalidateTokens();
        }

        return userRepository.save(user);
    }

    public User reactiveUser(Long id) {
        User user = getUser(id);

        user.setActive(true);
        return userRepository.save(user);
    }

    public User deactivateUser(Long id, Long actorId) {
        User user = getUser(id);

        if (!user.getActive()) {
            throw new UserInactiveException(id);
        }

        if (user.getId().equals(actorId)) {
            throw new BusinessRuleException("Você não pode desativar a própria conta.");
        }

        if (user.getUserType() == UserType.ADMIN) {
            ensureAnotherActiveAdminExists();
        }

        user.setActive(false);
        user.invalidateTokens();
        return userRepository.save(user);
    }

    public void logout(Long userId) {
        User user = getUser(userId);
        user.invalidateTokens();
        userRepository.save(user);
    }

    public void sendToken(ForgotPasswordDTO dto) {
        userRepository.findByEmailIgnoreCase(dto.email().trim())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .ifPresent(user -> {
                    String token = generateToken();

                    user.setToken(hashToken(token));
                    user.setExpirationToken(LocalDateTime.now().plus(TOKEN_VALIDITY));
                    userRepository.save(user);

                    emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
                });
    }

    public void resetPassword(ResetPasswordDTO dto) {
        PasswordPolicy.validate(dto.newPassword());

        User user = userRepository.findByToken(hashToken(dto.token()))
                .orElseThrow(InvalidTokenException::new);

        if (!Boolean.TRUE.equals(user.getActive()) || isExpired(user.getExpirationToken())) {
            throw new InvalidTokenException();
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setToken(null);
        user.setExpirationToken(null);
        user.invalidateTokens();

        userRepository.save(user);
    }

    private void ensureAnotherActiveAdminExists() {
        if (userRepository.countByUserTypeAndActiveTrue(UserType.ADMIN) <= 1) {
            throw new BusinessRuleException("É necessário manter pelo menos um administrador ativo.");
        }
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
}
