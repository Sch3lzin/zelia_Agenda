package com.zelia.agenda.service;

import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.domain.user.UserType;
import com.zelia.agenda.domain.user.dto.CreateUserDTO;
import com.zelia.agenda.domain.user.dto.ForgotPasswordDTO;
import com.zelia.agenda.domain.user.dto.ResetPasswordDTO;
import com.zelia.agenda.domain.user.dto.UpdateUserDTO;
import com.zelia.agenda.exceptions.BusinessRuleException;
import com.zelia.agenda.exceptions.InvalidPasswordException;
import com.zelia.agenda.exceptions.InvalidTokenException;
import com.zelia.agenda.exceptions.RegisteredEmailException;
import com.zelia.agenda.exceptions.UserInactiveException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, emailService);
    }

    private static User user(long id, String email, UserType tipo, boolean ativo) {
        User user = new User("Ana", email, "hash-antigo", tipo, ativo);
        user.setId(id);
        return user;
    }

    private static String sha256Hex(String valor) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    // ---------- criação ----------

    @Test
    void createUserWithWeakPasswordFailsWithoutSaving() {
        CreateUserDTO dto = new CreateUserDTO("Ana", "ana@exemplo.com", "1234567", UserType.PROFESSOR);

        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(InvalidPasswordException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserWithRegisteredEmailFails() {
        CreateUserDTO dto = new CreateUserDTO("Ana", "ana@exemplo.com", "senha12345", UserType.PROFESSOR);
        when(userRepository.findByEmailIgnoreCase("ana@exemplo.com"))
                .thenReturn(Optional.of(user(5L, "ana@exemplo.com", UserType.PROFESSOR, true)));

        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(RegisteredEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserEncodesPasswordAndTrimsEmail() {
        CreateUserDTO dto = new CreateUserDTO("Ana", " ana@exemplo.com ", "senha12345", UserType.SECRETARIO);
        when(userRepository.findByEmailIgnoreCase("ana@exemplo.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha12345")).thenReturn("hash-novo");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User criado = userService.createUser(dto);

        assertThat(criado.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(criado.getPassword()).isEqualTo("hash-novo");
        assertThat(criado.getUserType()).isEqualTo(UserType.SECRETARIO);
        assertThat(criado.getActive()).isTrue();
    }

    // ---------- atualização ----------

    @Test
    void updateNonExistingUserThrowsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L,
                new UpdateUserDTO("Ana", "ana@exemplo.com", UserType.PROFESSOR)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateInactiveUserFails() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, "ana@exemplo.com", UserType.PROFESSOR, false)));

        assertThatThrownBy(() -> userService.updateUser(1L,
                new UpdateUserDTO("Ana", "ana@exemplo.com", UserType.PROFESSOR)))
                .isInstanceOf(UserInactiveException.class);
    }

    @Test
    void updateUserWithEmailOfAnotherAccountFails() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, "ana@exemplo.com", UserType.PROFESSOR, true)));
        when(userRepository.findByEmailIgnoreCase("outra@exemplo.com"))
                .thenReturn(Optional.of(user(2L, "outra@exemplo.com", UserType.PROFESSOR, true)));

        assertThatThrownBy(() -> userService.updateUser(1L,
                new UpdateUserDTO("Ana", "outra@exemplo.com", UserType.PROFESSOR)))
                .isInstanceOf(RegisteredEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void demotingLastAdminFails() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, "ana@exemplo.com", UserType.ADMIN, true)));
        when(userRepository.countByUserTypeAndActiveTrue(UserType.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateUser(1L,
                new UpdateUserDTO("Ana", "ana@exemplo.com", UserType.PROFESSOR)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void changingRoleInvalidatesOldTokens() {
        User user = user(1L, "ana@exemplo.com", UserType.PROFESSOR, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User atualizado = userService.updateUser(1L,
                new UpdateUserDTO("Ana Souza", "ana@exemplo.com", UserType.SECRETARIO));

        assertThat(atualizado.getName()).isEqualTo("Ana Souza");
        assertThat(atualizado.getUserType()).isEqualTo(UserType.SECRETARIO);
        assertThat(atualizado.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void changingOnlyNameKeepsTokens() {
        User user = user(1L, "ana@exemplo.com", UserType.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User atualizado = userService.updateUser(1L,
                new UpdateUserDTO("Ana Souza", "ana@exemplo.com", UserType.ADMIN));

        assertThat(atualizado.getName()).isEqualTo("Ana Souza");
        assertThat(atualizado.getTokenVersion()).isZero();
    }

    // ---------- desativação e reativação ----------

    @Test
    void deactivatingOwnAccountFails() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, "ana@exemplo.com", UserType.ADMIN, true)));

        assertThatThrownBy(() -> userService.deactivateUser(1L, 1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivatingLastAdminFails() {
        User admin = user(2L, "admin@exemplo.com", UserType.ADMIN, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.countByUserTypeAndActiveTrue(UserType.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.deactivateUser(2L, 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deactivatingInactiveUserFails() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user(2L, "prof@exemplo.com", UserType.PROFESSOR, false)));

        assertThatThrownBy(() -> userService.deactivateUser(2L, 1L))
                .isInstanceOf(UserInactiveException.class);
    }

    @Test
    void deactivatingProfessorMarksInactiveAndInvalidatesTokens() {
        User professor = user(2L, "prof@exemplo.com", UserType.PROFESSOR, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User desativado = userService.deactivateUser(2L, 1L);

        assertThat(desativado.getActive()).isFalse();
        assertThat(desativado.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void reactivatingUserMarksActive() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user(2L, "prof@exemplo.com", UserType.PROFESSOR, false)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User reativado = userService.reactiveUser(2L);

        assertThat(reativado.getActive()).isTrue();
    }

    // ---------- recuperação de senha ----------

    @Test
    void sendTokenStoresHashAndSendsPlainToken() throws Exception {
        User user = user(1L, "ana@exemplo.com", UserType.PROFESSOR, true);
        when(userRepository.findByEmailIgnoreCase("ana@exemplo.com")).thenReturn(Optional.of(user));

        userService.sendToken(new ForgotPasswordDTO("ana@exemplo.com"));

        ArgumentCaptor<String> tokenEnviado = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("ana@exemplo.com"), eq("Ana"), tokenEnviado.capture());
        String tokenPlano = tokenEnviado.getValue();

        assertThat(user.getToken()).isEqualTo(sha256Hex(tokenPlano));
        assertThat(user.getToken()).isNotEqualTo(tokenPlano);
        assertThat(user.getExpirationToken()).isAfter(LocalDateTime.now().plusMinutes(14));
        verify(userRepository).save(user);
    }

    @Test
    void sendTokenForUnknownEmailDoesNothing() {
        when(userRepository.findByEmailIgnoreCase("ninguem@exemplo.com")).thenReturn(Optional.empty());

        userService.sendToken(new ForgotPasswordDTO("ninguem@exemplo.com"));

        verifyNoInteractions(emailService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void sendTokenForInactiveUserDoesNotSendEmail() {
        when(userRepository.findByEmailIgnoreCase("ana@exemplo.com"))
                .thenReturn(Optional.of(user(1L, "ana@exemplo.com", UserType.PROFESSOR, false)));

        userService.sendToken(new ForgotPasswordDTO("ana@exemplo.com"));

        verifyNoInteractions(emailService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetWithWeakPasswordFailsBeforeLookingUpToken() {
        assertThatThrownBy(() -> userService.resetPassword(new ResetPasswordDTO("token", "curta")))
                .isInstanceOf(InvalidPasswordException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void resetWithUnknownTokenFails() {
        when(userRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword(new ResetPasswordDTO("token-falso", "senha12345")))
                .isInstanceOf(InvalidTokenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetWithExpiredTokenFails() {
        User user = user(1L, "ana@exemplo.com", UserType.PROFESSOR, true);
        user.setToken("hash");
        user.setExpirationToken(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByToken(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.resetPassword(new ResetPasswordDTO("token", "senha12345")))
                .isInstanceOf(InvalidTokenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetForInactiveAccountFails() {
        User user = user(1L, "ana@exemplo.com", UserType.PROFESSOR, false);
        user.setToken("hash");
        user.setExpirationToken(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByToken(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.resetPassword(new ResetPasswordDTO("token", "senha12345")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validResetChangesPasswordClearsTokenAndInvalidatesSessions() {
        User user = user(1L, "ana@exemplo.com", UserType.PROFESSOR, true);
        user.setToken("hash");
        user.setExpirationToken(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByToken(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nova-senha-123")).thenReturn("hash-nova");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.resetPassword(new ResetPasswordDTO("token-do-email", "nova-senha-123"));

        assertThat(user.getPassword()).isEqualTo("hash-nova");
        assertThat(user.getToken()).isNull();
        assertThat(user.getExpirationToken()).isNull();
        assertThat(user.getTokenVersion()).isEqualTo(1);
    }

    // ---------- outros ----------

    @Test
    void logoutInvalidatesUserTokens() {
        User user = user(1L, "ana@exemplo.com", UserType.PROFESSOR, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.logout(1L);

        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void getUnknownUserThrowsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(9L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listUsersReturnsOnlyActiveUsers() {
        when(userRepository.findByActiveTrue())
                .thenReturn(List.of(user(1L, "ana@exemplo.com", UserType.PROFESSOR, true)));

        assertThat(userService.getAllUsers()).hasSize(1);
    }
}
